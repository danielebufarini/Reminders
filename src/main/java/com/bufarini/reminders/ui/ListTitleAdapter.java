package com.bufarini.reminders.ui;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bufarini.R;

public class ListTitleAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final int resource;

    private static class ViewHolder {
        TextView listTitle;
    }

    public ListTitleAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LinearLayout rowView = new LinearLayout(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(resource, rowView, true);
            rowView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            holder = new ViewHolder();
            holder.listTitle = (TextView) rowView.findViewById(R.id.listTitle);
            convertView = rowView;
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();
        final String title = getItem(position);
        holder.listTitle.setText(title);
        return convertView;
    }
}
