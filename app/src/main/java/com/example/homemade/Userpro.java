package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Userpro extends AppCompatActivity {

    // عناصر الواجهة
    private TextView tvUserName, tvAccountType, tvEmail, tvPhone, tvLogout, tvNoOrders;
    private ImageView imgProfile;
    private RecyclerView rvOrders;
    private Button btnOrder;

    // متغيرات Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userpro);

        // تهيئة Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // ربط العناصر
        imgProfile = findViewById(R.id.imgProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvAccountType = findViewById(R.id.tvAccountType);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvLogout = findViewById(R.id.tvLogout);
        tvNoOrders = findViewById(R.id.tvNoOrders);
        rvOrders = findViewById(R.id.rvOrders);
        btnOrder = findViewById(R.id.itemProduct);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        // تحميل البيانات
        loadUserData();

        // الانتقال لصفحة المنتجات
        btnOrder.setOnClickListener(v ->
                startActivity(new Intent(Userpro.this, BrowseProducts.class))
        );

        // تسجيل الخروج
        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(Userpro.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvUserName.setText(documentSnapshot.getString("name"));
                        tvEmail.setText(documentSnapshot.getString("email"));
                        tvPhone.setText(documentSnapshot.getString("phone"));
                        tvAccountType.setText(documentSnapshot.getString("accountType"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show()
                );

        loadOrders(userId);
    }

    private void loadOrders(String userId) {
        db.collection("orders").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoOrders.setVisibility(View.VISIBLE);
                        rvOrders.setVisibility(View.GONE);
                    } else {
                        tvNoOrders.setVisibility(View.GONE);
                        rvOrders.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        tvNoOrders.setVisibility(View.VISIBLE)
                );
    }
}