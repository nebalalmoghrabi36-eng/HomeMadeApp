package com.example.homemade.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvPrice.setText(product.getPrice() + " JD");
        holder.ratingBar.setRating(product.getRating());
        holder.tvRatingCount.setText("(" + product.getReviewCount() + ")");
        holder.tvBadge.setVisibility(product.isFeatured() ? View.VISIBLE : View.GONE);

        // ✅ عرض الصورة من Base64
        loadImage(product.getImageUrl(), holder.imgProduct);

        if (isSellerMode) {
            holder.btnAddToCart.setText("🗑 حذف");
            holder.btnAddToCart.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFE53935)
            );
            holder.btnAddToCart.setOnClickListener(v -> deleteProduct(product, position));
        } else {
            holder.btnAddToCart.setText("أضف للسلة");
            holder.btnAddToCart.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2E7D32)
            );
            holder.btnAddToCart.setOnClickListener(v -> addToCart(product));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetail.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("name", product.getName());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("reviewCount", product.getReviewCount());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("sellerId", product.getSellerId());
            intent.putExtra("sellerName", product.getSellerName());
            intent.putExtra("imageBase64", product.getImageUrl()); // ✅ تمرير الصورة
            context.startActivity(intent);
        });
    }

    // ✅ تحويل Base64 لصورة وعرضها
    private void loadImage(String base64, ImageView imageView) {
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {}
        }
        imageView.setImageResource(R.drawable.ic_image_placeholder);
    }

    private void addToCart(Product product) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(context, "يجب تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", product.getId());
        cartItem.put("productName", product.getName());
        cartItem.put("price", product.getPrice());
        cartItem.put("userId", userId);
        cartItem.put("quantity", 1);
        cartItem.put("sellerId", product.getSellerId() != null ? product.getSellerId() : "");
        cartItem.put("sellerName", product.getSellerName() != null ? product.getSellerName() : "-");

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
        ImageView imgProduct; // ✅ مضاف

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge       = itemView.findViewById(R.id.tvBadge);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvRatingCount = itemView.findViewById(R.id.tvRatingCount);
            ratingBar     = itemView.findViewById(R.id.ratingBar);
            btnAddToCart  = itemView.findViewById(R.id.btnAddToCart);
            imgProduct    = itemView.findViewById(R.id.imgProduct); // ✅ مضاف
        }
    }
}