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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.DateUtils;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.db.Tables;
import com.bufarini.reminders.event.RefreshView;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.model.Header;
import com.bufarini.reminders.ui.taskdetail.TaskDetailDialog;
import com.bufarini.reminders.ui.tasklists.ListManager;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class Reminders extends SlidingFragmentActivity {
	public static final String SHOW_CONTENT = "showContent", CHANGE_TITLE = "changeTitle";
    private static final RefreshView REFRESH_VIEW = new RefreshView();
	private static final Bus bus = BusProvider.getInstance();
    private final MemoryStore memoryStore = MemoryStore.getInstance();

	private TextView listName;
	private AlertDialog aboutDialog;

    private static final HttpTransport TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    public static Tasks getGoogleTasksService(final Context context, String accountName) {
        final GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(context, ListManager.TASKS_SCOPES);
        credential.setSelectedAccountName(accountName);
        Tasks googleService =
            new Tasks.Builder(TRANSPORT, JSON_FACTORY, credential)
                     .setApplicationName(DateUtils.getAppName(context))
                     .setHttpRequestInitializer(new HttpRequestInitializer() {
                         @Override
                         public void initialize(HttpRequest httpRequest) {
                             credential.initialize(httpRequest);
                             httpRequest.setConnectTimeout(3 * 1000);  // 3 seconds connect timeout
                             httpRequest.setReadTimeout(3 * 1000);  // 3 seconds read timeout
                         }
                     })
                     .build();
        return googleService;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bus.register(this);
		ActionBar actionBar = getActionBar();
		actionBar.setCustomView(R.layout.custom_actionbar);
		listName = (TextView) actionBar.getCustomView().findViewById(R.id.viewListNameActionBar);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);

		setContentView(R.layout.main);
		setBehindContentView(R.layout.task_lists);
		
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		setSlidingActionBarEnabled(false);
		
		actionBar.setIcon(R.drawable.ic_drawer);
		actionBar.show();
		
		View view = getLayoutInflater().inflate(R.layout.about_dialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(view).setTitle(getResources().getString(R.string.menu_item_about)).setPositiveButton(android.R.string.ok, null);
		aboutDialog = builder.create();

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
            handleSearchIntent(intent);
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null
                && "text/plain".equals(intent.getType()))
            handleSendText(intent);
	}

    private void handleSearchIntent(final Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        if (query != null) {
            List<GTask> tasksFound = doSearchTasks(query);
            if (tasksFound.size() > 0) {
                SearchResultDialog dialog = new SearchResultDialog();
                dialog.setTasks(tasksFound);
                dialog.setSearchKeyword(query);
                dialog.show(getFragmentManager(), "SearchResultDialog");
            } else
                Toast.makeText(this, getString(R.string.no_tasks_found), Toast.LENGTH_LONG).show();

            if (searchMenuItem != null)
                searchMenuItem.collapseActionView();
        }
    }

    private List<GTask> doSearchTasks(String query) {
        List tasksFound = new ArrayList(30);
        for (GTaskList list : memoryStore.getActiveLists()) {
            Header header = new Header();
            header.title = list.title;
            tasksFound.add(header);
            boolean removeHeader = true;
            for (GTask task : list.tasks)
                if (task.accountName.equals(memoryStore.getAccountName()) && !task.isDeleted)
                    if ((task.title != null && task.title.toLowerCase().contains(query.toLowerCase()))
                            || (task.notes != null && task.notes.toLowerCase().contains(query.toLowerCase()))) {
                        tasksFound.add(task);
                        removeHeader = false;
                    }
            if (removeHeader)
                tasksFound.remove(header);
        }
        return tasksFound;
    }

    private MenuItem searchMenuItem;

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.search);
        final SearchView searchView =
                (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                if(!queryTextFocused) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });

		return true;
	}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSearchIntent(intent);
        String action = intent.getStringExtra("action");
        GTask task = (GTask) intent.getSerializableExtra("task");
        if ("view task".equals(action)) {
            long listId = task.list.id;
            MemoryStore memoryStore = MemoryStore.getInstance();
            List<GTaskList> activeLists = memoryStore.getActiveLists();
            GTaskList listFound = null;
            for (GTaskList list : activeLists)
                if (list.id == listId) {
                    listFound = list;
                    break;
                }
            if (listFound != null) {
                memoryStore.setActiveList(listFound);
                bus.post(new Object[]{TasksFragment.SETUP_LIST, listFound, this});
                listName.setText(listFound.title);
            }
            TaskDetailDialog taskDetailDialog = new TaskDetailDialog();
            taskDetailDialog.task = task;
            taskDetailDialog.isReadOnly = (listFound == null || task.isDeleted);
            taskDetailDialog.show(getFragmentManager(), "TaskDetail");
        } else if ("refresh list if active".equals(action)) {
            GTaskList activeList = MemoryStore.getInstance().getActiveList();
            for (GTask gTask : activeList.tasks)
                if (gTask.id == task.id) {
                    gTask.reminderDate = 0L;
                    gTask.reminderInterval = 0L;
                }
            if (task.list.id == activeList.id)
                bus.post(new Object[]{TasksFragment.REFRESH_TASKS_LIST, this});
        }
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				SlidingMenu sm = getSlidingMenu();
				if (sm.isMenuShowing())
					sm.showContent();
				else
					sm.showMenu();
				return true;
			case R.id.menu_about:
				aboutDialog.show();
				return true;
			//case R.id.menu_calendar_view:
			default:
	            return super.onOptionsItemSelected(item);
		}
	}

	@Subscribe
	public void changeListTitle(String...strings) {
		if (CHANGE_TITLE.equals(strings[0]))
			listName.setText(strings[1]);
	}
	
	@Subscribe
	public void onShowContent(String cmd) {
		if (SHOW_CONTENT.equals(cmd))
			getSlidingMenu().showContent();
	}

    private static class ItemList {
        public long id;
        public String title;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private boolean updateListInMemory(GTaskList list, GTask task) {
        MemoryStore memoryStore = MemoryStore.getInstance();
        List<GTaskList> lists = memoryStore.getActiveLists();
        if (lists != null)
            for (GTaskList gTaskList : lists)
                if (gTaskList.id == list.id) {
                    gTaskList.tasks.add(task);
                    return true;
                }
        return false;
    }

    private boolean createListInMemory(GTaskList list, GTask task) {
        MemoryStore memoryStore = MemoryStore.getInstance();
        List<GTaskList> lists = memoryStore.getActiveLists();
        if (lists != null) {
            list.tasks.add(task);
            lists.add(list);
            return true;
        }
        return false;
    }

    private void handleSendText(Intent intent) {
        final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            final Context context = getApplication();
            SharedPreferences prefs =
                    context.getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
            final String accountName = prefs.getString(ListManager.PREF_ACCOUNT_NAME, null);
            DialogFragment selectList = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View dialog = inflater.inflate(R.layout.import_intent_dialog, null);
                    final EditText text = (EditText) dialog.findViewById(R.id.listNewList);
                    final List<ItemList> items = getListNames(getActivity());
                    final List<String> titles = new ArrayList<String>(items.size());
                    for (ItemList item : items)
                        titles.add(item.title);
                    final ArrayAdapter<String> adapter = new ListTitleAdapter(context, R.layout.list_title, titles);
                    ListView lists = (ListView) dialog.findViewById(R.id.taskLists);
                    lists.setAdapter(adapter);
                    lists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            SQLiteDatabase db = null;
                            try {
                                RemindersDbHelper dbHelper = new RemindersDbHelper(context);
                                db = dbHelper.getReadableDatabase();
                                GTaskList list = new GTaskList(items.get(position).id);
                                GTask task = new GTask();
                                task.title = sharedText;
                                task.googleId = "";
                                task.list = list;
                                task.accountName = accountName;
                                if (updateListInMemory(list, task) == false)
                                    task.insert(db);
                                else
                                    bus.post(REFRESH_VIEW);
                                showToast(String.format("task saved in list \"%s\"", items.get(position).title));
                                getActivity().finish();
                            } finally {
                                if (db != null)
                                    db.close();
                            }
                        }
                    });
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setView(dialog)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getActivity().finish();
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    String listName = text.getText().toString().trim();
                                    if (!listName.equals("")) {
                                        SQLiteDatabase db = null;
                                        try {
                                            RemindersDbHelper dbHelper = new RemindersDbHelper(context);
                                            db = dbHelper.getReadableDatabase();
                                            GTaskList list = new GTaskList();
                                            list.title = listName;
                                            list.googleId = "";
                                            list.accountName = accountName;
                                            list.tasks = new ArrayList<GTask>();
                                            GTask task = new GTask();
                                            task.title = sharedText;
                                            task.googleId = "";
                                            task.list = list;
                                            task.accountName = accountName;
                                            if (createListInMemory(list, task) == false) {
                                                list.insert(db);
                                                task.insert(db);
                                            } else
                                                bus.post(REFRESH_VIEW);
                                            getActivity().finish();
                                            showToast(String.format("task saved in list \"%s\"", listName));
                                        } finally {
                                            if (db != null)
                                                db.close();
                                        }
                                    }
                                }
                            });
                    return builder.create();
                }
            };
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            Fragment prev = fragmentManager.findFragmentByTag("ImportTaskFromSharedIntent");
            if (prev != null)
                ft.remove(prev);
            ft.addToBackStack(null);
            ft.commit();
            selectList.show(fragmentManager, "ImportTaskFromSharedIntent");
        }
    }

    private List<ItemList> getListNames(Context context) {
        List<ItemList> names = new ArrayList<ItemList>();
        SharedPreferences prefs =
                context.getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        String accountName = prefs.getString(ListManager.PREF_ACCOUNT_NAME, null);
        if (accountName != null && !accountName.equals("")) {
            RemindersDbHelper dbHelper = new RemindersDbHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    String.format("select %s, %s, %s from %s where %s=\"%s\"",
                            Tables.ID, Tables.TITLE, Tables.DELETED, Tables.LIST_TABLE,
                            Tables.ACCOUNT_NAME, accountName
                    ),
                    null
            );
            try {
                while (cursor.moveToNext()) {
                    boolean notDeleted = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.DELETED)) == 0;
                    if (notDeleted) {
                        ItemList itemList = new ItemList();
                        itemList.title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
                        itemList.id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
                        names.add(itemList);
                    }
                }
            } finally {
                cursor.close();
                db.close();
            }
        }
        return names;
    }
}
