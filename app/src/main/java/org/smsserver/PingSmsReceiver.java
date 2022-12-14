package org.smsserver;

import static org.smsserver.config.Const.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.smsserver.R;

import org.smsserver.view.StoreActivity;

public class PingSmsReceiver extends BroadcastReceiver {
    public static final String TAG = "PingSmsReceiver";
    public final String CHANNEL_ID = "org.smsserver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("STORE", "PingSmsReceiver started");
        if (!Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Log.d("STORE", "Received intent, not DATA_SMS_RECEIVED_ACTION, but " + intent.getAction());
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREF_DATA_SMS_STORE, Context.MODE_PRIVATE);
        if (!context.getSharedPreferences("HttpActivity", Context.MODE_PRIVATE).getBoolean(PREF_RECEIVE_DATA_SMS, false)) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Object[] PDUs = (Object[]) bundle.get("pdus");
        Log.d("STORE", "Received " + (PDUs != null ? PDUs.length : 0) + " messages");

        int counter = 0;
        for (Object pdu : PDUs != null ? PDUs : new Object[0]) {
            StringBuilder sb = new StringBuilder();
            for (byte b : (byte[]) pdu) {
                sb.append(String.format("%02x", b));
            }
            Log.d("STORE", "HEX[" + (counter + 1) + "]: " + sb);
            String storeString = preferences.getString(PREF_DATA_SMS_STORE, "");
            storeString += sb + ",";
            preferences.edit().putString(PREF_DATA_SMS_STORE, storeString).apply();
            Notification(context, sb.toString());
        }
    }

    public void Notification(Context context, String message) {

        NotificationManager notificationmanager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationmanager == null) {
            return;
        }

        Intent intent = new Intent(context, StoreActivity.class);
        // Send data to NotificationView Class
        intent.putExtra("title", "Binary SMS Received");
        intent.putExtra("text", message);
        // Open NotificationView.java Activity
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create Notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ID)
                // Set Icon
                .setSmallIcon(R.mipmap.ic_launcher)
                // Set Ticker Message
                .setTicker(message)
                // Set Title
                .setContentTitle("Binary SMS Received")
                // Set Text
                .setContentText(message)
                // Add an Action Button below Notification
                .addAction(R.mipmap.ic_launcher, "Open SMS Ping", pIntent)
                // Set PendingIntent into Notification
                .setContentIntent(pIntent)
                // Dismiss Notification
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "com.itds.sms.ping", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            mChannel.setDescription("Data SMS Received Notifications");
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            notificationmanager.createNotificationChannel(mChannel);
        }
        // Build Notification with Notification Manager
        notificationmanager.notify(0, builder.build());

    }
}