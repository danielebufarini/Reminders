/*
Copyright 2015 Daniele Bufarini

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.bufarini.reminders.ui;

import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.DateUtils;
import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.Header;
import com.bufarini.reminders.model.Priority;
import com.squareup.otto.Bus;

public class GTaskAdapter extends ArrayAdapter<GTask> {
	private static Calendar calendar = Calendar.getInstance();
	private static final Bus bus = BusProvider.getInstance();
	private Context context;
	
	private static class ViewHolder {
		TextView title;
		CheckBox completed;
		TextView dueDate;
		ImageView reminder;
		ImageView recurring;
        TextView priority;
        DragGripView handle;
        DragGripView noHandle;
	}

	public GTaskAdapter(Context context, int resource, List<GTask> items) {
		super(context, resource, items);
		this.context = context;
		bus.register(this);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        final Object item = getItem(position);
        if (item instanceof Header) {
            LinearLayout rowView = new LinearLayout(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(R.layout.list_item_header, rowView, true);
            TextView header = (TextView) rowView.findViewById(R.id.list_item_header);
            header.setText(((Header) item).title);
            header.setTypeface(null, Typeface.BOLD_ITALIC);
            header.setPadding(0, 0, 0, 0);
            return rowView;
        }
		ViewHolder taskViewHolder;
        //if (convertView == null) {
			LinearLayout rowView = new LinearLayout(context);
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			li.inflate(R.layout.item, rowView, true);
			rowView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			taskViewHolder = new ViewHolder();
			taskViewHolder.title = (TextView) rowView.findViewById(R.id.item);
			taskViewHolder.completed = (CheckBox) rowView.findViewById(R.id.item_check);
			taskViewHolder.dueDate = (TextView) rowView.findViewById(R.id.dueDate);
			taskViewHolder.reminder = (ImageView) rowView.findViewById(R.id.reminder);
			taskViewHolder.recurring = (ImageView) rowView.findViewById(R.id.recurring);
            taskViewHolder.priority = (TextView) rowView.findViewById(R.id.item_priority);
            taskViewHolder.handle = (DragGripView) rowView.findViewById(R.id.drag_handle);
            taskViewHolder.noHandle = (DragGripView) rowView.findViewById(R.id.no_handle);
			convertView = rowView;
			convertView.setTag(taskViewHolder);
//		} else
//			taskViewHolder = (ViewHolder) convertView.getTag();
        final GTask task = getItem(position);
		if (task.reminderDate < System.currentTimeMillis() && task.reminderInterval == 0)
			task.reminderDate = 0;
		boolean isChecked = task.completed > 0;
		taskViewHolder.title.setText(task.title);
        Linkify.addLinks(taskViewHolder.title, Linkify.ALL);
		if (isChecked)
			taskViewHolder.title.setPaintFlags(taskViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		else
			taskViewHolder.title.setPaintFlags(taskViewHolder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
		taskViewHolder.completed.setOnCheckedChangeListener(null);
		taskViewHolder.completed.setChecked(isChecked);
		taskViewHolder.completed.setOnCheckedChangeListener(
			new OnCheckedChangeListener() {	
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked)
						NotificationUtils.cancelReminder(context, task);
					else {
						if (task.reminderDate > 0)
							NotificationUtils.setReminder(context, task);
					}
					task.completed = isChecked ? System.currentTimeMillis() : 0;
					task.isModified = true;
					task.updated = System.currentTimeMillis();
					bus.post(new Object[] { TasksFragment.REFRESH_TASKS_LIST, context });
				}
			}
		);
		if (task.dueDate > 0) {
			calendar.setTimeInMillis(task.dueDate);
			taskViewHolder.dueDate.setTextColor(DateUtils.isInThePast2(calendar) ?
				context.getResources().getColor(android.R.color.holo_red_dark) :
					context.getResources().getColor(android.R.color.black));
			taskViewHolder.dueDate.setText(DateUtils.formatDate(calendar));
		} else
			taskViewHolder.dueDate.setText("");
		taskViewHolder.reminder.setVisibility(task.reminderDate > 0 ? View.VISIBLE : View.INVISIBLE);
		taskViewHolder.recurring.setVisibility(task.reminderInterval > 0 ? View.VISIBLE : View.INVISIBLE);
        if (task.priority > 0 && task.priority < Priority.PRIORITIES.length) {
            GradientDrawable shape = (GradientDrawable) context.getResources().getDrawable(R.drawable.coloured_box);
            shape.setColor(Priority.getColourForValue(task.priority));
            taskViewHolder.priority.setBackgroundDrawable(shape);
            taskViewHolder.priority.setVisibility(View.VISIBLE);
            taskViewHolder.priority.setText(String.valueOf(task.priority));
            taskViewHolder.priority.setTextColor(Color.WHITE);
            taskViewHolder.priority.setTextSize(14.0F);
            taskViewHolder.priority.setTypeface(null, Typeface.BOLD_ITALIC);
            taskViewHolder.priority.setPadding(8, 40, 8, 8);
        } else
            taskViewHolder.priority.setVisibility(View.GONE);
        taskViewHolder.handle.setVisibility(task.list.isSortedByDueDate ? View.GONE : View.VISIBLE);
        taskViewHolder.noHandle.setVisibility(task.list.isSortedByDueDate ? View.VISIBLE : View.GONE);
		return convertView;
	}
}
