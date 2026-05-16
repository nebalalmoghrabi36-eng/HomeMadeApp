package com.example.homemade;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddProduct extends AppCompatActivity {

    private TextInputEditText etProductName, etProductDesc, etProductPrice;
    private Spinner spinnerCategory;
    private CheckBox cbFeatured;
    private Button btnSaveProduct, btnPickImage;
    private ImageView imgProductPreview, btnBackAddProduct;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String imageBase64 = ""; // الصورة محولة لـ Base64

    // Launcher لاختيار الصورة
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    convertImageToBase64(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ربط العناصر
        etProductName     = findViewById(R.id.etProductName);
        etProductDesc     = findViewById(R.id.etProductDesc);
        etProductPrice    = findViewById(R.id.etProductPrice);
        spinnerCategory   = findViewById(R.id.spinnerCategory);
        cbFeatured        = findViewById(R.id.cbFeatured);
        btnSaveProduct    = findViewById(R.id.btnSaveProduct);
        btnPickImage      = findViewById(R.id.btnPickImage);
        imgProductPreview = findViewById(R.id.imgProductPreview);
        btnBackAddProduct = findViewById(R.id.btnBackAddProduct);

        // إعداد الفئات
        String[] categories = {
                "اختر الفئة", "أكل وحلويات", "إكسسوارات",
                "أشغال يدوية", "أطفال", "هدايا", "ديكور"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        btnBackAddProduct.setOnClickListener(v -> finish());

        // اختيار صورة
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    private void convertImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // تصغير الصورة
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // عرض معاينة
            imgProductPreview.setImageBitmap(resized);
            Toast.makeText(this, "✅ تم اختيار الصورة", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "خطأ في تحميل الصورة", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProduct() {
        String name     = etProductName.getText().toString().trim();
        String desc     = etProductDesc.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        boolean featured = cbFeatured.isChecked();

        // التحقق
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

        double price    = Double.parseDouble(priceStr);
        String sellerId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        // جلب اسم البائع من Firestore
        db.collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    String sellerName = doc.getString("name");
                    if (sellerName == null) sellerName = "بائع";

                    // حفظ المنتج
                    Map<String, Object> product = new HashMap<>();
                    product.put("name", name);
                    product.put("description", desc);
                    product.put("price", price);
                    product.put("category", category);
                    product.put("isFeatured", featured);
                    product.put("sellerId", sellerId);
                    product.put("sellerName", sellerName);
                    product.put("rating", 0.0);
                    product.put("reviewCount", 0);
                    product.put("imageBase64", imageBase64); // الصورة كـ Base64

                    btnSaveProduct.setEnabled(false);
                    btnSaveProduct.setText("جاري الحفظ...");

                    db.collection("products").add(product)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, " تم إضافة المنتج بنجاح! ✅", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSaveProduct.setEnabled(true);
                                btnSaveProduct.setText(" إضافة المنتج ✅");
                                Toast.makeText(this, "خطأ في إضافة المنتج", Toast.LENGTH_SHORT).show();
                            });
                });
    }
}