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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;

import com.bufarini.R;
import com.bufarini.reminders.model.GTask;

public class DateUtils {
	public static final SimpleDateFormat EUROPEAN_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
	private static final SimpleDateFormat DATE_FORMAT_WO_YEAR = new SimpleDateFormat("EEEE, d MMMM", Locale.US);
	private static final SimpleDateFormat DATE_FORMAT_W_YEAR = new SimpleDateFormat("EEEE, d MMMM y", Locale.US);
	
	public static String formatDate(Calendar cal) {
		String formattedDate;
		
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		int nowYear = now.get(Calendar.YEAR);
		int nowMonth = now.get(Calendar.MONTH);
		int nowDay = now.get(Calendar.DAY_OF_MONTH);
		
		if (year == nowYear && month == nowMonth) {
			if (day == nowDay)
				formattedDate = "today";
			else if (day == (nowDay + 1))
				formattedDate = "tomorrow";
			else
				formattedDate = DATE_FORMAT_WO_YEAR.format(cal.getTime());
		} else
			formattedDate = (year == nowYear) ? DATE_FORMAT_WO_YEAR.format(cal.getTime()) :
					DATE_FORMAT_W_YEAR.format(cal.getTime());
		
		return formattedDate;
	}
	
	public static boolean isToday(Calendar day) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) &&
				day.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
					day.get(Calendar.YEAR) == now.get(Calendar.YEAR))
			return true;
		return false;
	}
	
	public static boolean isTomorrow(Calendar day) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.get(Calendar.DAY_OF_MONTH) == (now.get(Calendar.DAY_OF_MONTH) + 1) &&
				day.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
					day.get(Calendar.YEAR) == now.get(Calendar.YEAR))
			return true;
		return false;
	}
	
	public static boolean isNextWeek(Calendar day) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.get(Calendar.DAY_OF_MONTH) >= now.get(Calendar.DAY_OF_MONTH) &&
				day.get(Calendar.DAY_OF_MONTH) <= (now.get(Calendar.DAY_OF_MONTH) + 7) &&
				day.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
				day.get(Calendar.YEAR) == now.get(Calendar.YEAR))
			return true;
		return false;
	}
	
	public static boolean isInTheFuture(Calendar day) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.get(Calendar.YEAR) > now.get(Calendar.YEAR))
			return true;
		if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& day.get(Calendar.MONTH) > now.get(Calendar.MONTH))
			return true;
		if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& day.get(Calendar.MONTH) == now.get(Calendar.MONTH)
				&& day.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH))
			return true;
		return false;
	}
	
	public static boolean isInThePast(Calendar day) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.getTimeInMillis() > 0 && day.before(now))
			return true;
		return false;
	}
	
	public static boolean isInThePast2(Calendar day) {
		if (day.getTimeInMillis() <= 0)
			return false;
		Calendar now = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		now.setTimeInMillis(System.currentTimeMillis());
		if (day.get(Calendar.YEAR) < now.get(Calendar.YEAR))
			return true;
		if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& day.get(Calendar.MONTH) < now.get(Calendar.MONTH))
			return true;
		if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& day.get(Calendar.MONTH) == now.get(Calendar.MONTH)
				&& day.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH))
			return true;
		return false;
	}
	
	public static List<GTask> sortTasksByDate(final List<GTask> tasks) {
		List<GTask> firstTwoDays = new ArrayList<GTask>(10);
		List<GTask> nextWeek = new ArrayList<GTask>(10);
		List<GTask> inTheFuture = new ArrayList<GTask>(10);
		List<GTask> inThePast = new ArrayList<GTask>(10);
		List<GTask> noDueDate = new ArrayList<GTask>(10);
		
		Calendar cal = Calendar.getInstance();
		for (GTask task: tasks) {
			cal.setTimeInMillis(task.dueDate);
			if (DateUtils.isToday(cal) || DateUtils.isTomorrow(cal))
				firstTwoDays.add(task);
			else if (DateUtils.isNextWeek(cal))
				nextWeek.add(task);
			else if (DateUtils.isInThePast2(cal))
				inThePast.add(task);
			else if (DateUtils.isInTheFuture(cal))
				inTheFuture.add(task);
			else
				noDueDate.add(task);
		}
		Collections.sort(firstTwoDays);
		Collections.sort(nextWeek);
		Collections.sort(inTheFuture);
		Collections.sort(inThePast);
		List<GTask> result = new ArrayList<GTask>(tasks.size());
		result.addAll(inThePast);
		result.addAll(firstTwoDays);
		result.addAll(nextWeek);
		result.addAll(inTheFuture);
		result.addAll(noDueDate);
		return result;
	}
	
	public static String getAppName(Context context) {
		String appName = context.getResources().getString(R.string.app_name)
        		+ "/" + context.getResources().getString(R.string.app_version);
		return appName;
	}
}
