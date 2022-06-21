package com.azimjon0910gmail;

import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.*;
import android.content.*;

import java.util.*;

import android.view.*;
import android.app.*;
import android.graphics.drawable.*;

import android.widget.AdapterView.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MyListView implements OnItemClickListener, OnItemLongClickListener {
    private Context context;
    private AppCompatActivity activity;
    private ArrayList<String> arrayMainText;
    private ArrayList<String> arraySubText;
    private ArrayList<Integer> arrayCheckedIndexes;

    private Boolean isListCheckable;

    private MyAdapter adapter;

    private SQLiteDatabase db;

    private Menu menu;



    // Конструкторы
    MyListView(AppCompatActivity context, ListView listView) {
        this.context = context;
        this.activity = context;
        // Поля

        arrayMainText = new ArrayList<>();
        arraySubText = new ArrayList<>();
        arrayCheckedIndexes = new ArrayList<>();
        isListCheckable = false;

        adapter = new MyAdapter();

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        db = new DBHelper(context).getWritableDatabase();
    }


    // Методы
    boolean isListCheckable() {
        return isListCheckable;
    }

    void removeCheckedItems() {
        int count = arrayCheckedIndexes.size();
        if (count > 0) {
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                names[i] = arrayMainText.get(arrayCheckedIndexes.get(i));
                int satId = DBHelper.getSatIdBySatName(db, names[i]);
                int[] ids = DBHelper.getTpIdBySatId(db, satId);
                if (ids != null) {
                    for (int id : ids) {
                        db.delete(DBHelper.getTableChannels(),
                                DBHelper.getChannelsFieldTPId()+"=?",
                                new String[]{String.valueOf(id)});
                    }
                }
                db.delete(DBHelper.getTableTP(), DBHelper.getTPFieldSatId() + "=?", new String[]{String.valueOf(satId)});
                db.delete(DBHelper.getTableSatellites(), DBHelper.getSatellitesFieldName() + "=?", new String[]{names[i]});
            }
            arrayMainText.removeAll(Arrays.asList(names));
            adapter.notifyDataSetChanged();
        }
    }

    boolean isItemChecked(int position) {
        return arrayCheckedIndexes.contains(position);
    }

    void update() {
        String name;
        String info;
        Cursor cr = db.query(DBHelper.getTableSatellites(), null, null, null, null, null, null);
        if (cr.moveToFirst()) {
            arrayMainText.clear();
            arraySubText.clear();
            do {
                name = cr.getString(cr.getColumnIndex(DBHelper.getSatellitesFieldName()));
                info = cr.getString(cr.getColumnIndex(DBHelper.getSatellitesFieldInfo()));
                arrayMainText.add(name);
                arraySubText.add(info);
            } while (cr.moveToNext());
            adapter.notifyDataSetChanged();
        }
        cr.close();
    }




    // Переопределенные методы
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isListCheckable)
            setItemCheckedChange(view, position);
        else {
            Intent intent = new Intent(context, TPActivity.class);
            String extraKey = context.getResources().getString(R.string.intent_extra_key_satName);
            intent.putExtra(extraKey, arrayMainText.get(position));
            context.startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (setListCheckable(!isListCheckable) == 1) {
            arrayCheckedIndexes.add(position);
        }

        return true;
    }




    // Геттеры
    int getCheckedItemCount() {
        if (isListCheckable)
            return arrayCheckedIndexes.size();

        return -1;
    }

    String getSingleCheckedMainText() {
        if (arrayCheckedIndexes.size() == 1) {
            return arrayMainText.get(arrayCheckedIndexes.get(0));
        }
        return null;
    }




    // Сеттеры
    private Boolean setItemChecked(View view, int position, boolean isChecked) {
        if (isListCheckable) {
            if (isChecked && !arrayCheckedIndexes.contains(position))
                arrayCheckedIndexes.add(position);
            else if (!isChecked && arrayCheckedIndexes.contains(position))
                arrayCheckedIndexes.remove(arrayCheckedIndexes.indexOf(position));

            ImageView item_ic_checked = view.findViewById(R.id.ic_check);
            item_ic_checked.setImageDrawable(context.getResources().getDrawable(isChecked ? R.drawable.ic_check_1 : R.drawable.ic_check_0));

            MenuItem edit_item = menu.findItem(R.id.action_edit);
            MenuItem delete_item = menu.findItem(R.id.action_delete);
            if (arrayCheckedIndexes.size() > 1) {
                edit_item.setVisible(false);
                delete_item.setVisible(true);
            } else if (arrayCheckedIndexes.size() == 1) {
                edit_item.setVisible(true);
                delete_item.setVisible(true);
            } else {
                edit_item.setVisible(false);
                delete_item.setVisible(false);
            }
            return isChecked;
        }
        return null;
    }

    private Boolean setItemCheckedChange(View view, int position) {
        if (isListCheckable) {
            setItemChecked(view, position, !arrayCheckedIndexes.contains(position));
            return true;
        }
        return null;
    }

    int setListCheckable(boolean isListCheckable) {
        if (this.isListCheckable != isListCheckable) {
            if (menu == null) {
                Toast.makeText(context, "Menu is null", Toast.LENGTH_SHORT).show();
                return -1;
            }

            this.isListCheckable = isListCheckable;

            FloatingActionButton fab = ((Activity) context).findViewById(R.id.fab);
            if (!isListCheckable) {
                arrayCheckedIndexes.clear();
                fab.show();
            } else fab.hide();

            menu.setGroupVisible(R.id.group_main, !isListCheckable);
            menu.setGroupVisible(R.id.group_list_checked, isListCheckable);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(isListCheckable);

            adapter.notifyDataSetChanged();
            return 1;
        }
        return 0;
    }

    void setMenu(Menu menu) {
        this.menu = menu;
    }




    // Адаптер
    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return arrayMainText.size();
        }

        @Override
        public Object getItem(int position) {
            return arrayMainText.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);

                view = inflater.inflate(R.layout.satellites_list_item, null);
            }

            TextView tvMainText = view.findViewById(R.id.tv_satellite_name);
            TextView tvSubText = view.findViewById(R.id.tv_satellite_info);

            tvMainText.setText(arrayMainText.get(position));
            tvSubText.setText(arraySubText.get(position));

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                tvMainText.setTextColor(context.getResources().getColor(android.R.color.white));
                tvSubText.setTextColor(context.getResources().getColor(R.color.colorLightGray));
            }

            ImageView img = view.findViewById(R.id.ic_check);
            Drawable drw;

            if (isListCheckable) {
                Drawable checkedOff = context.getResources().getDrawable(R.drawable.ic_check_0);
                Drawable checkedOn = context.getResources().getDrawable(R.drawable.ic_check_1);

                drw = isItemChecked(position) ? checkedOn : checkedOff;
            } else
                drw = context.getResources().getDrawable(R.color.colorTransparent);

            img.setImageDrawable(drw);

            return view;
        }

    }
}
