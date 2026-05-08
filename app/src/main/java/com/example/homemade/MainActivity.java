package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnBrowseProducts, btnJoinSeller;
    private TextView tvSeeAll, btnCart;
    private LinearLayout catFood, catAccessories, catHandcraft, catDecor, catKids, catGifts;
    private RecyclerView rvFeaturedProducts;

    private FirebaseFirestore db;
    private ProductAdapter productAdapter;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // لو مسجل دخول تحقق من نوع الحساب
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && "بائع".equals(doc.getString("accountType"))) {
                            // بائع → روح لداشبورد البائع
                            startActivity(new Intent(MainActivity.this, SellerDashboard.class));
                            finish();
                        }
                    });
        }

        initViews();
        setupRecyclerView();
        loadProductsFromFirebase();
        setupClickListeners();
    }

    private void initViews() {
        btnCart            = findViewById(R.id.btnCart);
        btnLogin           = findViewById(R.id.btnLogin);
        btnBrowseProducts  = findViewById(R.id.btnBrowseProducts);
        btnJoinSeller      = findViewById(R.id.btnJoinSeller);
        tvSeeAll           = findViewById(R.id.tvSeeAll);
        rvFeaturedProducts = findViewById(R.id.rvFeaturedProducts);
        catFood            = findViewById(R.id.catFood);
        catAccessories     = findViewById(R.id.catAccessories);
        catHandcraft       = findViewById(R.id.catHandcraft);
        catDecor           = findViewById(R.id.catDecor);
        catKids            = findViewById(R.id.catKids);
        catGifts           = findViewById(R.id.catGifts);
    }

    private void setupRecyclerView() {
        productList    = new ArrayList<>();
        productAdapter = new ProductAdapter(this, productList);
        rvFeaturedProducts.setLayoutManager(new LinearLayoutManager(this));
        rvFeaturedProducts.setAdapter(productAdapter);
        rvFeaturedProducts.setNestedScrollingEnabled(false);
    }

    private void loadProductsFromFirebase() {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = new Product();
                        product.setId(doc.getId());
                        product.setName(doc.getString("name"));
                        product.setDescription(doc.getString("description"));
                        product.setPrice(doc.getDouble("price") != null ? doc.getDouble("price") : 0);
                        product.setRating(doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 0);
                        product.setReviewCount(doc.getLong("reviewCount") != null ? doc.getLong("reviewCount").intValue() : 0);
                        product.setImageUrl(doc.getString("imageUrl"));
                        product.setFeatured(true);
                        productList.add(product);
                    }
                    productAdapter.notifyDataSetChanged();
                    if (productList.isEmpty()) {
                        Toast.makeText(this, "لا توجد منتجات", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خطأ في تحميل المنتجات", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(MainActivity.this, Userpro.class));
            } else {
                startActivity(new Intent(MainActivity.this, Login.class));
            }
        });

        btnBrowseProducts.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BrowseProducts.class))
        );

        btnJoinSeller.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, rig.class))
        );

        tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BrowseProducts.class))
        );

        btnCart.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(MainActivity.this, Cart.class));
            } else {
                Toast.makeText(this, "يجب تسجيل الدخول لعرض السلة", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, Login.class));
            }
        });

        catFood.setOnClickListener(v -> openCategory("أكل وحلويات"));
        catAccessories.setOnClickListener(v -> openCategory("إكسسوارات"));
        catHandcraft.setOnClickListener(v -> openCategory("أشغال يدوية"));
        catDecor.setOnClickListener(v -> openCategory("ديكور"));
        catKids.setOnClickListener(v -> openCategory("أطفال"));
        catGifts.setOnClickListener(v -> openCategory("هدايا"));
    }

    private void openCategory(String category) {
        Intent intent = new Intent(MainActivity.this, BrowseProducts.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }
}