package com.example.homemade;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homemade.adapters.ProductAdapter;
import com.example.homemade.models.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BrowseProducts extends AppCompatActivity {

    private RecyclerView rvProducts;
    private EditText etSearch;
    private TextView tvResultCount, tvNoProducts;
    private TextView catAll, catFood2, catAcc2, catHand2, catKids2, catGifts2, catDecor2;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private ProductAdapter productAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private String selectedCategory = "الكل";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_products);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        loadAllProducts();
        setupCategoryFilter();
        setupSearch();

        btnBack.setOnClickListener(v -> finish());

        // لو جاي من فئة معينة
        String category = getIntent().getStringExtra("category");
        if (category != null) {
            filterByCategory(category);
        }
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        etSearch = findViewById(R.id.etSearch);
        tvResultCount = findViewById(R.id.tvResultCount);
        tvNoProducts = findViewById(R.id.tvNoProducts);
        btnBack = findViewById(R.id.btnBack);
        catAll = findViewById(R.id.catAll);
        catFood2 = findViewById(R.id.catFood2);
        catAcc2 = findViewById(R.id.catAcc2);
        catHand2 = findViewById(R.id.catHand2);
        catKids2 = findViewById(R.id.catKids2);
        catGifts2 = findViewById(R.id.catGifts2);
        catDecor2 = findViewById(R.id.catDecor2);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(this, filteredProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(productAdapter);
    }

    private void loadAllProducts() {
        db.collection("products").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allProducts.clear();
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
                        allProducts.add(product);
                    }
                    filterByCategory("الكل");
                });
    }

    private void setupCategoryFilter() {
        catAll.setOnClickListener(v -> { selectCategory(catAll); filterByCategory("الكل"); });
        catFood2.setOnClickListener(v -> { selectCategory(catFood2); filterByCategory("أكل وحلويات"); });
        catAcc2.setOnClickListener(v -> { selectCategory(catAcc2); filterByCategory("إكسسوارات"); });
        catHand2.setOnClickListener(v -> { selectCategory(catHand2); filterByCategory("أشغال يدوية"); });
        catKids2.setOnClickListener(v -> { selectCategory(catKids2); filterByCategory("أطفال"); });
        catGifts2.setOnClickListener(v -> { selectCategory(catGifts2); filterByCategory("هدايا"); });
        catDecor2.setOnClickListener(v -> { selectCategory(catDecor2); filterByCategory("ديكور"); });
    }

    private void selectCategory(TextView selected) {
        // إعادة تعيين كل الأزرار
        for (TextView cat : new TextView[]{catAll, catFood2, catAcc2, catHand2, catKids2, catGifts2, catDecor2}) {
            cat.setBackgroundResource(R.drawable.tab_rounded);
            cat.setTextColor(getColor(android.R.color.black));
        }
        // تفعيل المختار
        selected.setBackgroundResource(R.drawable.tab_active);
        selected.setTextColor(getColor(android.R.color.white));
    }

    private void filterByCategory(String category) {
        selectedCategory = category;
        filteredProducts.clear();

        for (Product p : allProducts) {
            if (category.equals("الكل") || category.equals(p.getCategory())) {
                filteredProducts.add(p);
            }
        }

        productAdapter.notifyDataSetChanged();
        updateResultCount();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchProducts(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchProducts(String query) {
        filteredProducts.clear();
        for (Product p : allProducts) {
            boolean matchesCategory = selectedCategory.equals("الكل") || selectedCategory.equals(p.getCategory());
            boolean matchesSearch = query.isEmpty() || p.getName().contains(query) || p.getDescription().contains(query);
            if (matchesCategory && matchesSearch) {
                filteredProducts.add(p);
            }
        }
        productAdapter.notifyDataSetChanged();
        updateResultCount();
    }

    private void updateResultCount() {
        if (filteredProducts.isEmpty()) {
            tvNoProducts.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
            tvResultCount.setText("لا توجد نتائج");
        } else {
            tvNoProducts.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
            tvResultCount.setText(filteredProducts.size() + " منتج");
        }
    }
}