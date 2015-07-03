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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import android.content.Context;
import android.util.Log;

import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.model.Item;

public class LoadItems implements Runnable {
	private static final String LOGTAG = LoadItems.class.getSimpleName();
	private final boolean isSyncWithGTasksEnabled;
	private final Context context;
	private final String accountName;
	
	public LoadItems(Context context, boolean isSyncWithGTasksEnabled, String accountName) {
		this.isSyncWithGTasksEnabled = isSyncWithGTasksEnabled;
		this.context = context;
		this.accountName = accountName;
	}
	
	private void reconcile(List<? extends Item> dbItems, List<? extends Item> googleItems) {
		if (googleItems.size() > 0)
			for (Item dbItem: dbItems) {
				for (Item googleItem: googleItems)
					if (dbItem.googleId.equals(googleItem.googleId)) {
						googleItem.id = dbItem.id;
						googleItem.isStored = dbItem.isStored;
						if (dbItem.hasChildren())
							reconcile(dbItem.getChildren(), googleItem.getChildren());
						break;
					}
			}
	}
	
	private List<Item> mergeItems(List<? extends Item> dbItems, List<? extends Item> googleItems) {
		List<Item> result = new ArrayList<Item>(30);
		
		for (Item dbItem: dbItems) {
			if (googleItems.contains(dbItem)) {
				Item googleItem = googleItems.get(googleItems.indexOf(dbItem));
				List<Item> mergedItems = null;
				if (dbItem.hasChildren())
					mergedItems = mergeItems(dbItem.getChildren(), googleItem.getChildren());
				if (dbItem.updated < googleItem.updated) {
					googleItem.id = dbItem.id;
					googleItem.setReminder(dbItem.getReminder());
					googleItem.setReminderInterval(dbItem.getReminderInterval());
					dbItem = googleItem;
				}
				dbItem.setChildren(mergedItems);
			} else {
				if (dbItem.isMerged)
					dbItem.isDeleted = true;
			}
			result.add(dbItem);
		}
		for (Item googleItem: googleItems) {
			if (result.contains(googleItem))
				continue;
			if (dbItems.contains(googleItem)) {
				Item dbItem = dbItems.get(dbItems.indexOf(googleItem));
				List<Item> mergedItems = null;
				if (googleItem.hasChildren())
					mergedItems = mergeItems(dbItem.getChildren(), googleItem.getChildren());
				if (googleItem.updated < dbItem.updated) {
					dbItem.googleId = googleItem.googleId;
					googleItem = dbItem;
				}
				googleItem.setChildren(mergedItems);
			}
			result.add(googleItem);
		}
		
		return result;
	}
	
	public List<GTaskList> getItems() {
		CyclicBarrier barrier = new CyclicBarrier(isSyncWithGTasksEnabled ? 3 : 2);
		List<GTaskList> dbItems = new ArrayList<GTaskList>(30);
		List<GTaskList> googleItems = new ArrayList<GTaskList>(30);
		new Thread(new LoadItemsFromDB(context, barrier, dbItems, accountName)).start();
		if (isSyncWithGTasksEnabled)
			new Thread(new LoadItemsFromGoogle(context, barrier, googleItems, accountName)).start();
		try {
			barrier.await();
			reconcile(dbItems, googleItems);
			List<? extends Item> items = isSyncWithGTasksEnabled ? mergeItems(dbItems, googleItems) : dbItems;
			return ((List<GTaskList>) items);
		} catch (InterruptedException e) {
			Log.e(LOGTAG, "", e);
		} catch (BrokenBarrierException e) {
			Log.e(LOGTAG, "", e);
		}
		return null;
	}
	
	@Override
	public void run() {
		List<GTaskList> lists = getItems();
	}
}
