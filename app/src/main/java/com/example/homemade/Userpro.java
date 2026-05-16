package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Userpro extends AppCompatActivity {

    private TextView tvUserName, tvAccountType, tvEmail, tvPhone, tvLogout, tvNoOrders;
    private ImageView imgProfile;
    private RecyclerView rvOrders;
    private Button btnOrder, btnitemcart;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    private List<Map<String, Object>> ordersList = new ArrayList<>();
    private List<String> ordersDocIds = new ArrayList<>();
    private OrdersAdapter ordersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userpro);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        imgProfile    = findViewById(R.id.imgProfile);
        tvUserName    = findViewById(R.id.tvUserName);
        tvAccountType = findViewById(R.id.tvAccountType);
        tvEmail       = findViewById(R.id.tvEmail);
        tvPhone       = findViewById(R.id.tvPhone);
        tvLogout      = findViewById(R.id.tvLogout);
        tvNoOrders    = findViewById(R.id.tvNoOrders);
        rvOrders      = findViewById(R.id.rvOrders);
        btnOrder      = findViewById(R.id.itemProduct);
        btnitemcart   = findViewById(R.id.itemcart);

        // إعداد RecyclerView للطلبات
        ordersAdapter = new OrdersAdapter();
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(ordersAdapter);
        rvOrders.setNestedScrollingEnabled(false);

        loadUserData();

        btnOrder.setOnClickListener(v ->
                startActivity(new Intent(Userpro.this, BrowseProducts.class))
        );
        btnitemcart.setOnClickListener(v ->
                startActivity(new Intent(Userpro.this, Cart.class))
        );

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

        loadOrders();
    }

    private void loadOrders() {
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ordersList.clear();
                    ordersDocIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ordersList.add(doc.getData());
                        ordersDocIds.add(doc.getId());
                    }

                    ordersAdapter.notifyDataSetChanged();

                    if (ordersList.isEmpty()) {
                        tvNoOrders.setVisibility(View.VISIBLE);
                        rvOrders.setVisibility(View.GONE);
                    } else {
                        tvNoOrders.setVisibility(View.GONE);
                        rvOrders.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> tvNoOrders.setVisibility(View.VISIBLE));
    }

    // ✅ دالة التقييم — تفتح dialog للزبون يقيم المنتجات
    private void showRatingDialog(Map<String, Object> order, int position) {
        String orderId  = ordersDocIds.get(position);
        String sellerId = order.get("sellerId") != null ? order.get("sellerId").toString() : null;

        // جلب منتجات هذا الطلب من Firestore
        db.collection("orders").document(orderId)
                .collection("items")
                .get()
                .addOnSuccessListener(itemsSnapshot -> {
                    if (itemsSnapshot.isEmpty()) {
                        // لو ما في sub-collection، قيّم البائع مباشرة
                        showSellerRatingDialog(sellerId, orderId, order);
                    } else {
                        // قيّم كل منتج لحاله
                        showProductsRatingDialog(itemsSnapshot.getDocuments(), sellerId, orderId);
                    }
                })
                .addOnFailureListener(e -> {
                    // fallback: قيّم البائع مباشرة
                    showSellerRatingDialog(sellerId, orderId, order);
                });
    }

    // ✅ تقييم البائع مباشرة (لما ما في items sub-collection)
    private void showSellerRatingDialog(String sellerId, String orderId, Map<String, Object> order) {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⭐ قيّم طلبك");

        // بناء الـ dialog يدوياً
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView tvProductName = new TextView(this);
        String sellerName = order.get("sellerName") != null ? order.get("sellerName").toString() : "البائع";
        tvProductName.setText("تقييم الطلب من: " + sellerName);
        tvProductName.setTextSize(16);
        tvProductName.setGravity(android.view.Gravity.CENTER);
        tvProductName.setPadding(0, 0, 0, 20);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(5);
        android.widget.LinearLayout.LayoutParams params =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        ratingBar.setLayoutParams(params);

        layout.addView(tvProductName);
        layout.addView(ratingBar);
        builder.setView(layout);

        builder.setPositiveButton("إرسال التقييم", (dialog, which) -> {
            float rating = ratingBar.getRating();
            submitRatingToSeller(sellerId, rating, orderId);
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    // ✅ تقييم كل منتج لحاله
    private void showProductsRatingDialog(List<DocumentSnapshot> items, String sellerId, String orderId) {
        // نقيّم منتج منتج
        rateNextProduct(items, 0, sellerId, orderId);
    }

    private void rateNextProduct(List<DocumentSnapshot> items, int index, String sellerId, String orderId) {
        if (index >= items.size()) {
            Toast.makeText(this, "✅ شكراً! تم إرسال تقييماتك", Toast.LENGTH_SHORT).show();
            // تحديث الطلب كـ rated
            db.collection("orders").document(orderId).update("isRated", true);
            return;
        }

        DocumentSnapshot item = items.get(index);
        String productId   = item.getString("productId");
        String productName = item.getString("productName") != null ? item.getString("productName") : "منتج";

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView tvName = new TextView(this);
        tvName.setText("قيّم: " + productName);
        tvName.setTextSize(16);
        tvName.setGravity(android.view.Gravity.CENTER);
        tvName.setPadding(0, 0, 0, 20);

        TextView tvCount = new TextView(this);
        tvCount.setText((index + 1) + " / " + items.size());
        tvCount.setTextSize(12);
        tvCount.setTextColor(0xFF999999);
        tvCount.setGravity(android.view.Gravity.CENTER);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(5);
        android.widget.LinearLayout.LayoutParams params =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        ratingBar.setLayoutParams(params);

        layout.addView(tvName);
        layout.addView(tvCount);
        layout.addView(ratingBar);

        new AlertDialog.Builder(this)
                .setTitle("⭐ تقييم المنتجات")
                .setView(layout)
                .setPositiveButton("التالي ➡", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    if (productId != null) {
                        submitRatingToProduct(productId, sellerId, rating);
                    }
                    // انتقل للمنتج التالي
                    rateNextProduct(items, index + 1, sellerId, orderId);
                })
                .setNegativeButton("تخطي", (dialog, which) ->
                        rateNextProduct(items, index + 1, sellerId, orderId)
                )
                .setCancelable(false)
                .show();
    }

    // ✅ حفظ التقييم في Firestore وتحديث متوسط المنتج
    private void submitRatingToProduct(String productId, String sellerId, float rating) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        double oldRating = doc.getDouble("rating") != null ? doc.getDouble("rating") : 0;
                        long reviewCount = doc.getLong("reviewCount") != null ? doc.getLong("reviewCount") : 0;

                        // احساب المتوسط الجديد
                        double newRating = ((oldRating * reviewCount) + rating) / (reviewCount + 1);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("rating", newRating);
                        updates.put("reviewCount", reviewCount + 1);

                        db.collection("products").document(productId).update(updates);
                    }
                });
    }

    // ✅ تقييم البائع (يحدث متوسط تقييمه في داشبورده)
    private void submitRatingToSeller(String sellerId, float rating, String orderId) {
        if (sellerId == null) return;

        // حفظ التقييم في collection مستقلة
        Map<String, Object> review = new HashMap<>();
        review.put("sellerId", sellerId);
        review.put("userId", userId);
        review.put("orderId", orderId);
        review.put("rating", rating);

        db.collection("reviews").add(review)
                .addOnSuccessListener(ref -> {
                    // تحديث الطلب كـ rated
                    db.collection("orders").document(orderId).update("isRated", true);
                    Toast.makeText(this, "✅ شكراً! تم إرسال تقييمك", Toast.LENGTH_SHORT).show();
                    loadOrders(); // تحديث القائمة
                });
    }

    // ── Adapter للطلبات ────────────────────────────────────────────────────────
    class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // بناء الـ item يدوياً
            android.widget.LinearLayout layout = new android.widget.LinearLayout(Userpro.this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(32, 24, 32, 24);
            layout.setBackgroundColor(0xFFF9F9F9);

            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            layout.setLayoutParams(lp);
            layout.setElevation(4);

            return new OrderViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Map<String, Object> order = ordersList.get(position);

            String sellerName   = order.get("sellerName") != null ? order.get("sellerName").toString() : "-";
            Object total        = order.get("totalAmount");
            Object status       = order.get("status");
            Object payment      = order.get("paymentMethod");
            boolean isRated     = Boolean.TRUE.equals(order.get("isRated"));

            holder.tvSeller.setText("🏪 " + sellerName);
            holder.tvTotal.setText(total != null ?
                    String.format("المجموع: %.2f JD", ((Number) total).doubleValue()) : "");
            holder.tvStatus.setText("paid".equals(status) ? "مدفوع ✅" : "معلق ⏳");
            holder.tvPayment.setText(payment != null ? "الدفع: " + payment : "");

            if (isRated) {
                holder.btnRate.setText("تم التقييم ⭐");
                holder.btnRate.setEnabled(false);
                holder.btnRate.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFCCCCCC));
            } else {
                holder.btnRate.setText("قيّم الطلب ⭐");
                holder.btnRate.setEnabled(true);
                holder.btnRate.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2E7D32));
                holder.btnRate.setOnClickListener(v ->
                        showRatingDialog(order, holder.getAdapterPosition())
                );
            }
        }

        @Override
        public int getItemCount() { return ordersList.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvSeller, tvTotal, tvStatus, tvPayment;
            Button btnRate;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                android.widget.LinearLayout root = (android.widget.LinearLayout) itemView;

                tvSeller  = new TextView(Userpro.this);
                tvSeller.setTextSize(15);
                tvSeller.setTextColor(0xFF1B1B1B);
                tvSeller.setTypeface(null, android.graphics.Typeface.BOLD);
                tvSeller.setGravity(android.view.Gravity.END);

                tvTotal   = new TextView(Userpro.this);
                tvTotal.setTextSize(14);
                tvTotal.setTextColor(0xFF2E7D32);
                tvTotal.setGravity(android.view.Gravity.END);

                tvStatus  = new TextView(Userpro.this);
                tvStatus.setTextSize(13);
                tvStatus.setTextColor(0xFF555555);
                tvStatus.setGravity(android.view.Gravity.END);

                tvPayment = new TextView(Userpro.this);
                tvPayment.setTextSize(13);
                tvPayment.setTextColor(0xFF888888);
                tvPayment.setGravity(android.view.Gravity.END);

                btnRate = new Button(Userpro.this);
                btnRate.setText("قيّم الطلب ⭐");
                btnRate.setTextColor(0xFFFFFFFF);
                android.widget.LinearLayout.LayoutParams btnParams =
                        new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                btnParams.setMargins(0, 16, 0, 0);
                btnRate.setLayoutParams(btnParams);

                root.addView(tvSeller);
                root.addView(tvTotal);
                root.addView(tvStatus);
                root.addView(tvPayment);
                root.addView(btnRate);
            }
        }
    }
}