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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class contact_ajouter extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private ImageView imgMedication;
    private EditText etName;
    private EditText etPhone;
    private Bitmap selectedImageBitmap;

    private String contactId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_ajouter);

        imgMedication = findViewById(R.id.img_medication);
        Button btnSelectImage = findViewById(R.id.btn_select_image);
        Button btnDone = findViewById(R.id.btn_done);
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);

        // Vérifie si on est en mode modification
        Intent intent = getIntent();
        contactId = intent.getStringExtra("contactId");

        if (contactId != null) {
            isEditMode = true;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .collection("contacts")
                        .document(contactId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                etName.setText(documentSnapshot.getString("nom"));
                                etPhone.setText(documentSnapshot.getString("telephone"));
                                String imageBase64 = documentSnapshot.getString("imageBase64");
                                if (imageBase64 != null) {
                                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                    selectedImageBitmap = bitmap;
                                    imgMedication.setImageBitmap(bitmap);
                                }
                            }
                        });
            }
        }

        btnSelectImage.setOnClickListener(view -> showImagePickerDialog());

        btnDone.setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || selectedImageBitmap == null) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            } else {
                String imageBase64 = encodeImageToBase64(selectedImageBitmap);
                if (isEditMode) {
                    updateContact(name, phone, imageBase64);
                } else {
                    uploadContact(name, phone, imageBase64);
                }
            }
        });
    }

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

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Aucune application appareil photo trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                selectedImageBitmap = (Bitmap) data.getExtras().get("data");
                imgMedication.setImageBitmap(selectedImageBitmap);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imgMedication.setImageBitmap(selectedImageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erreur de chargement de l'image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void uploadContact(String name, String phone, String imageBase64) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> contact = new HashMap<>();
        contact.put("nom", name);
        contact.put("telephone", phone);
        contact.put("imageBase64", imageBase64);

        db.collection("users")
                .document(userId)
                .collection("contacts")
                .add(contact)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Contact ajouté avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de l'ajout : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateContact(String name, String phone, String imageBase64) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> contact = new HashMap<>();
        contact.put("nom", name);
        contact.put("telephone", phone);
        contact.put("imageBase64", imageBase64);

        db.collection("users")
                .document(userId)
                .collection("contacts")
                .document(contactId)
                .set(contact)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contact modifié avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de la mise à jour : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
