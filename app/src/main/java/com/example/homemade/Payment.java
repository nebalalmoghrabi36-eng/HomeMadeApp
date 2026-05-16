package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Payment extends AppCompatActivity {

    private TextInputEditText etCardName, etCardNumber, etExpiry, etCVV;
    private Button btnPay;
    private ImageView btnBackPayment;
    private ProgressBar progressPayment;
    private TextView tvPaymentAmount;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double totalPrice = 0;
    private String customerName, customerPhone, customerAddress;
    private List<String> cartDocIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db   = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // استقبال البيانات من Checkout
        totalPrice       = getIntent().getDoubleExtra("totalPrice", 0);
        customerName     = getIntent().getStringExtra("customerName");
        customerPhone    = getIntent().getStringExtra("customerPhone");
        customerAddress  = getIntent().getStringExtra("customerAddress");

        // ربط العناصر
        etCardName       = findViewById(R.id.etCardName);
        etCardNumber     = findViewById(R.id.etCardNumber);
        etExpiry         = findViewById(R.id.etExpiry);
        etCVV            = findViewById(R.id.etCVV);
        btnPay           = findViewById(R.id.btnPay);
        btnBackPayment   = findViewById(R.id.btnBackPayment);
        progressPayment  = findViewById(R.id.progressPayment);
        tvPaymentAmount  = findViewById(R.id.tvPaymentAmount);

        tvPaymentAmount.setText(String.format("%.2f JD", totalPrice));

        btnBackPayment.setOnClickListener(v -> finish());

        btnPay.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        String cardName   = etCardName.getText().toString().trim();
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiry     = etExpiry.getText().toString().trim();
        String cvv        = etCVV.getText().toString().trim();

        // التحقق من الحقول
        if (cardName.isEmpty()) {
            etCardName.setError("الرجاء إدخال اسم حامل البطاقة");
            etCardName.requestFocus();
            return;
        }
        if (cardNumber.length() != 16) {
            etCardNumber.setError("رقم البطاقة يجب أن يكون 16 رقم");
            etCardNumber.requestFocus();
            return;
        }
        if (expiry.isEmpty() || expiry.length()!=4) {
            etExpiry.setError("الرجاء إدخال تاريخ الانتهاء (MMYY)");
            etExpiry.requestFocus();
            return;
        }
        if (cvv.length() != 3) {
            etCVV.setError("CVV يجب أن يكون 3 أرقام");
            etCVV.requestFocus();
            return;
        }

        // إظهار التحميل
        btnPay.setEnabled(false);
        btnPay.setText("جاري المعالجة...");
        progressPayment.setVisibility(View.VISIBLE);

        // محاكاة عملية الدفع (3 ثواني)
        new Handler().postDelayed(() -> {
            progressPayment.setVisibility(View.GONE);
            saveOrderAndShowSuccess();
        }, 3000);
    }

    private void saveOrderAndShowSuccess() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // حفظ الطلب في Firestore
        Map<String, Object> order = new HashMap<>();
        order.put("userId", userId);
        order.put("customerName", customerName);
        order.put("customerPhone", customerPhone);
        order.put("customerAddress", customerAddress);
        order.put("paymentMethod", "بوابة دفع إلكتروني");
        order.put("totalAmount", totalPrice);
        order.put("status", "paid");

        db.collection("orders").add(order)
                .addOnSuccessListener(ref -> {
                    // حذف السلة
                    db.collection("cart")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(snap -> {
                                for (var doc : snap.getDocuments()) {
                                    db.collection("cart").document(doc.getId()).delete();
                                }
                            });

                    // إظهار رسالة نجاح
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    btnPay.setEnabled(true);
                    btnPay.setText("ادفع الآن 🔒 ");
                    Toast.makeText(this, "خطأ في معالجة الدفع", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("تم الدفع بنجاح! \"✅ " )
                .setMessage("شكراً " + customerName + "!\n\nتم تأكيد طلبك بمبلغ " +
                        String.format("JOD %.2f JD ", totalPrice) + "\n\nسيتم التواصل معك على " + customerPhone)
                .setCancelable(false)
                .setPositiveButton("العودة للرئيسية", (dialog, which) -> {
                    // رجوع للصفحة الرئيسية
                    Intent intent = new Intent(Payment.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}