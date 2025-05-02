package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ajoutmed extends AppCompatActivity {
    private MedicamentAdapter adapter;
    private List<Medicament> medicamentList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajoutmed);

        db = FirebaseFirestore.getInstance();

        ListView listView = findViewById(R.id.lvMedications);
        Button addButton = findViewById(R.id.btnAjouter);
        TextView tvMessageVide = findViewById(R.id.tvMessageVide);

        medicamentList = new ArrayList<>();
        adapter = new MedicamentAdapter(this, medicamentList);
        listView.setAdapter(adapter);

        addButton.setOnClickListener(v -> {
            startActivity(new Intent(ajoutmed.this, medicament_ajouter.class));
        });

        loadMedicaments();
    }

    private void loadMedicaments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("medicaments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        medicamentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Medicament medicament = document.toObject(Medicament.class);
                            medicament.setId(document.getId());
                            medicamentList.add(medicament);
                        }
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } else {
                        Toast.makeText(this, R.string.load_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        TextView tvMessageVide = findViewById(R.id.tvMessageVide);
        tvMessageVide.setVisibility(medicamentList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicaments();
    }
}
