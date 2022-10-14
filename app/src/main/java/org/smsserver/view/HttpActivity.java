package org.smsserver.view;

import static android.provider.Telephony.TextBasedSmsColumns.STATUS_PENDING;
import static org.smsserver.config.Const.DELIVERED_PING;
import static org.smsserver.config.Const.SENT_PING;
import static org.smsserver.config.Const.SENT_SMS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.smsserver.R;
import org.smsserver.model.Message;
import org.smsserver.service.ForegroundService;
import org.smsserver.viewModel.HttpViewModel;
import org.smsserver.viewModel.HttpViewModelFactory;

import org.mobicents.protocols.ss7.map.api.smstpdu.SmsStatusReportTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.mobicents.protocols.ss7.map.api.smstpdu.Status;
import org.mobicents.protocols.ss7.map.smstpdu.SmsTpduImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class HttpActivity extends AppCompatActivity {

    byte[] lastSendResultPDU = new byte[0];

    final String TAG = "PingSMS";

    final IntentFilter sentFilter = new IntentFilter(SENT_PING);
    final IntentFilter deliveryFilter = new IntentFilter(DELIVERED_PING);
    final IntentFilter wapDeliveryFilter = new IntentFilter("android.provider.Telephony.WAP_PUSH_RECEIVED");

    private RecyclerViewAdapter adapter;
    private ArrayList<String> logList = new ArrayList<>();
    private HttpViewModel viewModel;

    ImageButton btnExit, btnQrCode, btnConnect, btnSettings;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return;
        if (requestCode == 12) {
            viewModel.setServerAddress(data.getStringExtra("address"));
            btnConnect.setVisibility(View.VISIBLE);
        }
        if (requestCode == 21) {
            viewModel.setTokenDevice(data.getStringExtra("id_device"));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http);

        btnExit = findViewById(R.id.iBtnExit);
        btnConnect = findViewById(R.id.iBtnConnect);
        btnQrCode = findViewById(R.id.iBtnQr);
        btnSettings = findViewById(R.id.iBtnSettings);

        RecyclerView recyclerView = findViewById(R.id.rvLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter(logList);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this, new HttpViewModelFactory(
                this,
                getPreferences(Context.MODE_PRIVATE))).get(HttpViewModel.class);

        final Observer<String> observerLog = this::updateLogRecycler;
        viewModel.getLogListLiveData().observe(this, observerLog);

        final Observer<ArrayList<String>> observerPermission = this::addPermission;
        viewModel.getPermissionsLiveData().observe(this, observerPermission);

        final Observer<Boolean> observerConnect = this::updateConnect;
        viewModel.getConnectLiveData().observe(this, observerConnect);

        final Observer<Boolean> observerIdDevice = this::getIdDeviceFromQr;
        viewModel.getIdDeviceLiveData().observe(this, observerIdDevice);

        final Observer<Boolean> observerServerAddress = this::getServerAddressLiveData;
        viewModel.getServerAddressLiveData().observe(this, observerServerAddress);

        if (viewModel.serverAddress == null) {
            Toast.makeText(HttpActivity.this,
                    R.string.toast_input_server_address,
                    Toast.LENGTH_LONG).show();
            btnConnect.setVisibility(View.INVISIBLE);
        }

        btnConnect.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.connectLiveData.getValue())) viewModel.disconnect();
            else {
                try {
                    viewModel.connectToServerWebsocket();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        btnExit.setOnClickListener(v -> {
            stopService();
            finish();
        });

        btnSettings.setOnClickListener(v -> {
            startActivityForResult(new Intent(HttpActivity.this,
                    SettingsActivity.class), 12);
        });

        btnQrCode.setOnClickListener(v -> {
            startActivityForResult(new Intent(HttpActivity.this,
                    ScannedBarcodeActivity.class), 21);
        });

        IntentFilter intentFilter = new IntentFilter(SENT_PING);
        intentFilter.addAction(DELIVERED_PING);
    }

    private void getServerAddressLiveData(Boolean address) {
        if (!address) {
            Toast.makeText(HttpActivity.this, R.string.toast_input_server_address,
                    Toast.LENGTH_LONG).show();
            btnConnect.setVisibility(View.INVISIBLE);
        } else {
            btnConnect.setVisibility(View.VISIBLE);
        }
    }

    private void getIdDeviceFromQr(Boolean token) {
        if (!token) {
            Toast.makeText(HttpActivity.this,
                    "To start the application, you need to get the device token.\n" +
                            "Scan the QR code.",
                    Toast.LENGTH_LONG).show();
            btnConnect.setVisibility(View.INVISIBLE);
        }
    }

    private void updateConnect(Boolean aBoolean) {
        Log.d("Websocket", "Status connect " + aBoolean);
        if (aBoolean) btnConnect.setImageResource(R.drawable.cloud_done);
        else btnConnect.setImageResource(R.drawable.cloud_off);
    }

    private void addPermission(ArrayList<String> missingPermissions) {
        ActivityCompat.requestPermissions(this,
                missingPermissions.toArray(new String[0]), 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateLogRecycler(String s) {
        logList.add("> " + s);
        startService(s);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ACTIVITY", "onResult");
        registerReceiver(br, sentFilter);
        registerReceiver(br, deliveryFilter);
        registerReceiver(br, wapDeliveryFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ACTIVITY", "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
        Log.d("ACTIVITY", "onDestroy");
    }

    public void startService(String notify) {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", notify);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        Log.d("SERVICE", "stop");
        stopService(serviceIntent);
    }

    @Override
    public void onBackPressed() {

    }

    BroadcastReceiver br = new BroadcastReceiver() {
        String result = null;

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String sent = "";
            String deliveryReport = "";


            if ((SENT_PING).equalsIgnoreCase(intent.getAction())) {
                String sendNumber = intent.getStringExtra("s_number");
                Log.d("SENT CODE", String.valueOf(getResultCode()));
                sent = String.valueOf(getResultCode());
                viewModel.logListLiveData.postValue((getResultCode() == RESULT_OK
                        ? sendNumber + " " + getResources().getString(R.string.sent)
                        : sendNumber + " " + getResources().getString(R.string.notsent)).toLowerCase(Locale.ROOT) + "(Code " + getResultCode() + ")");
            } else if ((DELIVERED_PING).equalsIgnoreCase(intent.getAction())) {
                SmsMessage sms = null;
                String deliverNumber = intent.getStringExtra("d_number");
                SmsTpdu parsedResultPdu = null;
                boolean delivered = false;
                if (intent.hasExtra("pdu")) {
                    byte[] pdu = intent.getByteArrayExtra("pdu");
                    String format = intent.getStringExtra("format");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && format != null) {
                        sms = SmsMessage.createFromPdu(pdu, format);
                    } else {
                        sms = SmsMessage.createFromPdu(pdu);
                    }

                    deliveryReport = String.valueOf(sms.getStatus());

                    result = "Delivery result : " + translateDeliveryStatus(sms.getStatus());
                    viewModel.logListLiveData.postValue(deliverNumber + " delivery status : " + translateDeliveryStatus(sms.getStatus()));
                    if (pdu != null && pdu.length > 1) {
                        lastSendResultPDU = pdu;
                        parsedResultPdu = getSmsTpdu(pdu);
                        if (parsedResultPdu != null) {
                            delivered = parsedResultPdu.getSmsTpduType()
                                    .equals(SmsTpduType.SMS_STATUS_REPORT)
                                    && ((SmsStatusReportTpdu) parsedResultPdu)
                                    .getStatus().getCode() == Status.SMS_RECEIVED;
                        } else {
                            String resultPdu = getLogBytesHex(pdu).trim();
                            delivered = "00".equalsIgnoreCase(resultPdu.substring(resultPdu.length() - 2));
                            viewModel.logListLiveData.postValue(((SmsStatusReportTpdu) parsedResultPdu).getRecipientAddress().getAddressValue() +
                                    " " +
                                    getResources().getString(R.string.offline).toLowerCase() + " Code(" + parsedResultPdu.getSmsTpduType().getCode() + ")");

                        }
                    }
                    viewModel.savePingReportAfterDelivered(deliverNumber, "0", String.valueOf(sms.getStatus()), deliveryReport);
                    viewModel.logListLiveData.postValue(delivered
                            ? ((SmsStatusReportTpdu) parsedResultPdu).getRecipientAddress().getAddressValue() +
                            " " +
                            getResources().getString(R.string.delivered).toLowerCase() + " Code(" + parsedResultPdu.getSmsTpduType().getCode() + ")"
                            : ((SmsStatusReportTpdu) parsedResultPdu).getRecipientAddress().getAddressValue() +
                            " " +
                            getResources().getString(R.string.offline).toLowerCase() + " Code(" + parsedResultPdu.getSmsTpduType().getCode() + ")");

                }
            }
        }

        String translateDeliveryStatus(int status) {
            switch (status) {
                case Telephony.Sms.STATUS_COMPLETE:
                    return "Delivery.STATUS_COMPLETE".toLowerCase();
                case Telephony.Sms.STATUS_FAILED:
                    return "Delivery.STATUS_FAILED".toLowerCase();
                case STATUS_PENDING:
                    return "Delivery.STATUS_PENDING".toLowerCase();
                case Telephony.Sms.STATUS_NONE:
                    return "Delivery.STATUS_NONE".toLowerCase();
                default:
                    return "Delivery.Unknown status code".toLowerCase();
            }
        }
    };

    @Nullable
    public SmsTpdu getSmsTpdu(byte[] pduBytes) {
        SmsTpdu result = null;
        try {
            result = SmsTpduImpl.createInstance(pduBytes, false, null);
        } catch (Exception e) {
            Log.d(TAG, "getSmsTpdu:1", e);
        }
        if (result == null) {
            try {
                byte[] pduWithoutSCA = Arrays.copyOfRange(pduBytes, pduBytes[0] + 1, pduBytes.length);
                result = SmsTpduImpl.createInstance(pduWithoutSCA, false, null);
            } catch (Exception e) {
                Log.d(TAG, "getSmsTpdu:2", e);
            }
        }
        return result;
    }

    private String getLogBytesHex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("0x%02X ", b));
        }
        return sb.toString();
    }
}