package com.example.homemade;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

// DataSeeder.java
// ضعه في java/com/example/handmade/
// شغّله مرة وحدة بس من MainActivity عشان يرفع البيانات

public class DataSeeder {

    public static void seedProducts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // قائمة المنتجات
        addProduct(db, "كيكة الشوكولاتة المنزلية", "كيكة شوكولاتة طازجة مصنوعة بوصفة عائلية أصيلة", 7, 4.8f, 32, "أكل وحلويات", true);
        addProduct(db, "معمول بالتمر", "معمول منزلي بحشوة التمر الطازج", 10, 4.9f, 45, "أكل وحلويات", true);
        addProduct(db, "بقلاوة بالفستق", "بقلاوة مصنوعة يدوياً بالفستق الحلبي الأصلي", 15, 4.7f, 28, "أكل وحلويات", false);
        addProduct(db, "خبز طابون منزلي", "خبز طابون طازج يومي مصنوع بالطريقة التقليدية", 10.5, 4.6f, 60, "أكل وحلويات", false);
        addProduct(db, "سوار كروشيه ملون", "سوار يدوي بألوان متعددة مصنوع بخيوط عالية الجودة", 2, 4.5f, 19, "إكسسوارات", true);
        addProduct(db, "قلادة خرز يدوية", "قلادة مصنوعة يدوياً من خرز طبيعي ملون", 4, 4.7f, 22, "إكسسوارات", false);
        addProduct(db, "حقيبة كروشيه", "حقيبة يد مصنوعة كروشيه بأشكال جميلة", 8, 4.8f, 15, "إكسسوارات", true);
        addProduct(db, "لوحة تطريز فلسطينية", "لوحة مطرزة يدوياً بأنماط فلسطينية تقليدية", 120, 5.0f, 12, "أشغال يدوية", true);
        addProduct(db, "دمية كروشيه للأطفال", "دمية ناعمة مصنوعة كروشيه آمنة للأطفال", 5, 4.9f, 38, "أطفال", true);
        addProduct(db, "بطانية كروشيه للأطفال", "بطانية دافئة مصنوعة كروشيه بألوان زاهية", 9, 4.7f, 20, "أطفال", false);
        addProduct(db, "صندوق هدايا مزين", "صندوق هدايا مزين يدوياً مناسب لجميع المناسبات", 7, 4.8f, 42, "هدايا", true);
        addProduct(db, "شمعة معطرة يدوية", "شمعة معطرة مصنوعة يدوياً بعطور طبيعية", 3, 4.6f, 55, "هدايا", false);
        addProduct(db, "إطار صور خشبي مزخرف", "إطار خشبي مزخرف يدوياً مناسب للهدايا والديكور", 5, 4.5f, 17, "ديكور", false);
        addProduct(db, "مزهرية سيراميك يدوية", "مزهرية مصنوعة ومزخرفة يدوياً من السيراميك", 6, 4.7f, 23, "ديكور", true);
        addProduct(db, "وسادة تطريز ملونة", "وسادة ديكور مطرزة يدوياً بأنماط شرقية جميلة", 7, 4.8f, 30, "ديكور", false);
    }

    private static void addProduct(FirebaseFirestore db, String name, String description,
                                   double price, float rating, int reviewCount,
                                   String category, boolean isFeatured) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("rating", rating);
        product.put("reviewCount", reviewCount);
        product.put("category", category);
        product.put("imageUrl", "");
        product.put("isFeatured", isFeatured);
        product.put("sellerId", "");
        product.put("sellerName", "");


        db.collection("products")
                .add(product)
                .addOnSuccessListener(ref ->
                        System.out.println(" تم رفع المنتج: ✅ " + name)
                )
                .addOnFailureListener(e ->
                        System.out.println(" فشل رفع المنتج: ❌ " + name + " - " + e.getMessage())
                );
    }
}