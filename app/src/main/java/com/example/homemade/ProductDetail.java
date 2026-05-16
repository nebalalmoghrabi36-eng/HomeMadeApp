package com.example.homemade;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProductDetail extends AppCompatActivity {

    private ImageView imgProductDetail, btnBack;
    private TextView tvDetailName, tvDetailPrice, tvDetailDesc, tvDetailCategory, tvDetailRatingCount;
    private RatingBar ratingBarDetail;
    private Button btnAddToCartDetail;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String productId;
    private String sellerId;    // ✅ مضاف
    private String sellerName;  // ✅ مضاف

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ربط العناصر
        imgProductDetail    = findViewById(R.id.imgProductDetail);
        tvDetailName        = findViewById(R.id.tvDetailName);
        tvDetailPrice       = findViewById(R.id.tvDetailPrice);
        tvDetailDesc        = findViewById(R.id.tvDetailDesc);
        tvDetailCategory    = findViewById(R.id.tvDetailCategory);
        tvDetailRatingCount = findViewById(R.id.tvDetailRatingCount);
        ratingBarDetail     = findViewById(R.id.ratingBarDetail);
        btnAddToCartDetail  = findViewById(R.id.btnAddToCartDetail);
        btnBack             = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // استقبال بيانات المنتج
        productId  = getIntent().getStringExtra("productId");
        sellerId   = getIntent().getStringExtra("sellerId");    // ✅ مضاف
        sellerName = getIntent().getStringExtra("sellerName");  // ✅ مضاف

        String name     = getIntent().getStringExtra("name");
        String desc     = getIntent().getStringExtra("description");
        double price    = getIntent().getDoubleExtra("price", 0);
        float  rating   = getIntent().getFloatExtra("rating", 0);
        int reviewCount = getIntent().getIntExtra("reviewCount", 0);
        String category = getIntent().getStringExtra("category");

        // عرض البيانات
        tvDetailName.setText(name);
        tvDetailDesc.setText(desc);
        tvDetailPrice.setText(price + " JD");
        tvDetailCategory.setText(category != null ? category : "");
        ratingBarDetail.setRating(rating);
        tvDetailRatingCount.setText("(" + reviewCount + " تقييم)");

        // زر إضافة للسلة
        btnAddToCartDetail.setOnClickListener(v -> addToCart(name, price));
    }

    private void addToCart(String productName, double price) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "يجب تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("productName", productName);
        cartItem.put("price", price);
        cartItem.put("userId", userId);
        cartItem.put("quantity", 1);
        cartItem.put("sellerId", sellerId != null ? sellerId : "");         // ✅ مضاف
        cartItem.put("sellerName", sellerName != null ? sellerName : "-");  // ✅ مضاف

        db.collection("cart")
                .add(cartItem)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "تمت الإضافة للسلة! ✅", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خطأ في الإضافة", Toast.LENGTH_SHORT).show()
                );
    }
}