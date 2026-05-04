package com.example.homemade.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homemade.Login;
import com.example.homemade.ProductDetail;
import com.example.homemade.R;
import com.example.homemade.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private boolean isSellerMode;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.isSellerMode = false;
    }

    public ProductAdapter(Context context, List<Product> productList, boolean isSellerMode) {
        this.context = context;
        this.productList = productList;
        this.isSellerMode = isSellerMode;
    }

    public void updateData(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductDesc.setText(product.getDescription());
        holder.tvPrice.setText(product.getPrice() + " JOD");
        holder.ratingBar.setRating(product.getRating());
        holder.tvRatingCount.setText("(" + product.getReviewCount() + ")");
        holder.tvBadge.setVisibility(product.isFeatured() ? View.VISIBLE : View.GONE);

        if (isSellerMode) {
            // وضع البائع - زر الحذف
            holder.btnAddToCart.setText("🗑 حذف");
            holder.btnAddToCart.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFE53935)
            );
            holder.btnAddToCart.setOnClickListener(v -> deleteProduct(product, position));
        } else {
            // وضع الزبون - إضافة للسلة بـ Firestore
            holder.btnAddToCart.setText("أضف للسلة");
            holder.btnAddToCart.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2E7D32)
            );
            holder.btnAddToCart.setOnClickListener(v -> addToCart(product));
        }

        // عند الضغط على المنتج - فتح صفحة التفاصيل
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetail.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("name", product.getName());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("reviewCount", product.getReviewCount());
            intent.putExtra("category", product.getCategory());
            context.startActivity(intent);
        });
    }

    private void addToCart(Product product) {

            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (userId == null) {
                Toast.makeText(context, "يجب تسجيل الدخول أولاً للإضافة للسلة!", Toast.LENGTH_LONG).show();
                // انتقل لصفحة Login
                Intent intent = new Intent(context, Login.class);
                context.startActivity(intent);
                return;
            }



        // حفظ في Firestore
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", product.getId());
        cartItem.put("productName", product.getName());
        cartItem.put("price", product.getPrice());
        cartItem.put("userId", userId);
        cartItem.put("quantity", 1);

        FirebaseFirestore.getInstance()
                .collection("cart")
                .add(cartItem)
                .addOnSuccessListener(ref ->
                        Toast.makeText(context, "✅ تمت إضافة " + product.getName() + " للسلة", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context, "خطأ في الإضافة للسلة", Toast.LENGTH_SHORT).show()
                );
    }

    private void deleteProduct(Product product, int position) {
        FirebaseFirestore.getInstance()
                .collection("products")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    productList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "تم حذف المنتج ✅", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "خطأ في الحذف", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public int getItemCount() { return productList != null ? productList.size() : 0; }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvProductName, tvProductDesc, tvPrice, tvRatingCount;
        RatingBar ratingBar;
        Button btnAddToCart;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge       = itemView.findViewById(R.id.tvBadge);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvRatingCount = itemView.findViewById(R.id.tvRatingCount);
            ratingBar     = itemView.findViewById(R.id.ratingBar);
            btnAddToCart  = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}