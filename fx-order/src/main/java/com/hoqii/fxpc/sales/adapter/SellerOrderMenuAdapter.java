package com.hoqii.fxpc.sales.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hoqii.fxpc.sales.R;
import com.hoqii.fxpc.sales.SignageVariables;
import com.hoqii.fxpc.sales.activity.ScannerActivityCustom;
import com.hoqii.fxpc.sales.activity.SellerOrderMenuListActivity;
import com.hoqii.fxpc.sales.content.database.adapter.SerialNumberDatabaseAdapter;
import com.hoqii.fxpc.sales.entity.OrderMenu;
import com.hoqii.fxpc.sales.entity.OrderMenuSerial;
import com.hoqii.fxpc.sales.util.AuthenticationUtils;
import com.joanzapata.iconify.widget.IconTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miftakhul on 12/6/15.
 */
public class SellerOrderMenuAdapter extends RecyclerView.Adapter<SellerOrderMenuAdapter.ViewHolder> {
    private Context context;
    private String orderId;
    private List<OrderMenu> orderMenuList = new ArrayList<OrderMenu>();
    private List<String> orderMenuListSerial = new ArrayList<String>();
    private List<OrderMenuSerial> orderMenuSerials = new ArrayList<OrderMenuSerial>();
    private SerialNumberDatabaseAdapter serialNumberDatabaseAdapter;
    private SharedPreferences preferences;

    public SellerOrderMenuAdapter(Context context, String orderId) {
        this.context = context;
        this.orderId = orderId;

        serialNumberDatabaseAdapter = new SerialNumberDatabaseAdapter(context);
        orderMenuSerials = serialNumberDatabaseAdapter.getSerialNumberListByOrderId(orderId);
        for (int x = 0; x < orderMenuSerials.size(); x++) {
            String id = orderMenuSerials.get(x).getOrderMenu().getId();
            if (!orderMenuListSerial.contains(id)){
                orderMenuListSerial.add(id);
            }
        }

        preferences = context.getSharedPreferences(SignageVariables.PREFS_SERVER, 0);
    }


//    public SellerOrderMenuAdapter(Context context, String orderId, List<OrderMenu> orderMenus) {
//        this.context = context;
//        this.orderId = orderId;
//        this.orderMenuList = orderMenus;
//
//        orderMenuListSerial.add("default");
//        serialNumberDatabaseAdapter = new SerialNumberDatabaseAdapter(context);
//        orderMenuSerials = serialNumberDatabaseAdapter.getSerialNumberListByOrderId(orderId);
//        for (int x = 0; x < orderMenuSerials.size(); x++) {
//            String id = orderMenuSerials.get(x).getOrderMenu().getId();
//            if (!orderMenuListSerial.contains(id)){
//                orderMenuListSerial.add(id);
//            }
//        }
//    }
//
//    public SellerOrderMenuAdapter(Context context, String orderId, List<OrderMenu> orderMenus, List<String> orderMenuListSerial) {
//        this.context = context;
//        this.orderId = orderId;
//        this.orderMenuList = orderMenus;
//        this.orderMenuListSerial = orderMenuListSerial;
//
//        orderMenuListSerial.add("default");
//        serialNumberDatabaseAdapter = new SerialNumberDatabaseAdapter(context);
//        orderMenuSerials = serialNumberDatabaseAdapter.getSerialNumberListByOrderId(orderId);
//        for (int x = 0; x < orderMenuSerials.size(); x++) {
//            orderMenuListSerial.add(orderMenuSerials.get(x).getOrderMenu().getId());
//        }
//    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_seller_order_menu_list, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.productName.setText(orderMenuList.get(position).getProduct().getName());
        if (orderMenuList.get(position).getDescription().equals("")){
            holder.description.setText(context.getString(R.string.hoder_description)+" No description");
        }else {
            holder.description.setText(context.getString(R.string.hoder_description) + orderMenuList.get(position).getDescription());
        }
        holder.productCount.setText("{typcn-shopping-cart} " + Integer.toString(orderMenuList.get(position).getQty()));
        holder.scanCount.setText("{typcn-tick-outline} 0");

        String imageUrl = preferences.getString("server_url", "")+"/api/products/"+orderMenuList.get(position).getProduct().getId() + "/image?access_token="+ AuthenticationUtils.getCurrentAuthentication().getAccessToken();

        for (String orderMenuId : orderMenuListSerial){
            if (orderMenuList.get(position).getId().equalsIgnoreCase(orderMenuId)) {
                List<OrderMenuSerial> serials = serialNumberDatabaseAdapter.getSerialNumberListByOrderIdAndOrderMenuId(orderId, orderMenuId);
                if (serials.size() > 0){
                    holder.scanCount.setText("{typcn-tick-outline} " + serials.size());
                    holder.scanCount.setTextColor(context.getResources().getColor(R.color.colorAccent));
                }else {
                    holder.scanCount.setText("{typcn-tick-outline} 0");
                    holder.scanCount.setTextColor(context.getResources().getColor(R.color.grey));
                }
                break;
            } else {
                holder.scanCount.setText("{typcn-tick-outline} 0");
                holder.scanCount.setTextColor(context.getResources().getColor(R.color.grey));
            }
        }

        Glide.with(context).load(imageUrl).error(R.drawable.ic_description_24dp).into(holder.preview);

        final List<Integer> count = new ArrayList<Integer>();
        int maxCount = orderMenuList.get(position).getQty();

        for (int x = 1; x <= maxCount; x++) {
            count.add(x);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent(context, ScannerActivityCustom.class);
                data.putExtra("productId", orderMenuList.get(position).getProduct().getId());
                data.putExtra("productName", orderMenuList.get(position).getProduct().getName());
                data.putExtra("productQty", orderMenuList.get(position).getQty());
                data.putExtra("orderMenuId", orderMenuList.get(position).getId());
                data.putExtra("position", position);

//                ((SellerOrderMenuListActivity) context).openScanner(data);
                ((SellerOrderMenuListActivity) context).inputSerial(data);
            }
        });

        if (position == 0){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(10,24,10,0);
            holder.layout.setLayoutParams(params);
        }else{
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(10,0,10,0);
            holder.layout.setLayoutParams(params);
        }

        if (position == orderMenuList.size() - 1){
            ((SellerOrderMenuListActivity)context).loadMoreContent();
        }
    }

    @Override
    public int getItemCount() {
        return orderMenuList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView productName, description;
        private IconTextView productCount, scanCount;
        private ImageView preview;
        private LinearLayout layout;
//        private ImageView imageStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = (TextView) itemView.findViewById(R.id.om_name);
            productCount = (IconTextView) itemView.findViewById(R.id.om_count);
            scanCount = (IconTextView) itemView.findViewById(R.id.om_count_scan);
//            imageStatus = (ImageView) itemView.findViewById(R.id.ol_img);
            description = (TextView) itemView.findViewById(R.id.om_description);
            preview = (ImageView) itemView.findViewById(R.id.om_preview);
            layout = (LinearLayout) itemView.findViewById(R.id.layout);
        }
    }

    public void addItems(List<OrderMenu> orderMenus){
        for (OrderMenu o : orderMenus){
            if (!orderMenuList.contains(o)){
                orderMenuList.add(o);
                notifyDataSetChanged();
            }
        }
    }

    public void addOrderMenuSerial(String orderMenuId) {
        if (!orderMenuListSerial.contains(orderMenuId)) {
            for (String s : orderMenuListSerial) {
            }
            orderMenuListSerial.add(orderMenuId);
            for (String s : orderMenuListSerial) {
            }
        }
    }

    public void updateOrderMenuSerial(List<String> orderMenuserilsIds){
        this.orderMenuListSerial = orderMenuserilsIds;
        if (orderMenuListSerial.size() > 0){
            for (int x = 0; x < orderMenuList.size(); x++){
                orderMenuListSerial.contains(orderMenuList.get(x).getId());
                notifyItemChanged(x);
            }
        }else {
            notifyDataSetChanged();
        }
    }

    public void removeOrderMenuSerial(String orderMenuId) {

        for (int x = 0; x < orderMenuListSerial.size(); x++){
            if (orderMenuListSerial.get(x).equalsIgnoreCase(orderMenuId)){
                orderMenuListSerial.remove(x);
                break;
            }else {
                Log.d(getClass().getSimpleName(), "item serial not found, so not removed");
            }
        }

    }


}
