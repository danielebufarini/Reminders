package com.bufarini.reminders.collaboration;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bufarini.R;

public class FilterableContactsAdapter extends BaseAdapter implements Filterable {
	private ArrayList<ContactInfo> original, filtered;
	private final Context context;
	private final int resource;
	private Filter filter;
	
	private static class ViewHolder {
		TextView email;
		TextView name;
	}
	
	public FilterableContactsAdapter(Context context, int resource, List<ContactInfo> items) {
		this.context = context;
		this.resource = resource;
		original = new ArrayList<ContactInfo>(items);
		filtered = new ArrayList<ContactInfo>(items);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			LinearLayout rowView = new LinearLayout(context);
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			li.inflate(resource, rowView, true);
			holder = new ViewHolder();
			holder.email = (TextView) rowView.findViewById(R.id.contact_email);
			holder.name = (TextView) rowView.findViewById(R.id.contact_name);
			convertView = rowView;
			convertView.setTag(holder);
		} else
			holder = (ViewHolder) convertView.getTag();
		final ContactInfo item = (ContactInfo) getItem(position);
		holder.email.setText(item.getEmail());
		holder.name.setText(item.getName());
		return convertView;
	}

	@Override
	public Filter getFilter() {
		if (filter == null)
	        filter = new ContactsFilter();

	    return filter;
	}
	
	private class ContactsFilter extends Filter
	{
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			filtered = (ArrayList<ContactInfo>) results.values;
			notifyDataSetChanged();
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			ArrayList<ContactInfo> foundContacts = new ArrayList<ContactInfo>();
			
			if (original == null)
				original = new ArrayList<ContactInfo>(filtered);
			
			constraint = constraint.toString().toLowerCase();
			for (int i = 0; i < original.size(); i++) {
				ContactInfo contact = original.get(i);
				if (contact.getName().toLowerCase().contains(constraint.toString())
						|| contact.getEmail().toLowerCase().contains(constraint.toString()))
					foundContacts.add(contact);
			}
			results.count = foundContacts.size();
			results.values = foundContacts;

			return results;
		}
	}

	@Override
	public int getCount() {
		return filtered.size();
	}

	@Override
	public Object getItem(int position) {
		return filtered.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
