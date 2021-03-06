package com.hoqii.fxpc.sales.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hoqii.fxpc.sales.R;
import com.hoqii.fxpc.sales.SignageApplication;
import com.hoqii.fxpc.sales.content.database.adapter.SerialNumberDatabaseAdapter;
import com.hoqii.fxpc.sales.entity.OrderMenuSerial;
import com.hoqii.fxpc.sales.job.ReturnJob;
import com.path.android.jobqueue.JobManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akm on 19/04/16.
 */
public class ReturnOrderMenuAdapter extends RecyclerView.Adapter<ReturnOrderMenuAdapter.ViewHolder> {

    private Context context;
    private List<OrderMenuSerial> orderMenuSerialList = new ArrayList<OrderMenuSerial>();
    private List<OrderMenuSerial> orderMenuSerials = new ArrayList<OrderMenuSerial>();
    private List<String> tempSerialNumberList = new ArrayList<String>();
    private SerialNumberDatabaseAdapter serialNumberDatabaseAdapter;
    private boolean verify = false;
    private boolean isMinLoli = false;
    private OrderMenuSerial orderMenuSerial;
    private String site;
    private JobManager jobManager;

    public ReturnOrderMenuAdapter(Context context, String orderId) {
        this.context = context;

        serialNumberDatabaseAdapter = new SerialNumberDatabaseAdapter(context);
        List<OrderMenuSerial> sn = serialNumberDatabaseAdapter.getSerialNumberListByOrderId(orderId);
        for (OrderMenuSerial s : sn) {
            tempSerialNumberList.add(s.getSerialNumber());
        }

    }

    public ReturnOrderMenuAdapter(Context context, String orderId, boolean verify) {
        this.context = context;
        this.verify = verify;

        serialNumberDatabaseAdapter = new SerialNumberDatabaseAdapter(context);
        List<OrderMenuSerial> sn = serialNumberDatabaseAdapter.getSerialNumberListByOrderId(orderId);
        for (OrderMenuSerial s : sn) {
            tempSerialNumberList.add(s.getSerialNumber());
        }
    }

    @Override
    public ReturnOrderMenuAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_return_order_menu_list, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ReturnOrderMenuAdapter.ViewHolder holder, final int position) {
        holder.productName.setText(context.getString(R.string.holder_product) + orderMenuSerialList.get(position).getOrderMenu().getProduct().getName());
        holder.productSerial.setText(context.getString(R.string.holder_serial) + orderMenuSerialList.get(position).getSerialNumber());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            isMinLoli = true;
        } else {
            isMinLoli = false;
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Retur");
                builder.setMessage("Are you sure to return this item ?");
                View view = LayoutInflater.from(context).inflate(R.layout.view_return_desc, null);
                final TextView orderDesc = (TextView) view.findViewById(R.id.order_desc);
                orderDesc.setText("");
                builder.setView(view);
                builder.setTitle("Return Items");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        jobManager = SignageApplication.getInstance().getJobManager();
                        Log.d("SerialId", orderMenuSerialList.get(position).getId());
                        Log.d("siteToId", site);
                        jobManager.addJobInBackground(new ReturnJob(site, orderDesc.getText().toString(), orderMenuSerialList.get(position).getId()));
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        if (tempSerialNumberList.contains(orderMenuSerialList.get(position).getSerialNumber())) {
            holder.imageStatus.setVisibility(View.VISIBLE);
        } else {
            holder.imageStatus.setVisibility(View.GONE);
        }

        if (verify == true) {
            holder.imageStatus.setVisibility(View.VISIBLE);
        }

        if (position == 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(10, 24, 10, 0);
            holder.layout.setLayoutParams(params);
        } else {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(10, 0, 10, 0);
            holder.layout.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return orderMenuSerialList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView productName, productSerial;
        private ImageView imageStatus;
        private LinearLayout layout;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = (TextView) itemView.findViewById(R.id.om_name);
            productSerial = (TextView) itemView.findViewById(R.id.om_serial);
            imageStatus = (ImageView) itemView.findViewById(R.id.ol_img);
            layout = (LinearLayout) itemView.findViewById(R.id.layout);
        }
    }

    public void addItems(List<OrderMenuSerial> orderMenuSerials, String siteTo) {
        for (OrderMenuSerial s : orderMenuSerials) {
            if (!orderMenuSerialList.contains(s)) {
                orderMenuSerialList.add(s);
            }
            site = siteTo;
        }
        if (tempSerialNumberList.size() == orderMenuSerialList.size()) {
            this.verify = true;
        }
        notifyDataSetChanged();
    }

    public void addItem(OrderMenuSerial orderMenuSerial){
        orderMenuSerials.add(orderMenuSerial);
        Log.d("CEK VALUE", orderMenuSerial.getOrderMenu().getProduct().getName());
    }

}
