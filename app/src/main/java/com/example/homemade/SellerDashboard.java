package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homemade.adapters.ProductAdapter;
import com.example.homemade.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SellerDashboard extends AppCompatActivity {

    private RecyclerView rvSellerProducts;
    private TextView tvTotalProducts, tvTotalOrders, tvAvgRating, tvNoSellerProducts;
    private Button btnAddProduct;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProductAdapter productAdapter;
    private List<Product> sellerProducts = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // ربط العناصر
        rvSellerProducts = findViewById(R.id.rvSellerProducts);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvNoSellerProducts = findViewById(R.id.tvNoSellerProducts);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        // إعداد RecyclerView
        productAdapter = new ProductAdapter(this, sellerProducts);
        rvSellerProducts.setLayoutManager(new LinearLayoutManager(this));
        rvSellerProducts.setAdapter(productAdapter);
        rvSellerProducts.setNestedScrollingEnabled(false);

        // تحميل البيانات
        loadSellerProducts();

        // زر إضافة منتج
        btnAddProduct.setOnClickListener(v ->
                startActivity(new Intent(SellerDashboard.this, JoinSeller.class))
        );
    }

    private void loadSellerProducts() {
        if (userId == null) return;

        db.collection("products")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sellerProducts.clear();
                    float totalRating = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = new Product();
                        product.setId(doc.getId());
                        product.setName(doc.getString("name"));
                        product.setDescription(doc.getString("description"));
                        product.setPrice(doc.getDouble("price") != null ? doc.getDouble("price") : 0);
                        product.setRating(doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 0);
                        product.setReviewCount(doc.getLong("reviewCount") != null ? doc.getLong("reviewCount").intValue() : 0);
                        product.setImageUrl(doc.getString("imageUrl"));
                        product.setFeatured(Boolean.TRUE.equals(doc.getBoolean("isFeatured")));
                        sellerProducts.add(product);
                        totalRating += product.getRating();
                    }

                    productAdapter.notifyDataSetChanged();

                    // تحديث الإحصائيات
                    int count = sellerProducts.size();
                    tvTotalProducts.setText(String.valueOf(count));
                    tvAvgRating.setText(count > 0 ? String.format("%.1f", totalRating / count) : "0.0");

                    if (count == 0) {
                        tvNoSellerProducts.setVisibility(View.VISIBLE);
                        rvSellerProducts.setVisibility(View.GONE);
                    } else {
                        tvNoSellerProducts.setVisibility(View.GONE);
                        rvSellerProducts.setVisibility(View.VISIBLE);
                    }
                });

        // جلب عدد الطلبات
        db.collection("orders")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        tvTotalOrders.setText(String.valueOf(queryDocumentSnapshots.size()))
                );
    }
}