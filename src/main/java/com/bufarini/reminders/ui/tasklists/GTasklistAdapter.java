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

package com.bufarini.reminders.ui.tasklists;

import java.util.ArrayList;
import java.util.List;

import com.bufarini.reminders.model.GTaskList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class GTasklistAdapter extends ArrayAdapter<GTaskList> {
	public static final int NO_SELECTED_ITEM = -1;

	private int selectedPos = NO_SELECTED_ITEM;
	private List<Integer> multipleSelection = new ArrayList<Integer>();

	public GTasklistAdapter(Context context, int textViewResourceId, List<GTaskList> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView text = (TextView) convertView;
		final GTaskList tasksList = (GTaskList) getItem(position);
		if (convertView == null) {
			LayoutInflater li =
				(LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			text = (TextView) li.inflate(android.R.layout.simple_list_item_1, null);
		}
		text.setText(tasksList.title);
		if (multipleSelection.contains(position))
			text.setBackgroundColor(Color.parseColor("#99CC00"));
		else {
			if (position == selectedPos)
				text.setBackgroundColor(Color.parseColor("#AA66CC"));
			else
				text.setBackgroundColor(Color.parseColor("#E3E3E2"));
		}
		return text;
	}
	
	public void setSelectedItem(int selectedPos) {
		this.selectedPos = selectedPos;
		notifyDataSetChanged();
	}
	
	public void setMultipleSelectedItem(int pos) {
		multipleSelection.add(pos);
		notifyDataSetChanged();
	}

	public void removeMultipleSelectedItem(Integer pos) {
		multipleSelection.remove(pos);
		notifyDataSetChanged();
	}

	public void removeSelections() {
		multipleSelection.clear();
	}
}
