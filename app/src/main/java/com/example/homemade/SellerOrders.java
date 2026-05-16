package com.example.homemade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class SellerOrders extends AppCompatActivity {

    private RecyclerView rvSellerOrders;
    private LinearLayout layoutNoOrders;
    private ImageView btnBackOrders;

    private FirebaseFirestore db;
    private String userId;

    private List<Map<String, Object>> ordersList = new ArrayList<>();
    private OrdersAdapter ordersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        rvSellerOrders = findViewById(R.id.rvSellerOrders);
        layoutNoOrders = findViewById(R.id.layoutNoOrders);
        btnBackOrders  = findViewById(R.id.btnBackOrders);

        rvSellerOrders.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter();
        rvSellerOrders.setAdapter(ordersAdapter);

        btnBackOrders.setOnClickListener(v -> finish());
        loadOrders();
    }

    private void loadOrders() {
        db.collection("orders")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ordersList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ordersList.add(doc.getData());
                    }
                    ordersAdapter.notifyDataSetChanged();

                    if (ordersList.isEmpty()) {
                        layoutNoOrders.setVisibility(View.VISIBLE);
                        rvSellerOrders.setVisibility(View.GONE);
                    } else {
                        layoutNoOrders.setVisibility(View.GONE);
                        rvSellerOrders.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خطأ في تحميل الطلبات", Toast.LENGTH_SHORT).show()
                );
    }

    class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(SellerOrders.this)
                    .inflate(R.layout.item_seller_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Map<String, Object> order = ordersList.get(position);

            Object name    = order.get("customerName");
            Object phone   = order.get("customerPhone");
            Object address = order.get("customerAddress");
            Object payment = order.get("paymentMethod");
            Object total   = order.get("totalAmount");
            Object status  = order.get("status");

            holder.tvOrderId.setText("طلب #" + (position + 1));
            holder.tvCustomerName.setText(name != null ? name.toString() : "-");
            holder.tvCustomerPhone.setText(phone != null ? phone.toString() : "-");
            holder.tvCustomerAddress.setText(address != null ? address.toString() : "-");
            holder.tvPaymentMethod.setText(payment != null ? payment.toString() : "-");
            holder.tvOrderTotal.setText(total != null ?
                    String.format("%.2f JD", ((Number) total).doubleValue()) : "0 JD");

            if ("paid".equals(status)) {
                holder.tvOrderStatus.setText("مدفوع ✅");
                holder.tvOrderStatus.setBackgroundColor(0xFFE8F5E9);
                holder.tvOrderStatus.setTextColor(0xFF2E7D32);
            } else {
                holder.tvOrderStatus.setText("معلق ⏳");
                holder.tvOrderStatus.setBackgroundColor(0xFFFFF8E1);
                holder.tvOrderStatus.setTextColor(0xFFF57F17);
            }
        }

        @Override
        public int getItemCount() { return ordersList.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvCustomerName, tvCustomerPhone,
                    tvCustomerAddress, tvPaymentMethod, tvOrderTotal, tvOrderStatus;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId         = itemView.findViewById(R.id.tvOrderId);
                tvCustomerName    = itemView.findViewById(R.id.tvCustomerName);
                tvCustomerPhone   = itemView.findViewById(R.id.tvCustomerPhone);
                tvCustomerAddress = itemView.findViewById(R.id.tvCustomerAddress);
                tvPaymentMethod   = itemView.findViewById(R.id.tvPaymentMethod);
                tvOrderTotal      = itemView.findViewById(R.id.tvOrderTotal);
                tvOrderStatus     = itemView.findViewById(R.id.tvOrderStatus);
            }
        }
    }
}