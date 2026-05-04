package com.example.homemade;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class rig extends AppCompatActivity {

    private TextView tvLogin, tvRegister;
    private ImageView tvLogout;
    private TextInputEditText etEmail, etPassword, etConfirmPassword, etName, etPhone;
    private Button btnLogin;
    private CheckBox cbRememberMe;
    private Spinner spinner;
    private FirebaseAuth mAuth;// هاي فقك ل الايميل و الباسور
    private FirebaseFirestore db;// هاي تخزين البيانات بجدول لل مستخدم مثل رقم الهاتف
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rig);

        // تهيئة Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("HomemadePrefs", MODE_PRIVATE);

        // ربط العناصر
        spinner = findViewById(R.id.spinnerAccountType);
        btnLogin = findViewById(R.id.btnLogin);
        tvLogin = findViewById(R.id.tvGoLogin);
        tvRegister = findViewById(R.id.tv_goToRegister);
        cbRememberMe = findViewById(R.id.cbTerms);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        etConfirmPassword = findViewById(R.id.confirmPassword);
        etName = findViewById(R.id.name);
        etPhone = findViewById(R.id.phone);
        tvLogout = findViewById(R.id.btnBackCart);


        // إعداد Spinner
        String[] items = {"بائع", "زبون", "اختر نوع الحساب"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // تبويب "تسجيل الدخول"
        tvLogin.setOnClickListener(v -> {
            tvLogin.setBackgroundResource(R.drawable.tab_active);
            tvLogin.setTextColor(getColor(android.R.color.white));
            tvRegister.setBackgroundResource(R.drawable.tab_rounded);
            tvRegister.setTextColor(getColor(android.R.color.black));
            startActivity(new Intent(rig.this, Login.class));
        });

        // تبويب "إنشاء حساب"
        tvRegister.setOnClickListener(v -> {
            tvRegister.setBackgroundResource(R.drawable.tab_active);
            tvRegister.setTextColor(getColor(android.R.color.white));
            tvLogin.setBackgroundResource(R.drawable.tab_rounded);
            tvLogin.setTextColor(getColor(android.R.color.black));
        });

        // زر إنشاء الحساب
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String accountType = spinner.getSelectedItem().toString();

            // التحقق من الاسم
            if (name.isEmpty()) {
                etName.setError("الرجاء إدخال الاسم كاملاً");
                etName.requestFocus();
                return;
            }

            // التحقق من الهاتف
            if (phone.isEmpty()) {
                etPhone.setError("الرجاء إدخال رقم الهاتف");
                etPhone.requestFocus();
                return;
            }

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

            // التحقق من تطابق كلمة المرور
            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("كلمة المرور غير متطابقة");
                etConfirmPassword.requestFocus();
                return;
            }

            // التحقق من نوع الحساب
            if (accountType.equals("اختر نوع الحساب")) {
                Toast.makeText(this, "الرجاء اختيار نوع الحساب", Toast.LENGTH_SHORT).show();
                return;
            }

            // إنشاء الحساب في Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String userId = authResult.getUser().getUid();

                        // حفظ البيانات في Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("phone", phone);
                        userData.put("accountType", accountType);
                        userData.put("userId", userId);

                        db.collection("users")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "تم إنشاء الحساب بنجاح!", Toast.LENGTH_SHORT).show();
                                    Intent myintent = new Intent(rig.this, Userpro.class);
                                    myintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(myintent);


                                    // الانتقال حسب نوع الحساب
                                    Intent intent;
                                    if (accountType.equals("بائع")) {
                                        intent = new Intent(rig.this, JoinSeller.class);
                                    } else {
                                        intent = new Intent(rig.this,Userpro.class);
                                    }
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "خطأ في حفظ البيانات: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "خطأ في إنشاء الحساب: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
        tvLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myintent=new Intent(rig.this,MainActivity.class);
                startActivity(myintent);
            }
        });
    }
}