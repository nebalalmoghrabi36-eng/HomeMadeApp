package com.example.homemade;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddProduct extends AppCompatActivity {

    private TextInputEditText etProductName, etProductDesc, etProductPrice;
    private Spinner spinnerCategory;
    private CheckBox cbFeatured;
    private Button btnSaveProduct;
    private ImageView btnBackAddProduct;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ربط العناصر
        etProductName      = findViewById(R.id.etProductName);
        etProductDesc      = findViewById(R.id.etProductDesc);
        etProductPrice     = findViewById(R.id.etProductPrice);
        spinnerCategory    = findViewById(R.id.spinnerCategory);
        cbFeatured         = findViewById(R.id.cbFeatured);
        btnSaveProduct     = findViewById(R.id.btnSaveProduct);
        btnBackAddProduct  = findViewById(R.id.btnBackAddProduct);

        // إعداد الفئات
        String[] categories = {
                "اختر الفئة",
                "أكل وحلويات",
                "إكسسوارات",
                "أشغال يدوية",
                "أطفال",
                "هدايا",
                "ديكور"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        btnBackAddProduct.setOnClickListener(v -> finish());

        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    private void saveProduct() {
        String name     = etProductName.getText().toString().trim();
        String desc     = etProductDesc.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        boolean featured = cbFeatured.isChecked();

        // التحقق من الحقول
        if (name.isEmpty()) {
            etProductName.setError("الرجاء إدخال اسم المنتج");
            etProductName.requestFocus();
            return;
        }
        if (desc.isEmpty()) {
            etProductDesc.setError("الرجاء إدخال وصف المنتج");
            etProductDesc.requestFocus();
            return;
        }
        if (priceStr.isEmpty()) {
            etProductPrice.setError("الرجاء إدخال السعر");
            etProductPrice.requestFocus();
            return;
        }
        if (category.equals("اختر الفئة")) {
            Toast.makeText(this, "الرجاء اختيار الفئة", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        String sellerId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        // حفظ المنتج في Firestore
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", desc);
        product.put("price", price);
        product.put("category", category);
        product.put("isFeatured", featured);
        product.put("sellerId", sellerId);
        product.put("rating", 0.0);
        product.put("reviewCount", 0);
        product.put("imageUrl", "");

        btnSaveProduct.setEnabled(false);

        db.collection("products")
                .add(product)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ تم إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveProduct.setEnabled(true);
                    Toast.makeText(this, "خطأ في إضافة المنتج", Toast.LENGTH_SHORT).show();
                });
    }
}