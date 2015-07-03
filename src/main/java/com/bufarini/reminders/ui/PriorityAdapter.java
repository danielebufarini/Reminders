package com.bufarini.reminders.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bufarini.R;
import com.bufarini.reminders.model.Priority;

import java.util.List;

public class PriorityAdapter extends ArrayAdapter<Priority> {
    private Context context;

    private static class ViewHolder {
        TextView value;
        TextView colour;
    }

    public PriorityAdapter(Context context, int resource, Priority[] objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LinearLayout rowView = new LinearLayout(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(R.layout.priority_item, rowView, true);
            rowView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            holder = new ViewHolder();
            holder.value = (TextView) rowView.findViewById(R.id.priority_value);
            holder.colour = (TextView) rowView.findViewById(R.id.priority_colour);
            convertView = rowView;
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();
        Priority priority = getItem(position == Priority.NONE.getPriority() ? 0 : position);
        String priorityValue = priority.getPriority() == 0 ? "" : String.valueOf(priority.getPriority());
        holder.value.setText(priorityValue);
        GradientDrawable shape = (GradientDrawable) context.getResources().getDrawable(R.drawable.coloured_box);
        shape.setColor(priority.getColour());
        holder.colour.setBackgroundDrawable(shape);
        if (priority.getPriority() == Priority.NONE.getPriority())
            holder.value.setText("None");
        return convertView;
    }
}
