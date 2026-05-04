package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Checkout extends AppCompatActivity {

    private RecyclerView rvOrderItems;
    private TextView tvCheckoutTotal;
    private TextInputEditText etCustomerName, etCustomerPhone, etCustomerAddress;
    private RadioButton rbCash, rbOnline;
    private LinearLayout layoutCash, layoutOnlinePayment;
    private Button btnConfirmOrder;
    private ImageView btnBackCheckout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private double totalPrice = 0;

    private List<Map<String, Object>> orderItems = new ArrayList<>();
    private List<String> cartDocIds = new ArrayList<>();
    private OrderItemAdapter orderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkedout);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // ربط العناصر
        rvOrderItems        = findViewById(R.id.rvOrderItems);
        tvCheckoutTotal     = findViewById(R.id.tvCheckoutTotal);
        etCustomerName      = findViewById(R.id.etCustomerName);
        etCustomerPhone     = findViewById(R.id.etCustomerPhone);
        etCustomerAddress   = findViewById(R.id.etCustomerAddress);
        rbCash              = findViewById(R.id.rbCash);
        rbOnline            = findViewById(R.id.rbOnline);
        layoutCash          = findViewById(R.id.layoutCash);
        layoutOnlinePayment = findViewById(R.id.layoutOnlinePayment);
        btnConfirmOrder     = findViewById(R.id.btnConfirmOrder);
        btnBackCheckout     = findViewById(R.id.btnBackCheckout);

        totalPrice = getIntent().getDoubleExtra("totalPrice", 0);
        tvCheckoutTotal.setText(String.format("%.2f ₪", totalPrice));

        orderAdapter = new OrderItemAdapter();
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        rvOrderItems.setAdapter(orderAdapter);

        loadCartItems();

        btnBackCheckout.setOnClickListener(v -> finish());

        // اختيار طريقة الدفع
        layoutCash.setOnClickListener(v -> {
            rbCash.setChecked(true);
            rbOnline.setChecked(false);
            layoutCash.setBackgroundColor(0xFFE8F5E9);
            layoutOnlinePayment.setBackgroundColor(0xFFF5F5F5);
        });

        layoutOnlinePayment.setOnClickListener(v -> {
            rbOnline.setChecked(true);
            rbCash.setChecked(false);
            layoutOnlinePayment.setBackgroundColor(0xFFE8F5E9);
            layoutCash.setBackgroundColor(0xFFF5F5F5);
        });

        // تأكيد الطلب
        btnConfirmOrder.setOnClickListener(v -> confirmOrder());
    }

    private void loadCartItems() {
        if (userId == null) return;

        db.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    orderItems.clear();
                    cartDocIds.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        orderItems.add(doc.getData());
                        cartDocIds.add(doc.getId());
                    }
                    orderAdapter.notifyDataSetChanged();

                    // تحميل بيانات المستخدم تلقائياً
                    db.collection("users").document(userId).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String name  = doc.getString("name");
                                    String phone = doc.getString("phone");
                                    if (name  != null) etCustomerName.setText(name);
                                    if (phone != null) etCustomerPhone.setText(phone);
                                }
                            });
                });
    }

    private void confirmOrder() {
        String name    = etCustomerName.getText().toString().trim();
        String phone   = etCustomerPhone.getText().toString().trim();
        String address = etCustomerAddress.getText().toString().trim();

        // التحقق من الحقول
        if (name.isEmpty()) {
            etCustomerName.setError("الرجاء إدخال الاسم");
            etCustomerName.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etCustomerPhone.setError("الرجاء إدخال رقم الهاتف");
            etCustomerPhone.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            etCustomerAddress.setError("الرجاء إدخال العنوان");
            etCustomerAddress.requestFocus();
            return;
        }

        if (rbOnline.isChecked()) {
            // انتقل لصفحة بوابة الدفع
            Intent intent = new Intent(Checkout.this, Payment.class);
            intent.putExtra("totalPrice", totalPrice);
            intent.putExtra("customerName", name);
            intent.putExtra("customerPhone", phone);
            intent.putExtra("customerAddress", address);
            startActivity(intent);

        } else {
            // دفع نقداً - حفظ مباشرة في Firestore
            Map<String, Object> order = new HashMap<>();
            order.put("userId", userId);
            order.put("customerName", name);
            order.put("customerPhone", phone);
            order.put("customerAddress", address);
            order.put("paymentMethod", "نقداً");
            order.put("totalAmount", totalPrice);
            order.put("itemCount", orderItems.size());
            order.put("status", "pending");

            db.collection("orders").add(order)
                    .addOnSuccessListener(ref -> {
                        // حذف السلة
                        for (String docId : cartDocIds) {
                            db.collection("cart").document(docId).delete();
                        }
                        Toast.makeText(this,
                                "✅ تم تأكيد طلبك!\nسيتم التواصل معك على " + phone,
                                Toast.LENGTH_LONG).show();

                        // رجوع للرئيسية
                        Intent intent = new Intent(Checkout.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "خطأ في تأكيد الطلب", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    // ── Order Items Adapter ───────────────────────────────────────────────────
    class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(Checkout.this).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = orderItems.get(position);
            Object name  = item.get("productName");
            Object price = item.get("price");
            holder.text1.setText(name != null ? name.toString() : "منتج");
            holder.text2.setText(price != null ?
                    String.format("%.2f JOD", ((Number) price).doubleValue()) : " JOD 0 ");
            holder.text1.setTextDirection(View.TEXT_DIRECTION_RTL);
            holder.text2.setTextDirection(View.TEXT_DIRECTION_RTL);
        }

        @Override
        public int getItemCount() { return orderItems.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}