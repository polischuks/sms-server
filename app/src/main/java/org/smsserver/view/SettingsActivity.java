package org.smsserver.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.smsserver.R;

public class SettingsActivity extends AppCompatActivity {

    EditText inputAddress;
    Button btnOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        inputAddress = findViewById(R.id.etServerAdress);
        btnOk = findViewById(R.id.btnOkServerAddress);

        btnOk.setOnClickListener(v -> {
            if (inputAddress.getText().toString().isEmpty()){
                Toast toast = Toast.makeText(SettingsActivity.this, R.string.empty_address,Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            Intent intentRes = new Intent();
            intentRes.putExtra("address", inputAddress.getText().toString());
            setResult(RESULT_OK, intentRes);
            finish();
        });
    }
}