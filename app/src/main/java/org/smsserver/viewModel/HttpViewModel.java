package org.smsserver.viewModel;

import static android.provider.Telephony.TextBasedSmsColumns.ADDRESS;
import static android.provider.Telephony.TextBasedSmsColumns.BODY;
import static android.provider.Telephony.TextBasedSmsColumns.STATUS;
import static android.provider.Telephony.TextBasedSmsColumns.SUBJECT;
import static android.provider.Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID;
import static android.provider.Telephony.TextBasedSmsColumns.THREAD_ID;
import static android.provider.Telephony.ThreadsColumns.DATE;
import static org.smsserver.config.Const.ADDRESS_SERVER;
import static org.smsserver.config.Const.DELIVERED_PING;
import static org.smsserver.config.Const.DELIVERED_SMS;
import static org.smsserver.config.Const.EVENT_INFO;
import static org.smsserver.config.Const.EVENT_NEW_CONNECT;
import static org.smsserver.config.Const.EVENT_REPORT;
import static org.smsserver.config.Const.EVENT_RESULT_OK;
import static org.smsserver.config.Const.INCOMMING_STATUS;
import static org.smsserver.config.Const.IN_PROGRESS;
import static org.smsserver.config.Const.PREF_UUID_IDENTIFY_DEVICE;
import static org.smsserver.config.Const.SENT_PING;
import static org.smsserver.config.Const.SENT_SMS;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import org.smsserver.Permissions;
import org.smsserver.model.Settings;
import org.smsserver.data.DataBase;
import org.smsserver.model.Device;
import org.smsserver.model.Events;
import org.smsserver.model.Message;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HttpViewModel extends ViewModel {

    final byte[] payload = new byte[]{0x0A, 0x06, 0x03, (byte) 0xB0, (byte) 0xAF, (byte) 0x82, 0x03, 0x06, 0x6A, 0x00, 0x05};

    private WebSocketClient webSocketClient;
    private SharedPreferences preferences;
    private Context context;
    private Permissions permissions;
    private DataBase dataBaseMassages = new DataBase();
    private Settings settings = new Settings();
    private Device device = new Device();
    private Events events = new Events();

    private PendingIntent sentPI;
    private PendingIntent deliveryPI;
    private Intent sentIntent; // = new Intent(SENT);
    private Intent delivIntent; // = new Intent(DELIVER);
    private Boolean isSent = false;
    private Boolean isConnect = false;
    public String serverAddress = null; // ws://185.181.165.238:8011/
    private URI uri;

    public MutableLiveData<String> logListLiveData = new MutableLiveData<>();
    public MutableLiveData<ArrayList<String>> permissionsLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> connectLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> idDeviceLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> serverAddressLiveData = new MutableLiveData<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    public HttpViewModel(Context context, SharedPreferences preferences) throws InterruptedException {
        this.context = context;
        this.preferences = preferences;
        permissions = new Permissions(this.context, this.preferences);
        initViewModel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initViewModel() {
        if (!permissions.checkPermissions()) {
            logListLiveData.postValue("Permissions not received");
            permissionsLiveData.postValue(permissions.getMissingPermissions());
        } else logListLiveData.postValue("Permissions received");

        device.setTitle(Build.MANUFACTURER);
        device.setModel(Build.MODEL);

        idDeviceLiveData.setValue(checkIdDevice());
        serverAddressLiveData.setValue(checkAddressServer());

        events.setToken(device.getToken());
    }

    public MutableLiveData<Boolean> getServerAddressLiveData() {
        if (serverAddressLiveData == null) {
            serverAddressLiveData = new MutableLiveData<>();
        }
        return serverAddressLiveData;
    }

    public MutableLiveData<Boolean> getIdDeviceLiveData() {
        if (idDeviceLiveData == null) {
            idDeviceLiveData = new MutableLiveData<>();
        }
        return idDeviceLiveData;
    }

    public MutableLiveData<String> getLogListLiveData() {
        if (logListLiveData == null) {
            logListLiveData = new MutableLiveData<>();
        }
        return logListLiveData;
    }

    public MutableLiveData<ArrayList<String>> getPermissionsLiveData() {
        if (permissionsLiveData == null) {
            permissionsLiveData = new MutableLiveData<>();
        }
        return permissionsLiveData;
    }

    public MutableLiveData<Boolean> getConnectLiveData() {
        if (connectLiveData == null) {
            connectLiveData = new MutableLiveData<>();
        }
        return connectLiveData;
    }

    private String getEvent(String _event, String data){
        events.setEvent(_event);
        events.setData(data);
        return events.toJson();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void connectToServerWebsocket() throws InterruptedException {

        try {
            uri = new URI(serverAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            logListLiveData.postValue(e.getMessage());
            return;
        }
        try {
            logListLiveData.postValue("Connecting to server...");
            Log.d("EVENT", getEvent(EVENT_NEW_CONNECT, device.toJson()));
            webSocketClient = createWebSocketClient();
            webSocketClient.connect();
        } catch (Exception e) {
            logListLiveData.postValue(e.getMessage());
            return;
        }
    }

    private WebSocketClient createWebSocketClient(){
        WebSocketClient webSocketClient = new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d("WS", "Opened ");
                send(getEvent(EVENT_NEW_CONNECT, device.toJson()));
                Log.d("WS", getEvent(EVENT_NEW_CONNECT, device.toJson()));
                isConnect = true;
                logListLiveData.postValue("Connection established");
                connectLiveData.postValue(isConnect);
            }

            @Override
            public void onMessage(String message) {
                Log.d("WS", "Incoming message: " + message);
                logListLiveData.postValue("Incoming message...");
                if (message.contains("numberOfThread")) {
                    logListLiveData.postValue("Updating settings...");
                    updatingSettings(message);
                }
                if (message.contains("recipient")) {
                    dataBaseMassages.uploadDataBase(message);
                    logListLiveData.setValue("Database upgraded...");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        sendSmsOnNumber();
                    }
                }
                if (message.contains("report")) {
                    logListLiveData.postValue("Report request:" + message);
                    sendAllReport(message);
                }
                send(getEvent(EVENT_INFO, EVENT_RESULT_OK));
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d("WS", "onClose " + code);
                logListLiveData.postValue("Connect closed...");
                connectLiveData.postValue(false);

            }

            @Override
            public void onError(Exception ex) {
                Log.d("WS", "onError " + ex.getMessage());
                logListLiveData.postValue("Connected failed..." + ex.getMessage());
                connectLiveData.postValue(false);
            }
        };
        return webSocketClient;
    }

    private void updatingSettings(String set){
        Gson gson = new Gson();
        Settings newSettings = gson.fromJson(set, Settings.class);
        settings.setNumberOfThread(newSettings.getNumberOfThread());
        settings.setInterval(newSettings.getInterval());
    }

    public Boolean checkIdDevice() {
        if (preferences.getString(PREF_UUID_IDENTIFY_DEVICE, "") == null
                || preferences.getString(PREF_UUID_IDENTIFY_DEVICE, "").equals("")) {
            return false;
        }
        device.setToken(preferences.getString(PREF_UUID_IDENTIFY_DEVICE, ""));
        logListLiveData.postValue(device.toString());
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendSmsOnNumber() {
        isSent = true;
        ArrayList<Message> tempMessages = new ArrayList<>();
        dataBaseMassages.getDataBaseMessage().forEach(message -> {
            if (Objects.equals(message.getStatus().toLowerCase(), INCOMMING_STATUS))
                tempMessages.add(message);
            Log.d("BEFORE SENT", message.toString());
        });

        new Thread(new Runnable() {
            public void run() {
                for (Message mes : tempMessages) {
                    try {
                        Thread.currentThread().sleep(settings.getInterval() * 1000);
                    } catch (InterruptedException e) {
                        logListLiveData.postValue(e.getMessage());
                    }

                    sentIntent = new Intent(mes.getSim() == 0 ? SENT_PING : SENT_SMS).putExtra("s_number", mes.getRecipient());
                    delivIntent = new Intent(mes.getSim() == 0 ? DELIVERED_PING : DELIVERED_SMS).putExtra("d_number", mes.getRecipient());
                    sentPI = PendingIntent.getBroadcast(context, 0x1337, sentIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    deliveryPI = PendingIntent.getBroadcast(context, 0x1337, delivIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    SubscriptionManager localSubscriptionManager = SubscriptionManager.from(context);
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                    SubscriptionInfo simInfo1 = (SubscriptionInfo) localList.get(0);
                    SubscriptionInfo simInfo2 = (SubscriptionInfo) localList.get(1);

                    try {
                        switch (mes.getSim()){
                            case 0: {
                                SmsManager.getDefault().sendDataMessage(mes.getRecipient(), null, (short) 9200, payload, sentPI, deliveryPI);
                                break;
                            }
                            case 1: {
                                SmsManager.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId())
                                        .sendTextMessage(mes.getRecipient(), null, mes.getMessage(), sentPI, deliveryPI);
                                break;
                            }
                            case 2: {
                                SmsManager.getSmsManagerForSubscriptionId(simInfo2.getSubscriptionId())
                                        .sendTextMessage(mes.getRecipient(), null, mes.getMessage(), sentPI, deliveryPI);
                                break;
                            }
                        }
                        mes.setStatus(IN_PROGRESS);
                        Log.d("IN PROGRESS", mes.toString());
                        logListLiveData.postValue(IN_PROGRESS + " " + mes.getRecipient());
                    } catch (Exception e) {
                        webSocketClient.send(getEvent(EVENT_INFO, "Error sending on number " + mes.getRecipient() + " " + e.getMessage()));
                    }
                }
            }
        }).start();
        isSent = false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (webSocketClient != null) webSocketClient.close(1);
        connectLiveData.postValue(false);
        Log.d("VIEW", "cleared");
    }

    public void setServerAddress(String s){
        serverAddress = s;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(ADDRESS_SERVER, serverAddress);
        editor.apply();
    }

    private Boolean checkAddressServer(){
        if (preferences.getString(ADDRESS_SERVER, "").equals("")) {
            return false;
        }
        serverAddress = preferences.getString(ADDRESS_SERVER, "");
        return true;
    }

    public void disconnect(){
        webSocketClient.close();
        connectLiveData.postValue(false);
    }

    public void setTokenDevice(String tokenDevice) {
        this.device.setToken(tokenDevice);
        preferences.edit().putString(PREF_UUID_IDENTIFY_DEVICE, tokenDevice).apply();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initViewModel();
        }
    }

    public void sendAllReport(String key){
        new Thread(() -> {
            String keyReport = key.toLowerCase().split("_")[1];
            try {
                Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/" + keyReport), null, null, null, null);
                List<String> reportList = new ArrayList<>();
                if (cursor.moveToFirst()) {
                    Integer index = 0;
                    do {
                        StringBuilder msgData = new StringBuilder();
                        msgData.append("{");
                        for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                            msgData.append("\"").append(cursor.getColumnName(idx)).append("\":\"").append(cursor.getString(idx)).append("\",");
                        }
                        msgData.setCharAt(msgData.length() - 1, '}');
                        reportList.add(msgData.toString());
                        Log.d("Telephony", msgData.toString());
                        index++;
                        if (index == 2) break;
                    } while (cursor.moveToNext());
                    webSocketClient.send(getEvent(EVENT_REPORT, reportList.toString()));
                    logListLiveData.postValue("from: sms-server report sent...");
                } else {
                    webSocketClient.send(getEvent(EVENT_INFO, "report is empty"));
                    logListLiveData.postValue("Report is empty...");
                }
                cursor.close();
            } catch (Exception e) {
                webSocketClient.send(getEvent(EVENT_INFO, "report request error: " + e.getMessage()));
            }
        }).start();
    }

    public void savePingReportAfterDelivered(String deliverNumber, String sim, String status, String deliveryReport){
        savePingReport(deliverNumber, sim, status, deliveryReport, context);
    }

    public void savePingReport(String phoneNumber, String sim, String status, String deliveryReport, Context context) {
        Uri uri;
        uri = Uri.parse("content://sms/sent");
        boolean ret = false;
        try {
            ContentValues values = new ContentValues();
            values.put(SUBSCRIPTION_ID, sim);
            values.put(ADDRESS, phoneNumber);
            long date = System.currentTimeMillis();
            values.put(DATE, date);
            values.put(STATUS, status);
            values.put(SUBJECT, "");
            values.put(BODY, "ping");
            values.put(STATUS, deliveryReport);
            Long threadId = get_thread_id(uri, context);
            if (get_thread_id(uri, context) != -1L) {
                values.put(THREAD_ID, threadId);
            }
            context.getContentResolver().insert(uri, values);

            uri = Telephony.Sms.Sent.CONTENT_URI;
            context.getContentResolver().insert(uri, values);
            ret = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Long get_thread_id(Uri uri, Context context) {
        long threadId = 0;
        Cursor cursor = context.getContentResolver().query(uri, new String[] { "_id" },
                null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    threadId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
        Log.d("Telephony2", "threadId " + threadId);
        return threadId;
    }
}