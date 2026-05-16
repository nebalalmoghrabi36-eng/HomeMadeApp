package com.example.homemade;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cart extends AppCompatActivity {

    private RecyclerView rvCart;
    private TextView tvTotal;
    private Button btnCheckout;
    private ImageView btnBackCart;
    private LinearLayout layoutEmptyCart, layoutOrderSummary;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private List<Map<String, Object>> cartItems = new ArrayList<>();
    private List<String> cartDocIds = new ArrayList<>();
    private CartAdapter cartAdapter;
    private double totalPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "يجب تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvCart             = findViewById(R.id.rvCart);
        tvTotal            = findViewById(R.id.tvTotal);
        btnCheckout        = findViewById(R.id.btnCheckout);
        btnBackCart        = findViewById(R.id.btnBackCart);
        layoutEmptyCart    = findViewById(R.id.layoutEmptyCart);
        layoutOrderSummary = findViewById(R.id.layoutOrderSummary);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter();
        rvCart.setAdapter(cartAdapter);

        btnBackCart.setOnClickListener(v -> finish());

        loadCart();

        // إتمام الطلب → صفحة Checkout
        btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "السلة فارغة!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Cart.this, Checkout.class);
            intent.putExtra("totalPrice", totalPrice);
            startActivity(intent);
        });
    }

    private void loadCart() {
        if (userId == null) return;

        db.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    cartItems.clear();
                    cartDocIds.clear();
                    totalPrice = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> item = doc.getData();
                        cartItems.add(item);
                        cartDocIds.add(doc.getId());

                        Object priceObj = item.get("price");
                        if (priceObj instanceof Number) {
                            totalPrice += ((Number) priceObj).doubleValue();
                        }
                    }

                    cartAdapter.notifyDataSetChanged();
                    tvTotal.setText(String.format("%.2f JD", totalPrice));

                    if (cartItems.isEmpty()) {
                        layoutEmptyCart.setVisibility(View.VISIBLE);
                        rvCart.setVisibility(View.GONE);
                        layoutOrderSummary.setVisibility(View.GONE);
                    } else {
                        layoutEmptyCart.setVisibility(View.GONE);
                        rvCart.setVisibility(View.VISIBLE);
                        layoutOrderSummary.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خطأ في تحميل السلة", Toast.LENGTH_SHORT).show()
                );
    }

    private void removeItem(int position) {
        String docId = cartDocIds.get(position);
        db.collection("cart").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Object priceObj = cartItems.get(position).get("price");
                    if (priceObj instanceof Number) {
                        totalPrice -= ((Number) priceObj).doubleValue();
                    }
                    cartItems.remove(position);
                    cartDocIds.remove(position);
                    cartAdapter.notifyItemRemoved(position);
                    tvTotal.setText(String.format("%.2f JD", totalPrice));

                    if (cartItems.isEmpty()) {
                        layoutEmptyCart.setVisibility(View.VISIBLE);
                        rvCart.setVisibility(View.GONE);
                        layoutOrderSummary.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Cart Adapter ──────────────────────────────────────────────────────────
    class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

        @NonNull
        @Override
        public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(Cart.this)
                    .inflate(R.layout.item_cart, parent, false);
            return new CartViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
            Map<String, Object> item = cartItems.get(position);

            Object name   = item.get("productName");
            Object price  = item.get("price");
            Object seller = item.get("sellerName");

            holder.tvCartProductName.setText(name != null ? name.toString() : "منتج");
            holder.tvCartPrice.setText(price != null ? String.format("%.2f JD", ((Number) price).doubleValue()) : "0 JD");
            holder.tvCartSellerName.setText("البائع: " + (seller != null ? seller.toString() : "-"));

            holder.btnRemoveItem.setOnClickListener(v -> removeItem(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return cartItems.size(); }

        class CartViewHolder extends RecyclerView.ViewHolder {
            TextView tvCartProductName, tvCartPrice, tvCartSellerName, btnRemoveItem;

            CartViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCartProductName  = itemView.findViewById(R.id.tvCartProductName);
                tvCartPrice        = itemView.findViewById(R.id.tvCartPrice);
                tvCartSellerName   = itemView.findViewById(R.id.tvCartSellerName);
                btnRemoveItem      = itemView.findViewById(R.id.btnRemoveItem);
            }
        }
    }
}