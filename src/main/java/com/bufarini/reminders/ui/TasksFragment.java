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

package com.bufarini.reminders.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.DateUtils;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.ui.taskdetail.TaskDetailDialog;
import com.bufarini.reminders.ui.InsertTextFragment.InsertTaskEvent;
import com.bufarini.reminders.ui.tasklists.ListManager;
import com.bufarini.reminders.ui.tasklists.SaveItems;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class TasksFragment extends Fragment implements UndoBarController.UndoListener {
    public static final String REFRESH_TASKS_LIST = "refreshTaskList";
    private final Bus bus = BusProvider.getInstance();
    private GTaskAdapter adapter;
    private GTaskList parentList;
    private DragSortListView tasksView;
    private List<GTask> displayList = new ArrayList<GTask>(30);
    private UndoBarController undoBarController;
    private CheckBox hideCompleted, sortByDueDate;
    private boolean isHideCompleted, isSortedByDueDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        bus.register(this);
        View view = inflater.inflate(R.layout.items_list, container, false);
        return view;
    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (!isSortedByDueDate && from != to) {
                GTask task = adapter.getItem(from);
				displayList.remove(task);
                displayList.add(to, task);
                adapter = new GTaskAdapter(getActivity(), R.layout.item, displayList);
                tasksView.setAdapter(adapter);
            }
        }
    };

    private GTask undoTask;
    private int undoPosition;

    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            GTask task = adapter.getItem(which);
            task.isDeleted = true;
			displayList.remove(task);
            adapter = new GTaskAdapter(getActivity(), R.layout.item, displayList);
            tasksView.setAdapter(adapter);
            undoTask = task;
            undoPosition = which;
            String title =
                    (task.title == null || task.title.length() == 0) ? "" : "\"" + task.title + "\" ";
            NotificationUtils.cancelReminder(getActivity(), task);
            NotificationManager notificationManager =
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NotificationUtils.getNotificationId(undoTask.id));
            undoBarController.showUndoBar(false, "task " + title + "removed.", null);
        }
    };

    @Override
    public void onUndo(Parcelable token) {
        undoTask.isDeleted = false;
        displayList.add(undoPosition, undoTask);
        adapter = new GTaskAdapter(getActivity(), R.layout.item, displayList);
        tasksView.setAdapter(adapter);
        if (undoTask.reminderDate > 0)
            NotificationUtils.setReminder(getActivity(), undoTask);
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        hideCompleted = (CheckBox) getActivity().findViewById(R.id.hideCompleted);
        hideCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHideCompleted = ((CheckBox) v).isChecked();
                if (parentList != null) {
                    parentList.isModified = true;
                    parentList.isHideCompleted = isHideCompleted;
                    refreshTaskList(getActivity());
                }
            }
        });
        sortByDueDate = (CheckBox) getActivity().findViewById(R.id.sortByDate);
        sortByDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSortedByDueDate = ((CheckBox) v).isChecked();
                if (parentList != null) {
                    parentList.isModified = true;
                    parentList.isSortedByDueDate = isSortedByDueDate;
                    refreshTaskList(getActivity());
                }
            }
        });
        tasksView = (DragSortListView) getActivity().findViewById(R.id.tasksList);
        DragSortController controller = new DragSortController(tasksView);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setRemoveEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DRAG);
        controller.setRemoveMode(DragSortController.FLING_REMOVE);
        tasksView.setFloatViewManager(controller);
        tasksView.setOnTouchListener(controller);
        tasksView.setRemoveListener(onRemove);
        tasksView.setDropListener(onDrop);
        adapter = new GTaskAdapter(getActivity(), R.layout.item, displayList);
        tasksView.setAdapter(adapter);
        tasksView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("TaskDetail");
                if (prev != null)
                    ft.remove(prev);
                ft.addToBackStack(null);
                ft.commit();
                TaskDetailDialog taskDetailDialog = new TaskDetailDialog();
                taskDetailDialog.task = adapter.getItem(position);
                taskDetailDialog.show(getFragmentManager(), "TaskDetail");
            }
        });
        undoBarController = new UndoBarController(getActivity().findViewById(R.id.undobar), this);
    }

    public static final String SETUP_LIST = "setup_list";

    @Subscribe
    public void setupTaskList(final Object[] args) {
        if (SETUP_LIST.equals(args[0])) {
            parentList = (GTaskList) args[1];
            MemoryStore.getInstance().setActiveList(parentList);
            isHideCompleted = parentList.isHideCompleted;
            isSortedByDueDate = parentList.isSortedByDueDate;
            hideCompleted.setChecked(isHideCompleted);
            sortByDueDate.setChecked(isSortedByDueDate);
            refreshTaskList((Context) args[2]);
        }
    }

    private void refreshTaskList(Context context) {
        GTaskList parentList = MemoryStore.getInstance().getActiveList();
        displayList = new ArrayList<GTask>(parentList.tasks.size());
        for (GTask task : parentList.tasks)
            if (!task.isDeleted)
                if (task.completed == 0 || !isHideCompleted)
                    displayList.add(task);
        if (isSortedByDueDate && displayList.size() > 0) {
            Map<Long, List<GTask>> tasks = new TreeMap<Long, List<GTask>>(new Comparator<Long>() {
                @Override
                public int compare(Long lhs, Long rhs) {
                    if (lhs.intValue() == 0 && rhs.intValue() == 0)
                        return 0;
                    if (lhs.intValue() == 0)
                        return 1;
                    return lhs < rhs ? -1 : lhs > rhs ? 1 : 0;
                }
            });
            for (GTask task : displayList) {
                List<GTask> gTasks = tasks.get(task.dueDate);
                if (gTasks == null)
                    gTasks = new ArrayList<GTask>();
                gTasks.add(task);
                tasks.put(task.dueDate, gTasks);
            }
            List<GTask> sortedByDueDateAndPriority = new ArrayList<GTask>(displayList.size());
            for (Long dueDate : tasks.keySet()) {
                List<GTask> gTasks = tasks.get(dueDate);
                if (gTasks != null) {
                    Collections.sort(gTasks, new Comparator<GTask>() {
                        @Override
                        public int compare(GTask lhs, GTask rhs) {
                            return lhs.priority < rhs.priority ? -1 : (lhs.priority > rhs.priority ? 1 : 0);
                        }
                    });
                    for (GTask task : gTasks)
                        sortedByDueDateAndPriority.add(task);
                }
            }
            displayList = sortedByDueDateAndPriority;
        }
        adapter = new GTaskAdapter(context, R.layout.item, displayList);
        tasksView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onRefreshTaskList(final Object[] args) {
        if (REFRESH_TASKS_LIST.equals(args[0]))
            refreshTaskList((Context) args[1]);
    }

    @Subscribe
    public void onInsertTask(InsertTaskEvent insertTaskEvent) {
        MemoryStore memory = MemoryStore.getInstance();
        if (parentList == null) {
            parentList = memory.getActiveList();
            if (parentList == null) {
                parentList = ListManager.newGTaskList("to do");
                memory.setActiveList(parentList);
            }
        }
        insertTaskEvent.getTask().list = parentList;
        if (parentList.tasks == null)
            parentList.tasks = new ArrayList<GTask>(30);
        parentList.tasks.add(insertTaskEvent.getTask());
        displayList.add(insertTaskEvent.getTask());
        if (isSortedByDueDate) {
            displayList = DateUtils.sortTasksByDate(displayList);
        }
        adapter = new GTaskAdapter(insertTaskEvent.getContext(), R.layout.item, displayList);
        tasksView.setAdapter(adapter);
        new Thread(
                new SaveItems(insertTaskEvent.getContext(), false, parentList.accountName, memory.getActiveLists())
        ).start();
    }
}
