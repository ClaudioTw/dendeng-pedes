package com.hoqii.fxpc.sales.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.hoqii.fxpc.sales.R;
import com.hoqii.fxpc.sales.SignageVariables;
import com.hoqii.fxpc.sales.adapter.ReceiveAdapter;
import com.hoqii.fxpc.sales.adapter.ReturnAdapter;
import com.hoqii.fxpc.sales.core.LogInformation;
import com.hoqii.fxpc.sales.core.commons.Site;
import com.hoqii.fxpc.sales.entity.Order;
import com.hoqii.fxpc.sales.entity.Receive;
import com.hoqii.fxpc.sales.entity.Return;
import com.hoqii.fxpc.sales.entity.Shipment;
import com.hoqii.fxpc.sales.event.GenericEvent;
import com.hoqii.fxpc.sales.util.AuthenticationCeck;
import com.hoqii.fxpc.sales.util.AuthenticationUtils;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.TypiconsIcons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meruvian.midas.core.service.TaskService;
import org.meruvian.midas.core.util.ConnectionUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by akm on 13/04/16.
 */
public class ReturnListActivity extends AppCompatActivity implements TaskService {

    private int requestDetailCode = 101;
    private static final int REFRESH_TOKEN_RECEIVE_LIST = 301;

    private List<Return> returnList = new ArrayList<Return>();
    private SharedPreferences preferences;
    private RecyclerView recyclerView;
    private ReturnAdapter returnAdapter;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout dataNull, dataFailed;
    private ProgressDialog loadProgress;
    private int page = 1, totalPage;
    private AuthenticationCeck authenticationCeck = new AuthenticationCeck();

    private String receiveUrl = "/api/order/receives";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_list);
        EventBus.getDefault().register(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Explode());
            getWindow().setExitTransition(new Explode());
        }

        preferences = getSharedPreferences(SignageVariables.PREFS_SERVER, 0);

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Return");
        actionBar.setHomeAsUpIndicator(new IconDrawable(this, TypiconsIcons.typcn_chevron_left).colorRes(R.color.white).actionBarSize());

        returnAdapter = new ReturnAdapter(this);

        recyclerView = (RecyclerView) findViewById(R.id.receive_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(returnAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiptRefress);
        swipeRefreshLayout.setColorSchemeResources(R.color.green, R.color.yellow, R.color.blue, R.color.red);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        dataNull = (LinearLayout) findViewById(R.id.dataNull);
        dataFailed = (LinearLayout) findViewById(R.id.dataFailed);

        loadProgress = new ProgressDialog(this);
        loadProgress.setMessage(getResources().getString(R.string.message_fetch_data));
        loadProgress.setCancelable(false);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (authenticationCeck.isAccess()) {
                    ReceiveSync receiveSync = new ReceiveSync(ReturnListActivity.this, ReturnListActivity.this, false);
                    receiveSync.execute("0");
                    Log.d(getClass().getSimpleName(), "[ acces true / refreshing token not needed]");
                } else {
                    Log.d(getClass().getSimpleName(), "[ acces false / refreshing token]");
                    authenticationCeck.refreshToken(ReturnListActivity.this, REFRESH_TOKEN_RECEIVE_LIST);
                }
            }
        });

    }

    @Override
    public void onExecute(int code) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSuccess(int code, Object result) {
        swipeRefreshLayout.setRefreshing(false);
        returnAdapter.addItems(returnList);
        dataFailed.setVisibility(View.GONE);

        if (returnList.size() > 0) {
            dataNull.setVisibility(View.GONE);
        } else {
            dataNull.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCancel(int code, String message) {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onError(int code, String message) {
        swipeRefreshLayout.setRefreshing(false);
        dataFailed.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    class ReceiveSync extends AsyncTask<String, Void, JSONObject> {

        private Context context;
        private TaskService taskService;
        private boolean isLoadMore = false;

        public ReceiveSync(Context context, TaskService taskService, boolean isLoadMore) {
            this.context = context;
            this.taskService = taskService;
            this.isLoadMore = isLoadMore;
        }


        @Override
        protected JSONObject doInBackground(String... JsonObject) {
            Log.d(getClass().getSimpleName(), "?acces_token= " + AuthenticationUtils.getCurrentAuthentication().getAccessToken());
            return ConnectionUtil.get(preferences.getString("server_url", "") + receiveUrl + "?access_token="
                    + AuthenticationUtils.getCurrentAuthentication().getAccessToken() + "&page=" + JsonObject[0]);
        }

        @Override
        protected void onCancelled() {
            taskService.onCancel(SignageVariables.RECEIVE_GET_TASK, "Batal");
        }

        @Override
        protected void onPreExecute() {
            if (!isLoadMore) {
                swipeRefreshLayout.setRefreshing(true);
            }
            taskService.onExecute(SignageVariables.RECEIVE_GET_TASK);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            try {
                if (result != null) {

                    List<Return> returns = new ArrayList<Return>();

                    totalPage = result.getInt("totalPages");
                    Log.d("result order =====", result.toString());
                    JSONArray jsonArray = result.getJSONArray("content");
                    for (int a = 0; a < jsonArray.length(); a++) {
                        JSONObject object = jsonArray.getJSONObject(a);

                        Return aReturn = new Return();
                        aReturn.setId(object.getString("id"));

                        JSONObject logInformationObject = new JSONObject();
                        if (!object.isNull("logInformation")) {
                            logInformationObject = object.getJSONObject("logInformation");

                            LogInformation logInformation = new LogInformation();
                            logInformation.setCreateDate(new Date(logInformationObject.getLong("createDate")));

                            aReturn.setLogInformation(logInformation);
                        }

                        JSONObject shipmentObject = new JSONObject();
                        if (!object.isNull("shipment")) {
                            shipmentObject = object.getJSONObject("shipment");
                            Shipment shipment = new Shipment();
                            shipment.setId(shipmentObject.getString("id"));
                            shipment.setReceiptNumber(shipmentObject.getString("receiptNumber"));
                            shipment.setDeliveryServiceName(shipmentObject.getString("deliveryServiceName"));
                            if (shipmentObject.getString("status").equalsIgnoreCase("WAIT")) {
                                shipment.setStatus(Shipment.ShipmentStatus.WAIT);
                            } else if (shipmentObject.getString("status").equalsIgnoreCase("DELIVERED")) {
                                shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
                            } else if (shipmentObject.getString("status").equalsIgnoreCase("FAILED")) {
                                shipment.setStatus(Shipment.ShipmentStatus.FAILED);
                            }

                            JSONObject shipmentLogInformationObject = new JSONObject();
                            if (!shipmentObject.isNull("logInformation")) {
                                shipmentLogInformationObject = shipmentObject.getJSONObject("logInformation");

                                LogInformation logInformation = new LogInformation();
                                logInformation.setCreateDate(new Date(shipmentLogInformationObject.getLong("createDate")));

                                shipment.setLogInformation(logInformation);
                            }

                            aReturn.setShipment(shipment);
                        }

                        if (object.getString("status").equalsIgnoreCase("WAIT")) {
                            aReturn.setStatus(Return.ReturnStatus.WAIT);
                        } else if (object.getString("status").equalsIgnoreCase("RECEIVED")) {
                            aReturn.setStatus(Return.ReturnStatus.RECEIVED);
                        } else if (object.getString("status").equalsIgnoreCase("FAILED")) {
                            aReturn.setStatus(Return.ReturnStatus.FAILED);
                        }

                        aReturn.setRecipient(object.getString("recipient"));

                        JSONObject orderObject = new JSONObject();
                        if (!object.isNull("order")) {
                            orderObject = object.getJSONObject("order");

                            Order order = new Order();
                            order.setId(orderObject.getString("id"));
                            order.setReceiptNumber(orderObject.getString("receiptNumber"));

                            JSONObject orderLogInformationObject = new JSONObject();
                            if (!orderObject.isNull("logInformation")) {
                                orderLogInformationObject = orderObject.getJSONObject("logInformation");

                                LogInformation logInformation = new LogInformation();
                                logInformation.setCreateDate(new Date(orderLogInformationObject.getLong("createDate")));

                                order.setLogInformation(logInformation);
                            }

                            JSONObject siteObject = new JSONObject();
                            if (!orderObject.isNull("site")) {
                                siteObject = orderObject.getJSONObject("site");

                                Site site = new Site();
                                site.setId(siteObject.getString("id"));
                                site.setName(siteObject.getString("name"));
                                site.setDescription(siteObject.getString("description"));
                                order.setSite(site);
                            }
                            aReturn.setOrder(order);
                        }
                        returns.add(aReturn);
                    }

                    if (isLoadMore) {
                        page++;
                        loadProgress.dismiss();
                    }

                    returnList = returns;
                    taskService.onSuccess(SignageVariables.RECEIVE_GET_TASK, true);
                } else {
                    taskService.onError(SignageVariables.RECEIVE_GET_TASK, "Error");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                taskService.onError(SignageVariables.RECEIVE_GET_TASK, "Error");
            }


        }
    }

    private void refreshContent() {
        returnAdapter = new ReturnAdapter(this);
        recyclerView.setAdapter(returnAdapter);

        for (int x = 0; x < page; x++) {
            final int finalX = x;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ReceiveSync receiveSync = new ReceiveSync(ReturnListActivity.this, ReturnListActivity.this, false);
                    receiveSync.execute(Integer.toString(finalX));
                }
            }, 500);
        }
    }

    public void loadMoreContent() {
        if (page < totalPage) {
            loadProgress.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ReceiveSync receiveSync = new ReceiveSync(ReturnListActivity.this, ReturnListActivity.this, true);
                    receiveSync.execute(Integer.toString(page));
                }
            }, 500);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == requestDetailCode) {
            Log.d(getClass().getSimpleName(), "result ok");
            if (data != null) {
                String receiveId = data.getStringExtra("receiveId");
                Log.d(getClass().getSimpleName(), "receive id " + receiveId);
                returnAdapter.updateStatusDelivered(receiveId);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void openReceiveDetail(Intent intent) {
        startActivityForResult(intent, requestDetailCode);
    }

    public void onEventMainThread(GenericEvent.RequestSuccess requestSuccess) {
        Log.d(getClass().getSimpleName(), "RequestSuccess: " + requestSuccess.getProcessId());
        switch (requestSuccess.getProcessId()) {
            case REFRESH_TOKEN_RECEIVE_LIST:
                ReceiveSync receiveSync = new ReceiveSync(ReturnListActivity.this, ReturnListActivity.this, false);
                receiveSync.execute("0");
                break;
        }

    }
}