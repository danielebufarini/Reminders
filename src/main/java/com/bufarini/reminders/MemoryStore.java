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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.bufarini.reminders.model.GTaskList;

public class MemoryStore {
	private List<GTaskList> activelists = new ArrayList<GTaskList>();
    private AtomicInteger value;
    private GTaskList activeList;
    private String accountName;
    private AtomicLong atomicLong;

	private MemoryStore() {
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		public static final MemoryStore INSTANCE = new MemoryStore();
	}

	public static MemoryStore getInstance() {
		return SingletonHolder.INSTANCE;
	}

    private final static Object listsLock = new Object();

	public List<GTaskList> getActiveLists() {
        synchronized (listsLock) {
            return Collections.synchronizedList(activelists);
        }
	}

	public void setActiveLists(List<GTaskList> activelists) {
        synchronized (listsLock) {
            this.activelists = activelists;
        }
	}

	public void setAtomicInt(AtomicInteger value) {
		this.value = value;
	}

	public int getNextInt() {
		return value.getAndIncrement();
	}

    private final static Object listLock = new Object();

    public GTaskList getActiveList() {
        synchronized (listLock) {
            return activeList;
        }
    }

    public void setActiveList(GTaskList activeList) {
        synchronized (listLock) {
            this.activeList = activeList;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAtomicLong(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    public long getNextLong() {
        return atomicLong.getAndIncrement();
    }
}
