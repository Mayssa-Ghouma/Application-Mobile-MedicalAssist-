package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;


public class MainActivity extends AppCompatActivity {

    private ImageView play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure this is correct
        FirebaseApp.initializeApp(this);

        ImageView play = findViewById(R.id.imgLogo); // Verify ID matches XML
        if (play != null) {
            play.setOnClickListener(view -> {
                startActivity(new Intent(this, interfac_login.class));
                finish();
            });
        } else {
            Log.e("MainActivity", "ImageView not found!");
        }
    }
}
