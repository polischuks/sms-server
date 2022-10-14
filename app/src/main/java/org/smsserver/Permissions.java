package org.smsserver;

import static org.smsserver.config.Const.PREF_RECEIVE_DATA_SMS;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class Permissions {

    private Context context;
    private SharedPreferences preferences;
    private ArrayList<String> missingPermissions = new ArrayList<>();

    public Permissions(Context context, SharedPreferences pref){

        this.context = context;
        this.preferences = pref;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean checkPermissions() {

        int sendSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
        int readPhonePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
        int receiveSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS);
        int readPhoneNumberPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS);
        int readSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS);

        if (readSmsPermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_SMS);
        }

        if (readPhoneNumberPermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (sendSmsPermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.SEND_SMS);
        }

        if (readPhonePermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (receiveSmsPermission != PackageManager.PERMISSION_GRANTED && preferences.getBoolean(PREF_RECEIVE_DATA_SMS, false)) {
            missingPermissions.add(Manifest.permission.RECEIVE_SMS);
        }

        if (!missingPermissions.isEmpty()) {
            return false;
        }
        return true;
    }

    public ArrayList<String> getMissingPermissions() {
        return missingPermissions;
    }
}
