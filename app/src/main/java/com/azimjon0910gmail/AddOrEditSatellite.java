package com.azimjon0910gmail;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddOrEditSatellite extends AppCompatActivity implements View.OnClickListener {
    Resources res;

    // For add
    String satName, satInfo;
    // Для обновления (редактирования) спутника в БД
    String satOldName, satOldInfo;

    EditText etSatName, etSatInfo;

    SQLiteDatabase db;

    boolean addSat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_satellite);

        TextView tvTitle = findViewById(R.id.tvTitle);
        etSatName = findViewById(R.id.etSatName);
        etSatInfo = findViewById(R.id.etSatSubtext);

        res = getResources();
        db = new DBHelper(this).getWritableDatabase();

        String titleFromIntent = getIntent().getStringExtra(res.getString(R.string.intent_extra_key_title));
        String titleEditSat = res.getString(R.string.edit);

        tvTitle.setText(titleFromIntent);

        addSat = true;

        if (titleFromIntent.equals(titleEditSat)) {
            satOldName = satOldInfo = null;
            satOldName = getIntent().getStringExtra(res.getString(R.string.intent_extra_key_satOldName));
            Cursor cursor = db.query(DBHelper.getTableSatellites(),
                    null,
                    DBHelper.getSatellitesFieldName() + "=?",
                    new String[]{satOldName}, null, null, null);
            if (cursor.moveToFirst()) {
                satOldInfo = cursor.getString(cursor.getColumnIndex(DBHelper.getSatellitesFieldInfo()));
            }
            cursor.close();
            if (satOldName == null || satOldInfo == null) {
                Toast.makeText(this, "AddOrEditSatellite: onCreate: editSat: satOldName or satOldInfo is null", Toast.LENGTH_LONG).show();
                finish();
            }

            tvTitle.append(" ");
            tvTitle.append(Html.fromHtml("<i>" + satOldName + "</i>"));
            etSatName.setText(satOldName);
            etSatInfo.setText(satOldInfo);
            addSat = false;
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSave);
        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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
            default:
                break;
        }
    }

    public boolean save() {
        boolean success;

        Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        satName = etSatName.getText().toString();
        satInfo = etSatInfo.getText().toString();
        if (satName.isEmpty()) {
            toast.setText(R.string.enter_satName);
            toast.show();
            return false;
        }

        if (addSat) {
            boolean exists = DBHelper.existsSat(db, satName) > 0;
            if (!exists) {
                success = DBHelper.insertSat(db, satName, satInfo) > 0;
                if (!success) toast.setText(res.getString(R.string.error_insertDB));
            } else {
                toast.setText(res.getString(R.string.sat_exists));
                success = false;
            }
        } else {
            int updt = 0;
            if (!satName.equals(satOldName) || !satInfo.equals(satOldInfo)) {
                updt = DBHelper.updateSatByName(db, satOldName, satName, satInfo);
                success = updt > 0;
            } else success = true;

            if (!success) {
                if (updt == -2)
                    toast.setText(res.getString(R.string.sat_exists));
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


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
