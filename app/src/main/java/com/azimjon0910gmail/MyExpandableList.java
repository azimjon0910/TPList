package com.azimjon0910gmail;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MyExpandableList implements ExpandableListView.OnGroupClickListener, AdapterView.OnItemLongClickListener {
    private Context context;
    private ArrayList<String> arrayTP;
    private HashMap<String, List<String>> hashMapChannels;
    private MyAdapter adapter;
    private SQLiteDatabase db;
    private ArrayList<Integer> arrayCheckedIndexes;
    private int satId;

    private Boolean isListCheckable;

    private Menu menu;

    // Конструктор
    MyExpandableList(AppCompatActivity context, ExpandableListView elv, int satId) {
        this.context = context;
        this.satId = satId;

        arrayTP = new ArrayList<>();
        hashMapChannels = new HashMap<>();
        arrayCheckedIndexes = new ArrayList<>();
        isListCheckable = false;

        adapter = new MyAdapter();
        elv.setAdapter(adapter);
        elv.setOnGroupClickListener(this);
        elv.setOnItemLongClickListener(this);

        db = new DBHelper(context).getWritableDatabase();
    }



    // Методы
    void update() {
        String tp;
        List<String> channels;
        Cursor cr = db.query(DBHelper.getTableTP(), null,
                DBHelper.getTPFieldSatId() + "=?", new String[]{String.valueOf(satId)},
                null, null, null);
        if (cr.moveToFirst()) {
            arrayTP.clear();
            hashMapChannels.clear();
            do {
                tp = cr.getString(cr.getColumnIndex(DBHelper.getTPFieldTP()));
                int tpId = DBHelper.getTpId(db, satId, tp);
                String[] ch = DBHelper.getChannelsByTpId(db, tpId);
                if (ch == null) {
                    ch = new String[1];
                    ch[0] = context.getString(R.string.no_channels);
                }
                arrayTP.add(tp);
                if (ch.length == 1 && ch[0].equals(context.getResources().getString(R.string.no_channels)))
                    channels = new ArrayList<>();
                else
                    channels = Arrays.asList(ch);
                hashMapChannels.put(tp, channels);
            } while (cr.moveToNext());
            adapter.notifyDataSetChanged();
        }
        cr.close();
    }

    boolean isListCheckable() {
        return isListCheckable;
    }

    boolean isItemChecked(int position) {
        return arrayCheckedIndexes.contains(position);
    }

    void removeCheckedItems() {
        int count = arrayCheckedIndexes.size();
        if (count > 0) {
            String[] tps = new String[count];
            for (int i = 0; i < count; i++) {
                tps[i] = arrayTP.get(arrayCheckedIndexes.get(i));
                int tpId = DBHelper.getTpId(db, satId, tps[i]);
                db.delete(DBHelper.getTableChannels(),
                                DBHelper.getChannelsFieldTPId()+"=?",
                                new String[]{String.valueOf(tpId)});
                db.delete(DBHelper.getTableTP(), DBHelper.getFieldId() + "=?", new String[]{String.valueOf(tpId)});
            }
            arrayTP.removeAll(Arrays.asList(tps));
            adapter.notifyDataSetChanged();
        }
    }


    // Переопределенные методы
    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (isListCheckable) {
            setItemCheckedChange(v, groupPosition);
            return true;
        }
        return false;
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

    String getSingleCheckedTp() {
        if (arrayCheckedIndexes.size() == 1) {
            return arrayTP.get(arrayCheckedIndexes.get(0));
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

            setIconChecked(view, position);

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

            menu.setGroupVisible(R.id.group_list_checked, isListCheckable);

            adapter.notifyDataSetChanged();
            return 1;
        }
        return 0;
    }

    void setMenu(Menu menu) {
        this.menu = menu;
    }

    private void setIconChecked(View view, int position){
        ImageView img = view.findViewById(R.id.ic_check);
        Drawable drw = context.getResources().getDrawable(R.color.colorTransparent);
        if (isListCheckable) {
            Drawable checkedOff = context.getResources().getDrawable(R.drawable.ic_check_0);
            Drawable checkedOn = context.getResources().getDrawable(R.drawable.ic_check_1);

            drw = isItemChecked(position) ? checkedOn : checkedOff;
        }
        img.setImageDrawable(drw);
    }


    // Адаптер
    class MyAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return arrayTP.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<String> listChannels = hashMapChannels.get(arrayTP.get(groupPosition));
            if (listChannels != null) {
                return listChannels.size();
            }
            return -1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return arrayTP.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            List<String> listChannels = hashMapChannels.get(arrayTP.get(groupPosition));
            if (listChannels != null)
                return listChannels.get(childPosition);
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.tp_expandable_list_item, null);
            }
            TextView tvTP = view.findViewById(R.id.tv_tp);
            TextView tvChannelsCount = view.findViewById(R.id.tv_channels_count);

            String tp = (String) getGroup(groupPosition);
            int channelsCount = getChildrenCount(groupPosition);

            tvTP.setText(tp);
            tvChannelsCount.setText(String.valueOf(channelsCount));

            setIconChecked(view, groupPosition);

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.channels_list_item, null);
            }
            TextView tvTP = view.findViewById(R.id.tv_channel);
            String ch = (String) getChild(groupPosition, childPosition);
            tvTP.setText(ch);

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
