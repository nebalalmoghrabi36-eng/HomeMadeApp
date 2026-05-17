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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        totalPrice      = getIntent().getDoubleExtra("totalPrice", 0);
        customerName    = getIntent().getStringExtra("customerName");
        customerPhone   = getIntent().getStringExtra("customerPhone");
        customerAddress = getIntent().getStringExtra("customerAddress");

        etCardName      = findViewById(R.id.etCardName);
        etCardNumber    = findViewById(R.id.etCardNumber);
        etExpiry        = findViewById(R.id.etExpiry);
        etCVV           = findViewById(R.id.etCVV);
        btnPay          = findViewById(R.id.btnPay);
        btnBackPayment  = findViewById(R.id.btnBackPayment);
        progressPayment = findViewById(R.id.progressPayment);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);

        tvPaymentAmount.setText(String.format("%.2f JD", totalPrice));
        btnBackPayment.setOnClickListener(v -> finish());
        btnPay.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        String cardName   = etCardName.getText().toString().trim();
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiry     = etExpiry.getText().toString().trim();
        String cvv        = etCVV.getText().toString().trim();

        if (cardName.isEmpty()) {
            etCardName.setError("الرجاء إدخال اسم حامل البطاقة");
            etCardName.requestFocus(); return;
        }
        if (cardNumber.length() != 16) {
            etCardNumber.setError("رقم البطاقة يجب أن يكون 16 رقم");
            etCardNumber.requestFocus(); return;
        }
        if (expiry.isEmpty() || expiry.length() != 4) {
            etExpiry.setError("الرجاء إدخال تاريخ الانتهاء (MMYY)");
            etExpiry.requestFocus(); return;
        }
        if (cvv.length() != 3) {
            etCVV.setError("CVV يجب أن يكون 3 أرقام");
            etCVV.requestFocus(); return;
        }

        btnPay.setEnabled(false);
        btnPay.setText("جاري المعالجة...");
        progressPayment.setVisibility(View.VISIBLE);

        new Handler().postDelayed(() -> {
            progressPayment.setVisibility(View.GONE);
            saveOrderAndShowSuccess();
        }, 3000);
    }

    private void saveOrderAndShowSuccess() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // ✅ اقرأ السلة أولاً عشان نحفظ items و sellerId صح
        db.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(cartSnapshot -> {

                    List<Map<String, Object>> cartItems = new ArrayList<>();
                    List<String> cartDocIds = new ArrayList<>();
                    for (DocumentSnapshot doc : cartSnapshot.getDocuments()) {
                        cartItems.add(doc.getData());
                        cartDocIds.add(doc.getId());
                    }

                    // جمّع المنتجات حسب sellerId
                    Map<String, List<Map<String, Object>>> ordersBySeller = new HashMap<>();
                    for (Map<String, Object> item : cartItems) {
                        String sid = item.get("sellerId") != null
                                ? item.get("sellerId").toString() : "unknown";
                        if (!ordersBySeller.containsKey(sid))
                            ordersBySeller.put(sid, new ArrayList<>());
                        ordersBySeller.get(sid).add(item);
                    }

                    // احفظ طلب منفصل لكل بائع + items subcollection
                    for (Map.Entry<String, List<Map<String, Object>>> entry : ordersBySeller.entrySet()) {
                        String sellerId   = entry.getKey();
                        List<Map<String, Object>> items = entry.getValue();

                        double sellerTotal = 0;
                        for (Map<String, Object> item : items) {
                            if (item.get("price") instanceof Number)
                                sellerTotal += ((Number) item.get("price")).doubleValue();
                        }

                        String sellerName = items.get(0).get("sellerName") != null
                                ? items.get(0).get("sellerName").toString() : "-";

                        Map<String, Object> order = new HashMap<>();
                        order.put("userId",          userId);
                        order.put("sellerId",         sellerId);        // ✅
                        order.put("sellerName",       sellerName);      // ✅
                        order.put("customerName",     customerName);
                        order.put("customerPhone",    customerPhone);
                        order.put("customerAddress",  customerAddress);
                        order.put("paymentMethod",    "بوابة دفع إلكتروني");
                        order.put("totalAmount",      sellerTotal);
                        order.put("itemCount",        items.size());
                        order.put("status",           "paid");
                        order.put("isRated",          false);           // ✅

                        // احفظ الطلب ثم items
                        db.collection("orders").add(order)
                                .addOnSuccessListener(orderRef -> {
                                    // ✅ items subcollection — الأساس لتقييم المنتجات
                                    for (Map<String, Object> item : items) {
                                        Map<String, Object> itemData = new HashMap<>();
                                        itemData.put("productId",   item.get("productId") != null
                                                ? item.get("productId").toString() : "");
                                        itemData.put("productName", item.get("productName") != null
                                                ? item.get("productName").toString() : "منتج");
                                        itemData.put("price",       item.get("price"));
                                        itemData.put("quantity",    item.get("quantity") != null
                                                ? item.get("quantity") : 1);
                                        itemData.put("sellerId",    sellerId);
                                        itemData.put("sellerName",  sellerName);

                                        orderRef.collection("items").add(itemData);
                                    }
                                });
                    }

                    // احذف السلة
                    for (String docId : cartDocIds) {
                        db.collection("cart").document(docId).delete();
                    }

                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    // حتى لو فشل قراءة السلة، احفظ الطلب بشكل مبسط
                    Map<String, Object> order = new HashMap<>();
                    order.put("userId",         userId);
                    order.put("customerName",   customerName);
                    order.put("customerPhone",  customerPhone);
                    order.put("customerAddress",customerAddress);
                    order.put("paymentMethod",  "بوابة دفع إلكتروني");
                    order.put("totalAmount",    totalPrice);
                    order.put("status",         "paid");
                    order.put("isRated",        false);

                    db.collection("orders").add(order)
                            .addOnSuccessListener(ref -> showSuccessDialog())
                            .addOnFailureListener(err -> {
                                btnPay.setEnabled(true);
                                btnPay.setText("ادفع الآن 🔒");
                                Toast.makeText(this, "خطأ في معالجة الدفع", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("تم الدفع بنجاح! ✅")
                .setMessage("شكراً " + customerName + "!\n\nتم تأكيد طلبك بمبلغ " +
                        String.format("%.2f JD", totalPrice) +
                        "\n\nسيتم التواصل معك على " + customerPhone)
                .setCancelable(false)
                .setPositiveButton("العودة للرئيسية", (dialog, which) -> {
                    Intent intent = new Intent(Payment.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}