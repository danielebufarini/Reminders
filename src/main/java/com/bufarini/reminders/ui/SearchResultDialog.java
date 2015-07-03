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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.event.RefreshView;
import com.bufarini.reminders.model.BaseItem;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.model.Item;
import com.bufarini.reminders.ui.taskdetail.TaskDetailDialog;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;


public class SearchResultDialog extends DialogFragment {
    private static final Bus bus = BusProvider.getInstance();
    private static final RefreshView REFRESH_VIEW = new RefreshView();

    private ListView tasksView;
    private GTaskAdapter adapter;
    private List<GTask> tasks;
    private String searchKeyword;

    public SearchResultDialog() {
        bus.register(this);
    }

    public void setTasks(List<GTask> tasks) {
        this.tasks = tasks;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            Object obj = adapter.getItem(which);
            if (obj instanceof GTask) {
                adapter.remove((GTask) obj);
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialog = inflater.inflate(R.layout.search_result, null);
        tasksView = (ListView) dialog.findViewById(R.id.searchResult);
        tasksView.setAdapter(adapter);
        tasksView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object obj = adapter.getItem(position);
                if (obj instanceof GTask) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag("TaskDetail");
                    if (prev != null)
                        ft.remove(prev);
                    ft.addToBackStack(null);
                    ft.commit();
                    TaskDetailDialog taskDetailDialog = new TaskDetailDialog();
                    taskDetailDialog.task = (GTask) obj;
                    taskDetailDialog.show(getFragmentManager(), "TaskDetail");
                }
            }
        });
        adapter = new GTaskAdapter(getActivity(), R.layout.email_single_item, tasks);
        tasksView.setAdapter(adapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog)
               .setTitle("Search results")
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(R.string.saveAsList, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       MemoryStore memoryStore = MemoryStore.getInstance();
                       GTaskList list = new GTaskList();
                       list.title = "Search results for '" + searchKeyword +  "'";
                       list.googleId = "";
                       list.accountName = memoryStore.getAccountName();
                       list.updated = System.currentTimeMillis();
                       List<GTask> gTasks = new ArrayList<GTask>(tasks.size());
                       for (BaseItem item : tasks)
                           if (item instanceof GTask) {
                               GTask newTask = new GTask((GTask) item);
                               newTask.id = Item.generateId();
                               gTasks.add(newTask);
                           }
                       list.tasks = gTasks;
                       List<GTaskList> activeLists = memoryStore.getActiveLists();
                       activeLists.add(list);
                       bus.post(REFRESH_VIEW);
                   }
               });
        return builder.create();
    }
}
