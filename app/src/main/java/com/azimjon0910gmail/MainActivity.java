package com.azimjon0910gmail;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        DialogInterface.OnClickListener {

    // Для работы с базой данных
    SQLiteDatabase db;

    // Для satellite_listView
    MyListView myElv;

    // Для работы с ресурсами. Например, для получения drawable, string, integer, ...
    Resources res;

    // Для динамического изменения свойств путнктов меню
    Menu menu;

    // Для формирование диалогового окна подтверждения. Например, подтверждение удаления элемента списки.
    DialogInterface.OnClickListener dialogClickListener; // обработка нажатия
    AlertDialog.Builder builder; // builder

    // Состояние вспышки камеры (фонарика): on/off
    int flashLightState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        res = getResources();

        boolean isCameraFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (isCameraFlash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashLightState = res.getInteger(R.integer.flashLightOff);
            if (savedInstanceState != null) {
                if (savedInstanceState.getInt("flashLightState", res.getInteger(R.integer.flashLightOff)) == res.getInteger(R.integer.flashLightOn))
                    flashLightState = res.getInteger(R.integer.flashLightOn);
            }
        } else flashLightState = -1;

        dialogClickListener = this;
        builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener);

        db = new DBHelper(this).getWritableDatabase();

        myElv = new MyListView(this, (ListView) findViewById(R.id.satellites_list_view));
        myElv.update();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        myElv.setMenu(menu);
        if (flashLightState == -1) {
            menu.findItem(R.id.flashLight).setEnabled(false);
            menu.removeItem(R.id.flashLight);
        } else if (flashLightState == res.getInteger(R.integer.flashLightOn))
            menu.findItem(R.id.flashLight).setIcon(R.drawable.ic_flashlight_white_24dp);
        menu.setGroupVisible(R.id.group_main, true);
        menu.setGroupVisible(R.id.group_list_checked, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.flashLight:
                int flashOff = res.getInteger(R.integer.flashLightOff);
                int flashOn = res.getInteger(R.integer.flashLightOn);
                if (flashLightState == flashOff) {
                    setFlashlightOnOff(true);
                    item.setIcon(R.drawable.ic_flashlight_white_24dp);
                    flashLightState = flashOn;
                } else if (flashLightState == flashOn) {
                    setFlashlightOnOff(false);
                    item.setIcon(R.drawable.ic_flashlight_black_24dp);
                    flashLightState = flashOff;
                }
                break;

            case R.id.action_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;


            case R.id.action_delete:
                String satName;
                if (myElv.getCheckedItemCount() > 1)
                    satName = res.getString(R.string.satellites);
                else
                    satName = myElv.getSingleCheckedMainText();

                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.delete) + " " + satName)
                        .setMessage(res.getString(R.string.delete_ask))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                myElv.removeCheckedItems();
                                myElv.setListCheckable(false);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .setIcon(R.drawable.ic_dialog_alert)
                        .show();
                break;


            case R.id.action_edit:
                intent = new Intent(MainActivity.this, AddOrEditSatellite.class);

                String titleKey = res.getString(R.string.intent_extra_key_title);
                String titleValue = res.getString(R.string.edit);
                String oldNameKey = res.getString(R.string.intent_extra_key_satOldName);
                String oldNameValue = myElv.getSingleCheckedMainText();

                intent.putExtra(titleKey, titleValue);
                intent.putExtra(oldNameKey, oldNameValue);
                startActivityForResult(intent, res.getInteger(R.integer.REQUEST_EDIT_SATELLITE));

                myElv.setListCheckable(false);
                break;


            case android.R.id.home:
                myElv.setListCheckable(false);
                break;


            case R.id.exit:
                finish();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            Intent intent = new Intent(MainActivity.this, AddOrEditSatellite.class);

            String key = res.getString(R.string.intent_extra_key_title);
            String title = res.getString(R.string.add_satellite);

            intent.putExtra(key, title);
            startActivityForResult(intent, res.getInteger(R.integer.REQUEST_ADD_SATELLITE));
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                //No button clicked
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == res.getInteger(R.integer.REQUEST_ADD_SATELLITE) ||
                    requestCode == res.getInteger(R.integer.REQUEST_EDIT_SATELLITE)) {
                myElv.update();
            }
        }
    }

    private void setFlashlightOnOff(boolean flashOn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cameraManager = (CameraManager) getSystemService(Service.CAMERA_SERVICE);
            try {
                if (cameraManager != null) {
                    String cameraId = cameraManager.getCameraIdList()[0];

                    cameraManager.setTorchMode(cameraId, flashOn);
                }
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && myElv.isListCheckable()) {
            myElv.setListCheckable(false);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("flashLightState", flashLightState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        if (isFinishing() && flashLightState == res.getInteger(R.integer.flashLightOn))
            setFlashlightOnOff(false);
    }
}