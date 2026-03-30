package com.example.homemade;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    private TextView tvLogin, tvRegister;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // تهيئة Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // SharedPreferences لحفظ بيانات تذكرني
        prefs = getSharedPreferences("HomemadePrefs", MODE_PRIVATE);

        // ربط العناصر
        tvLogin = findViewById(R.id.tvGoToRegister);
        tvRegister = findViewById(R.id.tvGoLogin);
        cbRememberMe = findViewById(R.id.cbRemember);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);


        // لو في بيانات محفوظة من قبل احشيها تلقائياً
        if (prefs.getBoolean("rememberMe", false)) {
            etEmail.setText(prefs.getString("email", ""));
            etPassword.setText(prefs.getString("password", ""));
            cbRememberMe.setChecked(true);
        }

        // تبويب "إنشاء حساب"
        tvRegister.setOnClickListener(v -> {
            tvRegister.setBackgroundResource(R.drawable.tab_active);
            tvRegister.setTextColor(getColor(android.R.color.white));
            tvLogin.setBackgroundResource(R.drawable.tab_rounded);
            tvLogin.setTextColor(getColor(android.R.color.black));
            startActivity(new Intent(Login.this, rig.class));
        });

        // تبويب "تسجيل الدخول"
        tvLogin.setOnClickListener(v -> {
            tvLogin.setBackgroundResource(R.drawable.tab_active);
            tvLogin.setTextColor(getColor(android.R.color.white));
            tvRegister.setBackgroundResource(R.drawable.tab_rounded);
            tvRegister.setTextColor(getColor(android.R.color.black));
        });

        // زر تسجيل الدخول
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // التحقق من الإيميل
            if (email.isEmpty()) {
                etEmail.setError("الرجاء إدخال البريد الإلكتروني");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("البريد الإلكتروني غير صحيح");
                etEmail.requestFocus();
                return;
            }

            // التحقق من كلمة المرور
            if (password.isEmpty()) {
                etPassword.setError("الرجاء إدخال كلمة المرور");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
                etPassword.requestFocus();
                return;
            }

            // حفظ بيانات تذكرني
            SharedPreferences.Editor editor = prefs.edit();
            if (cbRememberMe.isChecked()) {
                editor.putBoolean("rememberMe", true);
                editor.putString("email", email);
                editor.putString("password", password);
            } else {
                editor.clear();
            }
            editor.apply();

            // تسجيل الدخول عبر Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Login.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "خطأ: البريد أو كلمة المرور غير صحيحة", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}