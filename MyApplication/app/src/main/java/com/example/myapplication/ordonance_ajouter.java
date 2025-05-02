package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ordonance_ajouter extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;

    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private ImageView imgMedication;
    private Button btnSelectImage;
    private Button btnDone;
    private EditText etName;

    private Bitmap selectedImageBitmap;
    private String ordonnanceId;
    private boolean isEditMode = false;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordonance_ajouter);

        // Initialisation des vues
        imgMedication = findViewById(R.id.img_medication);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnDone = findViewById(R.id.btn_done);
        etName = findViewById(R.id.et_name);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Check if we're in edit mode
        if (getIntent().hasExtra("ordonnance_id")) {
            isEditMode = true;
            ordonnanceId = getIntent().getStringExtra("ordonnance_id");
            etName.setText(getIntent().getStringExtra("ordonnance_name"));

            String imageBase64 = getIntent().getStringExtra("ordonnance_image");
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                selectedImageBitmap = decodeBase64ToBitmap(imageBase64);
                imgMedication.setImageBitmap(selectedImageBitmap);
            }
        }

        // Choisir une image via le bouton
        btnSelectImage.setOnClickListener(view -> showImagePickerDialog());

        // Action du bouton "Done"
        btnDone.setOnClickListener(view -> {

            String ordonnanceName = etName.getText().toString().trim();
            // Vérification des champs vides
            if (ordonnanceName.isEmpty() || selectedImageBitmap == null) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            } else {
                // Encoder l'image en Base64 et télécharger dans Firestore
                String imageBase64 = encodeImageToBase64(selectedImageBitmap);
                uploadOrdonnanceToFirestore(ordonnanceName, imageBase64);

            }
        });
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void updateOrdonnance(String id, String name, String imageBase64) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", name);
        updates.put("imageBase64", imageBase64);

        db.collection("users")
                .document(user.getUid())
                .collection("ordonnances")
                .document(id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Ordonnance mise à jour", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show();
                });
    }

    // Afficher le dialogue pour choisir entre la caméra ou la galerie
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Choisir une image")
                .setItems(new String[]{"Camera", "Galerie"}, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Aucune application appareil photo trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    // Ouvrir la caméra
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    // Ouvrir la galerie
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Vérifier le résultat de l'intention
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                selectedImageBitmap = (Bitmap) data.getExtras().get("data");
                imgMedication.setImageBitmap(selectedImageBitmap); // Afficher l'image dans l'ImageView
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imgMedication.setImageBitmap(selectedImageBitmap); // Afficher l'image dans l'ImageView
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erreur de chargement de l'image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Encoder l'image en Base64
    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Télécharger l'ordonnance dans Firestore
    private void uploadOrdonnanceToFirestore(String name, String imageBase64) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Créer un objet pour l'ordonnance
        Map<String, Object> ordonnance = new HashMap<>();
        ordonnance.put("nom", name);
        ordonnance.put("imageBase64", imageBase64);

        // Ajouter l'ordonnance dans la sous-collection "ordonnances" de l'utilisateur
        db.collection("users")
                .document(userId)
                .collection("ordonnances")
                .add(ordonnance)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Ordonnance ajoutée avec succès", Toast.LENGTH_SHORT).show();
                    finish(); // Ferme l’activité ou redirige
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de l'ajout : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}