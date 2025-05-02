package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class medicament_ajouter extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    public static final String CHANNEL_ID = "medication_reminder_channel";

    // Views
    private ImageView imgMedication;
    private Button btnSelectImage, btnDone;
    private ImageView medicineImage, prevMedicineType, nextMedicineType;
    private Spinner doseUnitSpinner, nombreSpinner, nbrDoseSpinner;
    private TextView medicineTypeText;
    private EditText etName;
    private LinearLayout horaireContainer;

    // Data
    private Bitmap selectedImageBitmap;
    private Uri selectedImageUri;
    private final Map<String, Integer> medicineImages = new HashMap<>();
    private final Map<String, String[]> doseUnits = new HashMap<>();
    private String[] medicineTypes;
    private int medicineIndex = 0;
    private List<EditText> horaireInputs = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Edit mode
    private String medicamentId;
    private boolean isEditMode = false;
    private Medicament medicamentToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicament_ajouter);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if in edit mode
        if (getIntent().hasExtra("medicamentId")) {
            medicamentId = getIntent().getStringExtra("medicamentId");
            isEditMode = true;
            loadMedicamentData();
        }

        // Request permissions
        requestPermissions();

        initializeViews();
        setupMedicineData();
        createNotificationChannel();
    }

    private void requestPermissions() {
        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void initializeViews() {
        imgMedication = findViewById(R.id.medicineImage);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnDone = findViewById(R.id.btn_done);
        etName = findViewById(R.id.et_name);
        medicineImage = findViewById(R.id.medicineImage);
        prevMedicineType = findViewById(R.id.prevMedicineType);
        nextMedicineType = findViewById(R.id.nextMedicineType);
        medicineTypeText = findViewById(R.id.medicineTypeText);
        doseUnitSpinner = findViewById(R.id.doseUnitSpinner);
        nombreSpinner = findViewById(R.id.nombre);
        nbrDoseSpinner = findViewById(R.id.nbr_dose_spinner);
        horaireContainer = findViewById(R.id.horaireContainer);

        btnDone.setText(isEditMode ? R.string.update_button_text : R.string.done_button_text);

        // Setup adapters
        ArrayAdapter<CharSequence> nombreAdapter = ArrayAdapter.createFromResource(this,
                R.array.nombre_fois_array, android.R.layout.simple_spinner_item);
        nombreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nombreSpinner.setAdapter(nombreAdapter);

        ArrayAdapter<CharSequence> doseAdapter = ArrayAdapter.createFromResource(this,
                R.array.nombre_dose_array, android.R.layout.simple_spinner_item);
        doseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nbrDoseSpinner.setAdapter(doseAdapter);

        // Set listeners
        btnSelectImage.setOnClickListener(view -> showImagePickerDialog());
        btnDone.setOnClickListener(view -> saveMedication());
        prevMedicineType.setOnClickListener(v -> changeMedicineType(-1));
        nextMedicineType.setOnClickListener(v -> changeMedicineType(1));

        nombreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateHoraireFields(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadMedicamentData() {
        db.collection("users")
                .document(currentUser.getUid())
                .collection("medicaments")
                .document(medicamentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        medicamentToEdit = documentSnapshot.toObject(Medicament.class);
                        if (medicamentToEdit != null) {
                            populateForm(medicamentToEdit);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateForm(Medicament medicament) {
        etName.setText(medicament.getName());

        int typeIndex = Arrays.asList(medicineTypes).indexOf(medicament.getType());
        if (typeIndex >= 0) {
            medicineIndex = typeIndex;
            updateUI(medicament.getType());
        }

        setSpinnerSelection(doseUnitSpinner, medicament.getDoseUnit());
        setSpinnerSelection(nombreSpinner, medicament.getNombreFois());
        setSpinnerSelection(nbrDoseSpinner, medicament.getNombreDose());

        if (medicament.getHoraires() != null) {
            updateHoraireFields(medicament.getHoraires().size());
            for (int i = 0; i < medicament.getHoraires().size(); i++) {
                if (i < horaireInputs.size()) {
                    horaireInputs.get(i).setText(medicament.getHoraires().get(i));
                }
            }
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private void updateHoraireFields(int count) {
        horaireContainer.removeAllViews();
        horaireInputs.clear();

        for (int i = 0; i < count; i++) {
            EditText etHoraire = new EditText(this);
            etHoraire.setId(View.generateViewId());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 16;
            etHoraire.setLayoutParams(params);
            etHoraire.setHint(getString(R.string.horaire_hint, i + 1));
            etHoraire.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);
            etHoraire.setBackground(ContextCompat.getDrawable(this, android.R.drawable.edit_text));
            etHoraire.setPadding(32, 16, 32, 16);

            horaireContainer.addView(etHoraire);
            horaireInputs.add(etHoraire);
        }
    }

    private void setupMedicineData() {
        medicineImages.put("Sirop", R.drawable.sirop);
        medicineImages.put("Injection", R.drawable.injection);
        medicineImages.put("Capsule", R.drawable.capsule);
        medicineImages.put("Tablette", R.drawable.tablette);
        medicineImages.put("Gouttes", R.drawable.gouttes);
        medicineImages.put("Poudre", R.drawable.poudre);
        medicineImages.put("Pomade", R.drawable.pomade);

        doseUnits.put("Sirop", new String[]{"mL (CC)", "Cuillérées"});
        doseUnits.put("Poudre", new String[]{"bouffées", "sachets", "grammes"});
        doseUnits.put("Capsule", new String[]{"caps", "grammes"});
        doseUnits.put("Tablette", new String[]{"comprimés", "pilules", "grammes"});
        doseUnits.put("Gouttes", new String[]{"gouttes", "grammes"});
        doseUnits.put("Injection", new String[]{"ampoules", "grammes"});
        doseUnits.put("Pomade", new String[]{"fois", "applications"});

        medicineTypes = new String[]{"Sirop", "Poudre", "Capsule", "Tablette", "Gouttes", "Injection", "Pomade"};

        updateUI(medicineTypes[medicineIndex]);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medication Reminder",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for medication reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void changeMedicineType(int direction) {
        medicineIndex = (medicineIndex + direction + medicineTypes.length) % medicineTypes.length;
        updateUI(medicineTypes[medicineIndex]);
    }

    private void updateUI(String medicineType) {
        medicineImage.setImageResource(medicineImages.get(medicineType));
        medicineTypeText.setText(medicineType);

        ArrayAdapter<String> doseAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                doseUnits.get(medicineType)
        );
        doseUnitSpinner.setAdapter(doseAdapter);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Permission de la caméra refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Les notifications ne fonctionneront pas sans permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                selectedImageBitmap = (Bitmap) data.getExtras().get("data");
                imgMedication.setImageBitmap(selectedImageBitmap);
                selectedImageUri = null;
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                selectedImageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    imgMedication.setImageBitmap(selectedImageBitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveMedication() {
        // Validation des champs
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = medicineTypeText.getText().toString();
        String doseUnit = doseUnitSpinner.getSelectedItem().toString();
        String nombreFois = nombreSpinner.getSelectedItem().toString();
        String nombreDose = nbrDoseSpinner.getSelectedItem().toString();

        // Validation des horaires
        List<String> horaires = new ArrayList<>();
        for (EditText et : horaireInputs) {
            String horaire = et.getText().toString().trim();
            if (horaire.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les horaires", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!horaire.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) {
                Toast.makeText(this, "Format horaire invalide (HH:MM)", Toast.LENGTH_SHORT).show();
                return;
            }
            horaires.add(horaire);
        }

        // Préparation des données
        Map<String, Object> medication = new HashMap<>();
        String docId = isEditMode ? medicamentId : UUID.randomUUID().toString();

        medication.put("id", docId);
        medication.put("name", name);
        medication.put("type", type);
        medication.put("doseUnit", doseUnit);
        medication.put("nombreFois", nombreFois);
        medication.put("nombreDose", nombreDose);
        medication.put("horaires", horaires);
        medication.put("userId", currentUser.getUid());
        medication.put("timestamp", System.currentTimeMillis());

        // Gestion de l'image
        if (selectedImageBitmap != null) {
            uploadImageWithFallback(medication);
        } else {
            handleNoImageCase(medication);
        }
    }

    private void uploadImageWithFallback(Map<String, Object> medication) {
        try {
            // 1. Essayer Firebase Storage
            String filename = "med_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef
                    .child("medicaments")
                    .child(currentUser.getUid())
                    .child(filename);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            imageRef.putBytes(imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            medication.put("imageUrl", uri.toString());
                            saveToFirestore(medication, uri.toString());
                        }).addOnFailureListener(e -> {
                            Log.e("Storage", "Failed to get URL", e);
                            fallbackToBase64(medication);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Storage", "Upload failed", e);
                        fallbackToBase64(medication);
                    });
        } catch (Exception e) {
            Log.e("Upload", "Critical error", e);
            fallbackToBase64(medication);
        }
    }

    private void fallbackToBase64(Map<String, Object> medication) {
        try {
            String imageBase64 = encodeImageToBase64(selectedImageBitmap);
            medication.put("imageBase64", imageBase64);
            saveToFirestore(medication, null);
            Toast.makeText(this, "Image sauvegardée en Base64", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Base64", "Encoding failed", e);
            saveToFirestore(medication, null);
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void handleNoImageCase(Map<String, Object> medication) {
        if (isEditMode && medicamentToEdit != null) {
            // Vérifier si l'image existe dans les données Firestore
            if (medicamentToEdit.getImageUrl() != null) {
                medication.put("imageUrl", medicamentToEdit.getImageUrl());
            }
            // Si vous utilisez Base64, ajoutez cette vérification
        /*
        try {
            String base64 = medicamentToEdit.getImageBase64();
            if (base64 != null && !base64.isEmpty()) {
                medication.put("imageBase64", base64);
            }
        } catch (Exception e) {
            Log.e("Medicament", "No imageBase64 field", e);
        }
        */
        }
        saveToFirestore(medication, null);
    }

    private void saveToFirestore(Map<String, Object> medication, String imageUrl) {
        if (imageUrl != null) {
            medication.put("imageUrl", imageUrl);
            medication.remove("imageBase64");
        }

        db.collection("users")
                .document(currentUser.getUid())
                .collection("medicaments")
                .document((String) medication.get("id"))
                .set(medication)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            isEditMode ? "Médicament mis à jour" : "Médicament ajouté",
                            Toast.LENGTH_SHORT).show();
                    scheduleNotification(medication);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Save failed", e);
                });
    }

    private void cancelExistingNotifications() {
        if (isEditMode && medicamentToEdit != null && medicamentToEdit.getHoraires() != null) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            int requestCodeBase = medicamentId.hashCode();

            for (int i = 0; i < medicamentToEdit.getHoraires().size(); i++) {
                int requestCode = requestCodeBase + i;
                Intent intent = new Intent(this, MedicationReminderReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNotification(Map<String, Object> medication) {
        List<String> horaires = (List<String>) medication.get("horaires");
        String medName = (String) medication.get("name");
        String nombreDose = (String) medication.get("nombreDose");
        String medId = (String) medication.get("id");
        int requestCodeBase = medId.hashCode();

        for (int i = 0; i < horaires.size(); i++) {
            try {
                String[] timeParts = horaires.get(i).split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(this, MedicationReminderReceiver.class);
                intent.putExtra("medicationName", medName);
                intent.putExtra("horaireIndex", i);
                intent.putExtra("nombreDose", nombreDose);
                intent.putExtra("medicationId", medId);

                int requestCode = requestCodeBase + i;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent);
                    }
                }
            } catch (Exception e) {
                Log.e("Notification", "Erreur programmation alarme", e);
            }
        }
    }
}
