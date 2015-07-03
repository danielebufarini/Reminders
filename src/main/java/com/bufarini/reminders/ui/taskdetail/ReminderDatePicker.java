package com.bufarini.reminders.ui.taskdetail;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bufarini.R;

public class ReminderDatePicker extends DialogFragment {
	public TextView reminderText;
	public Calendar reminderDate;
	public Spinner interval;
	
	public static String formatDate(int year, int month, int day, int hour, int minute) {
		//TODO use StringBuilder instead of String to compose the result
		String date = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", day);
		String time = String.format("%02d", hour) + ":" + String.format("%02d", minute);
		time = String.format("%" + (date.length() - time.length()) + "c", ' ') + time;
		return date + "\n" + time;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.date_time_picker, null);
		final DatePicker datePicker = (DatePicker) view.findViewById(R.id.alarmDatePicker);
		final TimePicker timePicker =  (TimePicker) view.findViewById(R.id.alarmTimePicker);
		timePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
		if (reminderDate.getTimeInMillis() <= 0)
			reminderDate.setTimeInMillis(System.currentTimeMillis());
		timePicker.setCurrentHour(reminderDate.get(Calendar.HOUR_OF_DAY));
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view).setPositiveButton(R.string.taskDetailDone,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					reminderDate.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
							timePicker.getCurrentHour(), timePicker.getCurrentMinute());
					reminderText.setText(
						formatDate(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
							timePicker.getCurrentHour(), timePicker.getCurrentMinute())
					);					
				}
			})
			.setNegativeButton(R.string.taskDetailDialogCancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					reminderDate.clear();
					interval.setSelection(0);
					reminderText.setText(getResources().getString(R.string.taskDetailRemindme));
				}
			})
			.setNeutralButton(R.string.cancel, null);
		return builder.create();
	}
}
