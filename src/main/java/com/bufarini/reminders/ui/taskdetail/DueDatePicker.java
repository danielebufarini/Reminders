package com.bufarini.reminders.ui.taskdetail;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import com.bufarini.R;

public class DueDatePicker extends DialogFragment {
	public TextView dueDateTextView;
	private Calendar dueDate;
	private int year, month, day;

	public static String formatDate(Calendar cal) {
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", day);
	}

	public void setDueDate(Calendar dueDate) {
		this.dueDate = dueDate;
		if (dueDate.getTimeInMillis() > 0) {
			year = dueDate.get(Calendar.YEAR);
			month = dueDate.get(Calendar.MONTH);
			day = dueDate.get(Calendar.DAY_OF_MONTH);
		} else {
			Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
			year = now.get(Calendar.YEAR);
			month = now.get(Calendar.MONTH);
			day = now.get(Calendar.DAY_OF_MONTH);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), null, year, month, day) {
			@Override
			public void onDateChanged(DatePicker view, int yy, int mm, int dd) {
				year = yy;
				month = mm;
				day = dd;
			};
		};
		datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE,
				getResources().getString(R.string.taskDetailDialogCancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dueDate.clear();
						dueDateTextView.setText(getResources().getString(R.string.taskDetailSetduedate));
					}
				});
		datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE,
				getResources().getString(R.string.taskDetailDone),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dueDate.set(year, month, day);
						dueDateTextView.setText(formatDate(dueDate));
					}
				});
		return datePickerDialog;
	}
}
