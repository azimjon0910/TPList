package com.azimjon0910gmail;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TPActivity extends AppCompatActivity implements View.OnClickListener {

    Intent intent;
    int satId;

    MyExpandableList myElv;

    Resources res;
    Menu menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tp);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        res = getResources();

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        intent = getIntent();

        String satName = intent.getStringExtra(res.getString(R.string.intent_extra_key_satName));
        satId = DBHelper.getSatIdBySatName(new DBHelper(this).getReadableDatabase(), satName);

        setTitle(satName);

        ExpandableListView elv = findViewById(R.id.tp_expandable_list_view);
        myElv = new MyExpandableList(this, elv, satId);
        myElv.update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        myElv.setMenu(menu);
        menu.setGroupVisible(R.id.group_main, false);
        menu.setGroupVisible(R.id.group_list_checked, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                String checkedTp = "";
                if (myElv.getCheckedItemCount() > 1)
                    checkedTp = getString(R.string.transponders);
                else checkedTp = myElv.getSingleCheckedTp();

                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.delete) + " " + checkedTp)
                        .setMessage(res.getString(R.string.delete_ask))

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                myElv.removeCheckedItems();
                                myElv.setListCheckable(false);
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(R.string.cancel, null)
                        .setIcon(R.drawable.ic_dialog_alert)
                        .show();
                break;


            case R.id.action_edit:
                Intent intent = new Intent(this, AddOrEditTp.class);

                String titleKey = res.getString(R.string.intent_extra_key_title);
                String titleValue = res.getString(R.string.edit);
                String oldTpKey = res.getString(R.string.intent_extra_key_oldTP);
                String oldTpValue = myElv.getSingleCheckedTp();
                String satIdKey = res.getString(R.string.intent_extra_key_satId);

                intent.putExtra(titleKey, titleValue);
                intent.putExtra(oldTpKey, oldTpValue);
                intent.putExtra(satIdKey, satId);
                startActivityForResult(intent, res.getInteger(R.integer.REQUEST_EDIT_TP));

                myElv.setListCheckable(false);
                break;


            case android.R.id.home:
                if (myElv.isListCheckable())
                    myElv.setListCheckable(false);
                else
                    onBackPressed();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            Intent intent = new Intent(this, AddOrEditTp.class);

            String titleKey = res.getString(R.string.intent_extra_key_title);
            String titleValue = res.getString(R.string.add_tp);
            String satIdKey = res.getString(R.string.intent_extra_key_satId);

            intent.putExtra(titleKey, titleValue);
            intent.putExtra(satIdKey, satId);

            startActivityForResult(intent, res.getInteger(R.integer.REQUEST_ADD_TP));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == res.getInteger(R.integer.REQUEST_ADD_TP) ||
                    requestCode == res.getInteger(R.integer.REQUEST_EDIT_TP)) {
                myElv.update();
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
}
