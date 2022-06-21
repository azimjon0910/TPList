package com.azimjon0910gmail;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tvVersion = findViewById(R.id.tv_app_version);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText(getString(R.string.version) + ": " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        TextView tvDeveloperMail = findViewById(R.id.tv_developer_mail);
        tvDeveloperMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:" + getString(R.string.developer_mail));
                intent.setData(data);
                startActivity(intent);
            }
        });
    }
}
