package com.azimjon0910gmail;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddOrEditTp extends AppCompatActivity implements View.OnClickListener {
    Resources res;

    // For add
    String tp;
    String[] channels;
    // Для обновления (редактирования) ТР в БД
    String oldTP, oldChannels;
    EditText etFreq1, etFreq2, etChannels;
    TextView tvPolarization;
    int satId;

    SQLiteDatabase db;

    boolean addTP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tp);

        TextView tvTitle = findViewById(R.id.tvTitle);
        etFreq1 = findViewById(R.id.etFreq1);
        tvPolarization = findViewById(R.id.tvPolarization);
        etFreq2 = findViewById(R.id.etFreq2);
        etChannels = findViewById(R.id.etChannels);

        res = getResources();
        db = new DBHelper(this).getWritableDatabase();

        satId = getIntent().getIntExtra(res.getString(R.string.intent_extra_key_satId), -1);

        String titleFromIntent = getIntent().getStringExtra(res.getString(R.string.intent_extra_key_title));
        String titleEditTP = res.getString(R.string.edit);

        tvTitle.setText(titleFromIntent);

        addTP = true;

        if (titleFromIntent.equals(titleEditTP)) {
            oldTP = oldChannels = null;
            oldTP = getIntent().getStringExtra(res.getString(R.string.intent_extra_key_oldTP));
            String oldTpId = String.valueOf(DBHelper.getTpId(db, satId, oldTP));

            Cursor cursor = db.query(DBHelper.getTableChannels(),
                    null,
                    DBHelper.getChannelsFieldTPId() + "=?",
                    new String[]{oldTpId}, null, null, null);
            if (cursor.moveToFirst()) {
                oldChannels = "";
                int count = cursor.getCount();
                int i = 0;
                do {
                    oldChannels += cursor.getString(cursor.getColumnIndex(DBHelper.getChannelsFieldName()));
                    if (i < count - 1)
                        oldChannels += "\n";
                    i++;
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (oldTP == null) {
                Toast.makeText(this, "AddOrEditTP: onCreate: editTP: oldTP is null", Toast.LENGTH_LONG).show();
                finish();
            }
            if (res.getConfiguration().locale.getCountry().equals("RU"))
                tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvTitle.getTextSize() - 5);
            tvTitle.append(" ");
            tvTitle.append(Html.fromHtml("<i>" + oldTP + "</i>"));
            setTpText(etFreq1, etFreq2, tvPolarization, oldTP.toCharArray());
            if (!oldChannels.equals(getResources().getString(R.string.no_channels)))
                etChannels.setText(oldChannels);
            addTP = false;
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSave);
        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        tvPolarization.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvPolarization:
                if (tvPolarization.getText().toString().equals(getString(R.string.polarization_H)))
                    tvPolarization.setText(getString(R.string.polarization_V));
                else
                    tvPolarization.setText(getString(R.string.polarization_H));
                break;

            case R.id.btnSave:
                if (save()) {
                    setResult(RESULT_OK);
                    finish();
                }
                break;


            case R.id.btnCancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }


    public boolean save() {
        boolean success;

        Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        String f1 = etFreq1.getText().toString();
        String f2 = etFreq2.getText().toString();
        if (f1.isEmpty() || f2.isEmpty()) {
            toast.setText(R.string.enter_tp);
            toast.show();
            return false;
        }
        String ch = etChannels.getText().toString();
        if (ch.isEmpty())
            ch = getResources().getString(R.string.no_channels);
        tp = f1 + tvPolarization.getText().toString() + f2;

        channels = ch.split("\n");

        if (addTP) {
            boolean exists = DBHelper.existsTP(db, satId, tp) > 0;
            if (!exists) {
                int tpInsertedCount = DBHelper.insertTP(db, tp, satId);
                int tpId = DBHelper.getTpId(db, satId, tp);
                int channelsInsertedCount = DBHelper.insertChannels(db, channels, tpId);
                success = (tpInsertedCount > 0) && (channelsInsertedCount > 0);
                if (!success) toast.setText(res.getString(R.string.error_insertDB));
            } else {
                toast.setText(res.getString(R.string.tp_exists));
                success = false;
            }
        } else {
            String[] old = null;
            if (oldChannels != null)
                old = oldChannels.split("\n");


            int updt = DBHelper.updateTpByTP(db, satId, oldTP, tp);
            boolean success1 = updt > 0;
            boolean success2;
            int tpId = DBHelper.getTpId(db, satId, tp);
            if (old != null)
                success2 = DBHelper.updateChannelsByChannels(db, old, channels, tpId) > 0;
            else success2 = DBHelper.insertChannels(db, channels, tpId) > 0;
            success = success1 & success2;

            if (!success) {
                if (updt == -2)
                    toast.setText(res.getString(R.string.tp_exists));
                else
                    toast.setText(res.getString(R.string.error_updateDB));
            }
        }
        if (!success) {
            toast.show();
            return false;
        }
        return true;
    }

    public void setTpText(EditText freq1, EditText freq2, TextView polarization, char[] tp) {
        freq1.setText("");
        freq2.setText("");
        polarization.setText(getString(R.string.polarization_H));
        String f1 = "";
        String f2 = "";
        String pol;

        int i = 0;
        while (tp[i] != ' ')
            f1 += tp[i++];
        i++;

        if (tp[i] == 'H')
            pol = getString(R.string.polarization_H);
        else if (tp[i] == 'V')
            pol = getString(R.string.polarization_V);
        else
            return;

        i += 2;

        while (i < tp.length)
            f2 += tp[i++];

        freq1.setText(f1);
        freq2.setText(f2);
        polarization.setText(pol);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
