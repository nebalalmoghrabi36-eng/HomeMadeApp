package com.example.homemade;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

import java.util.Map;

public class ChatBot extends AppCompatActivity {

    EditText editMessage;
    Button btnSend;
    TextView chatText;

    // مصفوفة من الخرائط (Maps) لتخزين بيانات المنتجات مباشرة من الفايرستور
    ArrayList<Map<String, Object>> productsList = new ArrayList<>();

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        chatText = findViewById(R.id.chatText);

        db = FirebaseFirestore.getInstance();

        // جلب المنتجات من الفايرستور
        getProductsFromFirestore();

        btnSend.setOnClickListener(v -> {
            String userMessage = editMessage.getText().toString();
            if (!userMessage.trim().isEmpty()) {
                String response = botReply(userMessage);

                chatText.append("\n\nأنت: " + userMessage);
                chatText.append("\nالبوت: " + response);

                editMessage.setText("");
            }
        });
    }

    private void getProductsFromFirestore() {

        db.collection("products").addSnapshotListener((value, error) -> {
            if (error != null) {
                chatText.append("\nالبوت: عذراً، فشل الاتصال بقاعدة البيانات.");
                return;
            }

            if (value != null) {
                productsList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    // تخزين البيانات بداخل Map لتفادي مشاكل كلاس Product والـ Getters
                    Map<String, Object> productData = doc.getData();
                    if (productData != null) {
                        productsList.add(productData);
                    }
                }
            }
        });
    }

    private String botReply(String message) {
        message = message.toLowerCase().trim();

        if (productsList.isEmpty()) {
            return "لحظة من فضلك، أقوم بتحميل المنتجات الحالية المتوفرة في المتجر...";
        }

        if (message.contains("أهلاً") || message.contains("مرحبا") || message.contains("سلام") || message.equals("الو")) {
            return "أهلاً بك يا غالي في متجر Homemade! أنا مساعدك الذكي. اكتب لي أي كلمة بتدور عليها (مثل: معمول، كيك، كروشيه) ورح أفرجيك كل المنتجات المطابقة وأسعارها فوراً!";
        }

        StringBuilder replyBuilder = new StringBuilder();
        int matchCount = 0;

        // البحث المرن داخل المصفوفة
        for (Map<String, Object> p : productsList) {
            // جلب القيم مباشرة باستخدام أسماء الحقول كما هي مخزنة في الفايرستور

            String name = p.get("pName") != null ? p.get("pName").toString().toLowerCase() :
                    (p.get("name") != null ? p.get("name").toString().toLowerCase() : "");

            String category = p.get("category") != null ? p.get("category").toString().toLowerCase() : "";
            String seller = p.get("sellerName") != null ? p.get("sellerName").toString() : "متجر Homemade";
            String city = p.get("city") != null ? p.get("city").toString() : "الأردن";
            String price = p.get("price") != null ? p.get("price").toString() : "0";

            // فحص مرن جداً: لو كتب "معمول" واسم المنتج يحتوي على "معمول" رح يلقطه ويجمعه
            if (name.contains(message) || category.contains(message)) {
                matchCount++;
                replyBuilder.append("\n\n")
                        .append("🛍️ المنتج (").append(matchCount).append("): ").append(p.get("pName") != null ? p.get("pName").toString() : p.get("name"))
                        .append("\n💰 السعر: ").append(price).append(" د.أ\n")
                        .append("🏪 اسم البائع: ").append(seller).append("\n")
                        .append("📌 مكان البيع: ").append(city).append("\n")
                        .append("------------------------");
            }
        }

        if (matchCount > 0) {
            return "لقيتلك هالمقترحات الرهيبة بناءً على طلبك: " + replyBuilder.toString();
        }

        return "عذراً، ما لقيت منتجات بتطابق كلمة '" + message + "' حالياً بالموقع. جرب اكتب كلمة ثانية (مثل: كيك، معمول، دمية).";
    }
}