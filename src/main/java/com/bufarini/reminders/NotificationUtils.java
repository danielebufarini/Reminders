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

package com.bufarini.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.bufarini.R;
import com.bufarini.reminders.model.GTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationUtils {
	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String DUE_DATE = "dueDate";
	public static final String LIST_ID = "listId";
    
    private static final ConcurrentHashMap<Long, Integer> NOTIFICATION_ID = new ConcurrentHashMap<Long, Integer>(100);
    private static final MemoryStore MEMORY_STORE = MemoryStore.getInstance();

    static {
        MEMORY_STORE.setAtomicInt(new AtomicInteger(0));
    }

    public static int getNotificationId(long id) {
        Integer newId = NOTIFICATION_ID.get(id);
        if (newId == null) {
            synchronized (MEMORY_STORE) {
                newId = NOTIFICATION_ID.get(id);
                if (newId == null) {
                    newId = MEMORY_STORE.getNextInt();
                    NOTIFICATION_ID.put(id, newId);
                }
            }
        }
        return newId;
    }
    
	public static void setReminder(Context context, long id, String title, long dueDate,
			long listId, long reminderDate, long reminderInterval)
	{
		String actionName = context.getResources().getString(R.string.intent_action_alarm);
		Intent alarmIntent = new Intent(actionName);
		alarmIntent.putExtra(NotificationUtils.ID, id);
        alarmIntent.putExtra(NotificationUtils.TITLE, title);
        alarmIntent.putExtra(NotificationUtils.DUE_DATE, dueDate);
        alarmIntent.putExtra(NotificationUtils.LIST_ID, listId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, getNotificationId(id), alarmIntent, 0);
	    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    if (reminderInterval > 0) {
	    	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminderDate,
	    			reminderInterval, alarmPendingIntent);
	    	if (Log.LOGV)
	    		Log.v("TaskDetailDialog set recurring reminder at " + dueDate);
	    }
	    else {
	    	alarmManager.set(AlarmManager.RTC_WAKEUP, reminderDate, alarmPendingIntent);
	    	if (Log.LOGV)
	    		Log.v("TaskDetailDialog set reminder at " + dueDate);
	    }
	}

    public static void setReminder(Context context, GTask task)
	{
		String actionName = context.getResources().getString(R.string.intent_action_alarm);
		Intent alarmIntent = new Intent(actionName);
		alarmIntent.putExtra(NotificationUtils.ID, task.id);
        alarmIntent.putExtra(NotificationUtils.TITLE, task.title);
        alarmIntent.putExtra(NotificationUtils.DUE_DATE, task.dueDate);
        alarmIntent.putExtra(NotificationUtils.LIST_ID, task.list.id);
        alarmIntent.putExtra("task", task);
        PendingIntent alarmPendingIntent =
            PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    if (task.reminderInterval > 0) {
	    	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, task.reminderDate,
                    task.reminderInterval, alarmPendingIntent);
	    	if (Log.LOGV)
	    		Log.v("TaskDetailDialog set recurring reminder at " + task.dueDate);
	    } else {
	    	alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminderDate, alarmPendingIntent);
	    	if (Log.LOGV)
	    		Log.v("TaskDetailDialog set reminder at " + task.dueDate);
	    }
	}

    public static void cancelReminder(Context context, GTask task) {
        if (task != null) {
            String actionName = context.getResources().getString(R.string.intent_action_alarm);
            Intent alarmIntent = new Intent(actionName);
            alarmIntent.putExtra(NotificationUtils.ID, task.id);
            if (task.title != null)
                alarmIntent.putExtra(NotificationUtils.TITLE, task.title);
            alarmIntent.putExtra(NotificationUtils.DUE_DATE, task.dueDate);
            alarmIntent.putExtra(NotificationUtils.LIST_ID, task.list.id);
            alarmIntent.putExtra("task", task);
            PendingIntent alarmPendingIntent =
                    PendingIntent.getBroadcast(context, getNotificationId(task.id), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);
            alarmPendingIntent.cancel();
            if (Log.LOGV)
                Log.v("TaskDetailDialog deleted reminder set at " + task.dueDate);
        }
    }
}
