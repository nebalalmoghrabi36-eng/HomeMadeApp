package com.example.homemade;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
public class AllOrders extends AppCompatActivity {


        private RecyclerView rvAllOrders;
        private TextView tvNoOrders;

        private FirebaseAuth mAuth;
        private FirebaseFirestore db;
        private String userId;

        private List<Map<String, Object>> ordersList = new ArrayList<>();
        private List<String> ordersDocIds = new ArrayList<>();
        private AllOrdersAdapter adapter;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_all_orders);

            mAuth  = FirebaseAuth.getInstance();
            db     = FirebaseFirestore.getInstance();
            userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

            rvAllOrders = findViewById(R.id.rvAllOrders);
            tvNoOrders  = findViewById(R.id.tvNoOrders);

            // زر الرجوع
            View btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            adapter = new AllOrdersAdapter();
            rvAllOrders.setLayoutManager(new LinearLayoutManager(this));
            rvAllOrders.setAdapter(adapter);

            loadAllOrders();
        }

        private void loadAllOrders() {
            if (userId == null) return;

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

                        adapter.notifyDataSetChanged();

                        if (ordersList.isEmpty()) {
                            tvNoOrders.setVisibility(View.VISIBLE);
                            rvAllOrders.setVisibility(View.GONE);
                        } else {
                            tvNoOrders.setVisibility(View.GONE);
                            rvAllOrders.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> tvNoOrders.setVisibility(View.VISIBLE));
        }

        // ────────────────────────────────────────────────────────────────────────────
        // تحديث isRated في القائمة المحلية فوراً بدون إعادة تحميل
        // ────────────────────────────────────────────────────────────────────────────
        private void markOrderAsRated(String orderId) {
            int index = ordersDocIds.indexOf(orderId);
            if (index != -1) {
                ordersList.get(index).put("isRated", true);
                adapter.notifyItemChanged(index);
            }
        }

        // ────────────────────────────────────────────────────────────────────────────
        // منطق التقييم (نفس Userpro بالضبط)
        // ────────────────────────────────────────────────────────────────────────────
        private void showRatingDialog(Map<String, Object> order, int position) {
            String orderId  = ordersDocIds.get(position);
            String sellerId = order.get("sellerId") != null ? order.get("sellerId").toString() : null;

            db.collection("orders").document(orderId)
                    .collection("items")
                    .get()
                    .addOnSuccessListener(itemsSnapshot -> {
                        if (!itemsSnapshot.isEmpty()) {
                            rateNextProduct(itemsSnapshot.getDocuments(), 0, sellerId, orderId);
                        } else {
                            showSellerRatingDialog(sellerId, orderId, order);
                        }
                    })
                    .addOnFailureListener(e -> showSellerRatingDialog(sellerId, orderId, order));
        }

        private void showSellerRatingDialog(String sellerId, String orderId, Map<String, Object> order) {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 20);

            TextView tvName = new TextView(this);
            String sellerName = order.get("sellerName") != null ? order.get("sellerName").toString() : "البائع";
            tvName.setText("تقييم الطلب من: " + sellerName);
            tvName.setTextSize(16);
            tvName.setGravity(android.view.Gravity.CENTER);
            tvName.setPadding(0, 0, 0, 20);

            RatingBar ratingBar = new RatingBar(this);
            ratingBar.setNumStars(5);
            ratingBar.setStepSize(1.0f);
            ratingBar.setRating(5);
            android.widget.LinearLayout.LayoutParams p =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            p.gravity = android.view.Gravity.CENTER;
            ratingBar.setLayoutParams(p);

            layout.addView(tvName);
            layout.addView(ratingBar);

            new AlertDialog.Builder(this)
                    .setTitle("⭐ قيّم طلبك")
                    .setView(layout)
                    .setPositiveButton("إرسال التقييم", (dialog, which) ->
                            submitRating(sellerId, ratingBar.getRating(), orderId))
                    .setNegativeButton("إلغاء", null)
                    .show();
        }

        private void rateNextProduct(List<DocumentSnapshot> items, int index, String sellerId, String orderId) {
            if (index >= items.size()) {
                // ✅ خلّص كل المنتجات — حدّث الطلب وحدّث تقييم البائع
                db.collection("orders").document(orderId)
                        .update("isRated", true)
                        .addOnSuccessListener(unused -> {
                            markOrderAsRated(orderId);
                            Toast.makeText(this, "✅ شكراً! تم إرسال تقييماتك", Toast.LENGTH_SHORT).show();
                        });
                return;
            }

            DocumentSnapshot item  = items.get(index);
            String productId       = item.getString("productId");
            String productName     = item.getString("productName") != null
                    ? item.getString("productName") : "منتج";

            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 20);

            TextView tvName = new TextView(this);
            tvName.setText("قيّم: " + productName);
            tvName.setTextSize(16);
            tvName.setGravity(android.view.Gravity.CENTER);
            tvName.setPadding(0, 0, 0, 10);

            TextView tvCount = new TextView(this);
            tvCount.setText((index + 1) + " / " + items.size());
            tvCount.setTextSize(12);
            tvCount.setTextColor(0xFF999999);
            tvCount.setGravity(android.view.Gravity.CENTER);
            tvCount.setPadding(0, 0, 0, 20);

            RatingBar ratingBar = new RatingBar(this);
            ratingBar.setNumStars(5);
            ratingBar.setStepSize(1.0f);
            ratingBar.setRating(5);
            android.widget.LinearLayout.LayoutParams p =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            p.gravity = android.view.Gravity.CENTER;
            ratingBar.setLayoutParams(p);

            layout.addView(tvName);
            layout.addView(tvCount);
            layout.addView(ratingBar);

            new AlertDialog.Builder(this)
                    .setTitle("⭐ تقييم المنتجات")
                    .setView(layout)
                    .setPositiveButton("التالي ➡", (dialog, which) -> {
                        float rating = ratingBar.getRating();
                        if (productId != null) {
                            // 1️⃣ حدّث تقييم المنتج
                            updateProductRating(productId, rating);
                            // 2️⃣ حدّث تقييم البائع
                            if (sellerId != null) updateSellerRating(sellerId, rating, orderId);
                        }
                        rateNextProduct(items, index + 1, sellerId, orderId);
                    })
                    .setNegativeButton("تخطي", (dialog, which) ->
                            rateNextProduct(items, index + 1, sellerId, orderId))
                    .setCancelable(false)
                    .show();
        }

        // ────────────────────────────────────────────────────────────────────────────
        // ✅ تحديث تقييم المنتج (weighted average)
        // ────────────────────────────────────────────────────────────────────────────
        private void updateProductRating(String productId, float rating) {
            db.collection("products").document(productId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        double oldRating   = doc.getDouble("rating")      != null ? doc.getDouble("rating")      : 0;
                        long   reviewCount = doc.getLong("reviewCount")    != null ? doc.getLong("reviewCount")   : 0;
                        double newRating   = ((oldRating * reviewCount) + rating) / (reviewCount + 1);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("rating",      newRating);
                        updates.put("reviewCount", reviewCount + 1);
                        db.collection("products").document(productId).update(updates);
                    });
        }

        // ────────────────────────────────────────────────────────────────────────────
        // ✅ تحديث تقييم البائع (weighted average) — هذا الجديد
        // ────────────────────────────────────────────────────────────────────────────
        private void updateSellerRating(String sellerId, float rating, String orderId) {
            db.collection("users").document(sellerId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        double oldRating   = doc.getDouble("rating")      != null ? doc.getDouble("rating")      : 0;
                        long   reviewCount = doc.getLong("reviewCount")    != null ? doc.getLong("reviewCount")   : 0;
                        double newRating   = ((oldRating * reviewCount) + rating) / (reviewCount + 1);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("rating",      newRating);
                        updates.put("reviewCount", reviewCount + 1);
                        db.collection("users").document(sellerId).update(updates);

                        // احفظ review مستقل للبائع أيضاً
                        Map<String, Object> review = new HashMap<>();
                        review.put("sellerId", sellerId);
                        review.put("userId",   userId);
                        review.put("orderId",  orderId);
                        review.put("rating",   rating);
                        review.put("type",     "seller");
                        db.collection("reviews").add(review);
                    });
        }

        // ────────────────────────────────────────────────────────────────────────────
        // submitRating للطلب العام (بدون items)
        // ────────────────────────────────────────────────────────────────────────────
        private void submitRating(String sellerId, float rating, String orderId) {
            // 1️⃣ حدّث تقييم البائع
            if (sellerId != null && !sellerId.isEmpty()) {
                updateSellerRating(sellerId, rating, orderId);
            }

            // 2️⃣ سجّل الـ review وحدّث الطلب
            Map<String, Object> review = new HashMap<>();
            review.put("sellerId", sellerId != null ? sellerId : "");
            review.put("userId",   userId);
            review.put("orderId",  orderId);
            review.put("rating",   rating);
            review.put("type",     "order");

            db.collection("reviews").add(review)
                    .addOnSuccessListener(ref ->
                            db.collection("orders").document(orderId)
                                    .update("isRated", true)
                                    .addOnSuccessListener(unused -> {
                                        markOrderAsRated(orderId);
                                        Toast.makeText(this, "✅ شكراً! تم إرسال تقييمك", Toast.LENGTH_SHORT).show();
                                    }))
                    .addOnFailureListener(e ->
                            db.collection("orders").document(orderId)
                                    .update("isRated", true)
                                    .addOnSuccessListener(unused -> {
                                        markOrderAsRated(orderId);
                                        Toast.makeText(this, "✅ تم إرسال تقييمك", Toast.LENGTH_SHORT).show();
                                    }));
        }

        // ────────────────────────────────────────────────────────────────────────────
        // Adapter
        // ────────────────────────────────────────────────────────────────────────────
        class AllOrdersAdapter extends RecyclerView.Adapter<AllOrdersAdapter.VH> {

            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                android.widget.LinearLayout layout = new android.widget.LinearLayout(AllOrders.this);
                layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                layout.setPadding(32, 24, 32, 24);
                layout.setBackgroundColor(0xFFFFFFFF);
                android.widget.LinearLayout.LayoutParams lp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 16);
                layout.setLayoutParams(lp);
                return new VH(layout);
            }

            @Override
            public void onBindViewHolder(@NonNull VH holder, int position) {
                Map<String, Object> order = ordersList.get(position);

                String sellerName = order.get("sellerName") != null ? order.get("sellerName").toString() : "-";
                Object total      = order.get("totalAmount");
                Object status     = order.get("status");
                Object payment    = order.get("paymentMethod");
                boolean isRated   = Boolean.TRUE.equals(order.get("isRated"));

                holder.tvSeller.setText("🏪 " + sellerName);
                holder.tvTotal.setText(total != null ?
                        String.format("المجموع: %.2f JD", ((Number) total).doubleValue()) : "");
                holder.tvStatus.setText("paid".equals(status) ? "مدفوع ✅" : "معلق ⏳");
                holder.tvPayment.setText(payment != null ? "الدفع: " + payment : "");

                // ✅ عرض تقييم البائع إن وجد
                Object sellerRating = order.get("sellerRating");
                if (sellerRating != null) {
                    holder.tvSellerRating.setVisibility(View.VISIBLE);
                    holder.tvSellerRating.setText(String.format("⭐ تقييم البائع: %.1f", ((Number) sellerRating).doubleValue()));
                } else {
                    holder.tvSellerRating.setVisibility(View.GONE);
                }

                if (isRated) {
                    holder.btnRate.setVisibility(View.GONE);
                } else {
                    holder.btnRate.setVisibility(View.VISIBLE);
                    holder.btnRate.setText("قيّم الطلب ⭐");
                    holder.btnRate.setEnabled(true);
                    holder.btnRate.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF2E7D32));
                    holder.btnRate.setOnClickListener(v ->
                            showRatingDialog(order, holder.getAdapterPosition()));
                }
            }

            @Override
            public int getItemCount() { return ordersList.size(); }

            class VH extends RecyclerView.ViewHolder {
                TextView tvSeller, tvTotal, tvStatus, tvPayment, tvSellerRating;
                Button btnRate;

                VH(@NonNull View itemView) {
                    super(itemView);
                    android.widget.LinearLayout root = (android.widget.LinearLayout) itemView;

                    tvSeller = new TextView(AllOrders.this);
                    tvSeller.setTextSize(15);
                    tvSeller.setTextColor(0xFF1B1B1B);
                    tvSeller.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvSeller.setGravity(android.view.Gravity.END);

                    tvTotal = new TextView(AllOrders.this);
                    tvTotal.setTextSize(14);
                    tvTotal.setTextColor(0xFF2E7D32);
                    tvTotal.setGravity(android.view.Gravity.END);

                    tvStatus = new TextView(AllOrders.this);
                    tvStatus.setTextSize(13);
                    tvStatus.setTextColor(0xFF555555);
                    tvStatus.setGravity(android.view.Gravity.END);

                    tvPayment = new TextView(AllOrders.this);
                    tvPayment.setTextSize(13);
                    tvPayment.setTextColor(0xFF888888);
                    tvPayment.setGravity(android.view.Gravity.END);

                    // ✅ جديد: عرض تقييم البائع
                    tvSellerRating = new TextView(AllOrders.this);
                    tvSellerRating.setTextSize(13);
                    tvSellerRating.setTextColor(0xFFF57F17);
                    tvSellerRating.setGravity(android.view.Gravity.END);

                    btnRate = new Button(AllOrders.this);
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
                    root.addView(tvSellerRating);   // ← جديد
                    root.addView(btnRate);
                }
            }
        }
    }