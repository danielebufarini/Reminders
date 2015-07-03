package com.bufarini.reminders.model;

import android.database.sqlite.SQLiteDatabase;

import com.bufarini.reminders.MemoryStore;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends BaseItem implements Serializable {
    private static final long serialVersionUID = 987654322L;

    public static final List<Item> EMPTY_LIST = new ArrayList<Item>(0);
	public long id, updated;
	public String title, googleId, accountName;
	public boolean isDeleted, isMerged;
	public transient boolean isModified, isStored;

	public Item() {
		id = generateId();
	}

    public Item(long id) {
        this.id = id;
    }

	public Item(Item that) {
		this.id = that.id;
		this.updated = that.updated;
        this.title = that.title;
		this.googleId = that.googleId;
		this.accountName = that.accountName;
		this.isDeleted = that.isDeleted;
		this.isMerged = that.isMerged;
		this.isModified = that.isModified;
	}

    public static long generateId() {
        return MemoryStore.getInstance().getNextLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        if (id != item.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    public abstract void insert(SQLiteDatabase db);
	public abstract void delete(SQLiteDatabase db);
	public abstract void merge(SQLiteDatabase db);
	
	public abstract void insert(Tasks googleService) throws IOException;
	public abstract void delete(Tasks googleService) throws IOException;
	public abstract void merge(Tasks googleService) throws IOException;
	
	public abstract boolean hasChildren();
	public abstract List<? extends Item> getChildren();
	public abstract void setChildren(List<? extends Item> items);

	public abstract boolean hasReminder();
	public abstract long getReminder();
	public abstract void setReminder(long reminder);
	public abstract long getReminderInterval();
	public abstract void setReminderInterval(long intervall);
}
