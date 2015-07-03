package com.bufarini.reminders.ui.taskdetail;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.Priority;
import com.bufarini.reminders.ui.PriorityAdapter;
import com.bufarini.reminders.ui.TasksFragment;
import com.bufarini.reminders.ui.tasklists.SaveItems;
import com.squareup.otto.Bus;

public class TaskDetailDialog extends DialogFragment {
	private final Bus bus = BusProvider.getInstance();	
	private static final long[] INTERVALS = {
		0L, // none
		60000L, // one minute
		3600000L, // one hour
		86400000L, // one day
		604800000L, // one week
		1209600000L, // two weeks
		2630000000L, // one month
		31560000000L // one year
	};
	private static final Map<Long, Integer> INTERVALS_MAP = new HashMap<Long, Integer>(INTERVALS.length);

	static {
		for (int i = 0; i < INTERVALS.length; ++i)
			INTERVALS_MAP.put(INTERVALS[i], i);
	}
	private Calendar reminderDate = Calendar.getInstance(), dueDate = Calendar.getInstance();
	public GTask task;
    public boolean isReadOnly = false;
	
	public TaskDetailDialog() {
		super();
		bus.register(this);
	}
	
	private void adjustInterval() {
		long interval = task.getReminderInterval();
		if (interval == 18144000000L)
			task.setReminderInterval(2630000000L);
		else if (interval == 217728000000L)
			task.setReminderInterval(31560000000L);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View dialog = inflater.inflate(R.layout.task_detail, null);
		adjustInterval();
		final EditText title = (EditText) dialog.findViewById(R.id.taskTitle);
		title.setText(task.title);
        Linkify.addLinks(title, Linkify.ALL);
        final EditText notes = (EditText) dialog.findViewById(R.id.note);
        notes.setText(task.notes);
        Linkify.addLinks(notes, Linkify.ALL);
        final Spinner interval = (Spinner) dialog.findViewById(R.id.reminderInterval);
		interval.setSelection(INTERVALS_MAP.get(task.getReminderInterval()));
		final TextView reminderText = (TextView) dialog.findViewById(R.id.alarmDateTime);
		reminderText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ReminderDatePicker timePicker = new ReminderDatePicker();
				timePicker.reminderDate = reminderDate;
				timePicker.reminderText = reminderText;
				timePicker.interval = interval;
				timePicker.show(getFragmentManager(), "DateTimePicker");
			};
		});
		if (task.reminderDate > 0) {
			reminderDate.setTimeInMillis(task.reminderDate);
			reminderText.setText(
				ReminderDatePicker.formatDate(
					reminderDate.get(Calendar.YEAR), reminderDate.get(Calendar.MONTH),
					reminderDate.get(Calendar.DAY_OF_MONTH),
					reminderDate.get(Calendar.HOUR_OF_DAY), reminderDate.get(Calendar.MINUTE)
				)
			);
		} else
			reminderDate.setTimeInMillis(0);
		final TextView dueDateText = (TextView) dialog.findViewById(R.id.dueDate);
		dueDateText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DueDatePicker datePicker = new DueDatePicker();
				datePicker.setDueDate(dueDate);
				datePicker.dueDateTextView = dueDateText;
				datePicker.show(getFragmentManager(), "DueDatePicker");
			};
		});
		if (task.dueDate > 0) {
			dueDate.setTimeInMillis(task.dueDate);
			dueDateText.setText(DueDatePicker.formatDate(dueDate));
		} else
			dueDate.setTimeInMillis(0);
        final Spinner priority = (Spinner) dialog.findViewById(R.id.priority);
        priority.setAdapter(new PriorityAdapter(getActivity(), R.layout.priority_item, Priority.PRIORITIES));
        priority.setSelection(task.priority);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (isReadOnly) {
            builder.setView(dialog)
                .setPositiveButton(android.R.string.ok, null);
        } else {
            builder.setView(dialog) //.setTitle(R.string.taskDetailDialogTitle)
                .setPositiveButton(R.string.taskDetailDialogSave, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        boolean hasPreviousReminder = task.reminderDate > 0;
                        task.title = title.getText().toString();
                        task.notes = notes.getText().toString();
                        task.dueDate = (dueDate.getTimeInMillis() > 0) ? dueDate.getTimeInMillis() : 0;
                        task.isModified = true;
                        task.updated = System.currentTimeMillis();
                        task.reminderDate = reminderDate.getTimeInMillis();
                        task.priority = priority.getSelectedItemPosition() == 0 ?
                                    Priority.NONE.getPriority() : priority.getSelectedItemPosition();
                        final Context context = getActivity();
                        if (task.reminderDate > 0) {
                            task.reminderInterval = INTERVALS[interval.getSelectedItemPosition()];
                            NotificationUtils.setReminder(getActivity(), task);
                            Toast toast =
                                    Toast.makeText(context, formatToast(context, task.reminderDate), Toast.LENGTH_LONG);
                            toast.show();
                        } else {
                            task.reminderInterval = 0;
                            if (hasPreviousReminder)
                                NotificationUtils.cancelReminder(getActivity(), task);
                        }
                        bus.post(new Object[]{TasksFragment.REFRESH_TASKS_LIST, context});
                        MemoryStore memory = MemoryStore.getInstance();
                        new Thread(
                                new SaveItems(context, false, task.list.accountName, memory.getActiveLists())
                        ).start();

                    }
                }).setNegativeButton(R.string.cancel, null);
        }
		return builder.create();
	}

	/**
	* format "Reminder set for 2 days 7 hours and 53 minutes from
	* now"
	*/
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}
