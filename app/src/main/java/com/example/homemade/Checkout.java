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
        tvCheckoutTotal.setText(String.format("%.2f JD", totalPrice));

        orderAdapter = new OrderItemAdapter();
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        rvOrderItems.setAdapter(orderAdapter);

        loadCartItems();

        btnBackCheckout.setOnClickListener(v -> finish());

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
            Intent intent = new Intent(Checkout.this, Payment.class);
            intent.putExtra("totalPrice", totalPrice);
            intent.putExtra("customerName", name);
            intent.putExtra("customerPhone", phone);
            intent.putExtra("customerAddress", address);
            startActivity(intent);
        } else {
            saveOrdersGroupedBySeller(name, phone, address, "نقداً");
        }
    }

    private void saveOrdersGroupedBySeller(String name, String phone, String address, String paymentMethod) {

        // 1️⃣ جمّع المنتجات حسب sellerId
        Map<String, List<Map<String, Object>>> ordersBySeller = new HashMap<>();
        for (Map<String, Object> item : orderItems) {
            String sid = item.get("sellerId") != null ? item.get("sellerId").toString() : "unknown";
            if (!ordersBySeller.containsKey(sid)) {
                ordersBySeller.put(sid, new ArrayList<>());
            }
            ordersBySeller.get(sid).add(item);
        }

        // 2️⃣ احفظ طلب منفصل لكل بائع + items subcollection
        for (Map.Entry<String, List<Map<String, Object>>> entry : ordersBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<Map<String, Object>> items = entry.getValue();

            double sellerTotal = 0;
            for (Map<String, Object> item : items) {
                if (item.get("price") instanceof Number) {
                    sellerTotal += ((Number) item.get("price")).doubleValue();
                }
            }

            String sellerName = items.get(0).get("sellerName") != null
                    ? items.get(0).get("sellerName").toString() : "-";

            Map<String, Object> order = new HashMap<>();
            order.put("userId",           userId);
            order.put("sellerId",         sellerId);
            order.put("sellerName",       sellerName);
            order.put("customerName",     name);
            order.put("customerPhone",    phone);
            order.put("customerAddress",  address);
            order.put("paymentMethod",    paymentMethod);
            order.put("totalAmount",      sellerTotal);
            order.put("itemCount",        items.size());
            order.put("status",           "paid");
            order.put("isRated",          false);

            // ✅ احفظ الطلب الرئيسي أولاً، ثم احفظ كل منتج كـ subcollection
            db.collection("orders").add(order)
                    .addOnSuccessListener(orderRef -> {
                        // ✅ هذا هو الجزء المهم — بدونه التقييم ما يشتغل
                        for (Map<String, Object> item : items) {
                            Map<String, Object> itemData = new HashMap<>();

                            // productId — مهم جداً للتقييم
                            itemData.put("productId",   item.get("productId") != null
                                    ? item.get("productId").toString() : "");

                            // productName — يظهر في dialog التقييم
                            itemData.put("productName", item.get("productName") != null
                                    ? item.get("productName").toString() : "منتج");

                            itemData.put("price",       item.get("price"));
                            itemData.put("quantity",    item.get("quantity") != null
                                    ? item.get("quantity") : 1);
                            itemData.put("sellerId",    sellerId);
                            itemData.put("sellerName",  sellerName);

                            // احفظ كل منتج داخل orders/{orderId}/items/
                            orderRef.collection("items").add(itemData);
                        }
                    });
        }

        // 3️⃣ احذف السلة
        for (String docId : cartDocIds) {
            db.collection("cart").document(docId).delete();
        }

        Toast.makeText(this,
                "✅ تم تأكيد طلبك!\nسيتم التواصل معك على " + phone,
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Checkout.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────
    class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(Checkout.this)
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = orderItems.get(position);
            Object itemName = item.get("productName");
            Object price    = item.get("price");
            holder.text1.setText(itemName != null ? itemName.toString() : "منتج");
            holder.text2.setText(price != null ?
                    String.format("%.2f JD", ((Number) price).doubleValue()) : "0 JD");
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