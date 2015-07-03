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

package com.bufarini.reminders.collaboration;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.bufarini.R;
import com.bufarini.reminders.model.GTask;

import java.util.List;

public class EmailAdapter extends ArrayAdapter<MailEntry> {
	private final Context context;
	
	private static class ViewHolder {
		TextView subject;
		CheckBox toBeImported;
		TextView emailFrom;
		TextView date;
	}
	
	public EmailAdapter(Context context, int resource, List<MailEntry> items) {
		super(context, resource, items);
		this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			LinearLayout rowView = new LinearLayout(context);
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			li.inflate(R.layout.email_single_item, rowView, true);
			holder = new ViewHolder();
			holder.subject = (TextView) rowView.findViewById(R.id.subject);
			holder.toBeImported = (CheckBox) rowView.findViewById(R.id.email_to_import);
			holder.emailFrom = (TextView) rowView.findViewById(R.id.email_from);
			holder.date = (TextView) rowView.findViewById(R.id.sent_date);
			convertView = rowView;
			convertView.setTag(holder);
		} else
			holder = (ViewHolder) convertView.getTag();
		final MailEntry item = getItem(position);
		holder.subject.setText(item.getSubject());
		holder.emailFrom.setText(item.getFromEmail());
		holder.date.setText(GTask.DUE_DATE_FORMAT.format(item.getDate()));
		holder.toBeImported.setOnCheckedChangeListener(
			new OnCheckedChangeListener() {	
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					item.setToBeImported(isChecked);
				}
			}
		);
		holder.subject.setSelected(true);
		return convertView;
	}

}
