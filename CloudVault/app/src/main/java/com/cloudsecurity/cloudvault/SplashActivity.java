package com.cloudsecurity.cloudvault;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Shivanshu on 28-02-2016.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, CloudVault.class);
        startActivity(intent);
        finish();
    }
}