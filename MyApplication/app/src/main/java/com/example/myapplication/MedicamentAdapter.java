package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MedicamentAdapter extends ArrayAdapter<Medicament> {
    private FirebaseFirestore db;
    private Context context;

    public MedicamentAdapter(@NonNull Context context, @NonNull List<Medicament> medicaments) {
        super(context, 0, medicaments);
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Medicament medicament = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_medicament, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tvMedicamentName);
        TextView tvDetails = convertView.findViewById(R.id.tvMedicamentDetails);
        ImageView ivImage = convertView.findViewById(R.id.ivMedicamentImage);
        ImageButton btnEdit = convertView.findViewById(R.id.btnEdit);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        if (medicament != null) {
            tvName.setText(medicament.getName());

            String details = medicament.getNombreDose() + " - " +
                    medicament.getType() + " - " +
                    medicament.getNombreFois() + "/jour";
            tvDetails.setText(details);

            // Charger l'image du médicament
            loadMedicamentImage(medicament, ivImage);

            // Gestion du clic sur l'icône de modification
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, medicament_ajouter.class);
                intent.putExtra("medicamentId", medicament.getId());
                context.startActivity(intent);
            });

            // Gestion du clic sur l'icône de suppression
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Confirmer la suppression")
                        .setMessage("Voulez-vous vraiment supprimer " + medicament.getName() + "?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            deleteMedicament(medicament, position);
                        })
                        .setNegativeButton("Non", null)
                        .show();
            });
        }

        return convertView;
    }

    private void loadMedicamentImage(Medicament medicament, ImageView imageView) {
        // Image par défaut immédiate


        if (medicament == null || imageView == null) {
            return;
        }

        // Priorité à l'URL Firebase Storage
        if (medicament.getImageUrl() != null && !medicament.getImageUrl().isEmpty()) {
            loadFromUrl(medicament.getImageUrl(), imageView);
        }
        // Fallback sur Base64
        else if (medicament.getImageBase64() != null && !medicament.getImageBase64().isEmpty()) {
            loadFromBase64(medicament.getImageBase64(), imageView);
        }
    }

    private void loadFromUrl(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                input = connection.getInputStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(input);

                if (bitmap != null) {
                    // Compression pour éviter les OutOfMemoryError
                    Bitmap compressedBitmap = scaleBitmap(bitmap, 800);

                    imageView.post(() -> {
                        imageView.setImageBitmap(compressedBitmap);
                        if (bitmap != compressedBitmap) {
                            bitmap.recycle();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("ImageLoad", "Error loading from URL", e);

            } finally {
                try {
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e("ImageLoad", "Error closing resources", e);
                }
            }
        }).start();
    }

    private void loadFromBase64(String base64, ImageView imageView) {
        new Thread(() -> {
            try {
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (decodedBitmap != null) {
                    Bitmap compressedBitmap = scaleBitmap(decodedBitmap, 800);

                    imageView.post(() -> {
                        imageView.setImageBitmap(compressedBitmap);
                        if (decodedBitmap != compressedBitmap) {
                            decodedBitmap.recycle();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("ImageLoad", "Error decoding Base64", e);

            }
        }).start();
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = (float) width / (float) height;

        if (width > height) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void deleteMedicament(Medicament medicament, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && medicament != null && medicament.getId() != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("medicaments")
                    .document(medicament.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        remove(medicament);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Médicament supprimé", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Delete failed", e);
                    });
        }
    }
}