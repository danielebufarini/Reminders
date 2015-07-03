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

import static android.util.Patterns.EMAIL_ADDRESS;
import static com.bufarini.reminders.ui.InsertTextFragment.SET_ACCOUNT_NAME;
import static com.bufarini.reminders.ui.Reminders.SHOW_CONTENT;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.DateUtils;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.collaboration.ContactInfo;
import com.bufarini.reminders.collaboration.DeleteEmailViaImap;
import com.bufarini.reminders.collaboration.EmailImportDialog;
import com.bufarini.reminders.collaboration.FilterableContactsAdapter;
import com.bufarini.reminders.collaboration.GMailDialogListener;
import com.bufarini.reminders.collaboration.GMailImapReader;
import com.bufarini.reminders.collaboration.MailEntry;
import com.bufarini.reminders.collaboration.ShareByEmail;
import com.bufarini.reminders.event.RefreshView;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.ui.Reminders;
import com.bufarini.reminders.ui.TasksFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class ListManager extends Fragment implements GMailDialogListener {
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_LIST_SELECTED_ITEM = "listSelectedItem";
    public static final String PREF_PREMIUM = "premium";
    public static final Collection<String> TASKS_SCOPES;

    static {
        TASKS_SCOPES = new ArrayList<String>(1);
        TASKS_SCOPES.add(TasksScopes.TASKS);
    }

    private static final String PREF_SYNC_GTASKS = "syncGTaks", PREF_COLLABORATION = "collaboration";
    private static final String NO_ACCOUNT_SETUP = "<no account set up>";
    private static final String PREF_SHOW_COLLABORATION = "showCollaborationDialog";
    private static final String PREF_UNIQUE_LONG_ID = "uniqueLongId";
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int CHANGED_SYNCHRONISATION_STATUS = 1;
    private static final int CHANGED_GOOGLE_ACCOUNT = 2;
    private static final String LOGTAG = ListManager.class.getSimpleName();

    private final Bus bus = BusProvider.getInstance();

    private final MemoryStore memory;
    private final List<GTaskList> deletedLists;
    private int selectedListPosition;
    private Account[] accounts;
    private String selectedAccountName;
    private ListView listsView;
    private EditText newListText;
    private ImageButton addListButton;
    private GTasklistAdapter adapter;
    private volatile boolean isSyncWithGTasksEnabled;
    private boolean isSyncSwitchEnabled, isCollaborationEnabled, showCollaborationDialog, isPremium;
    private InputMethodManager imm;

    public int numAsyncTasks;

    public ListManager() {
        bus.register(this);
        memory = MemoryStore.getInstance();
        deletedLists = new ArrayList<GTaskList>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public static GTaskList newGTaskList(String title) {
        GTaskList list = new GTaskList();
        list.title = title;
        list.googleId = "";
        list.accountName = MemoryStore.getInstance().getAccountName(); //selectedAccountName;
        list.updated = System.currentTimeMillis();
        list.tasks = new ArrayList<GTask>();
        return list;
    }

    public void setTaskLists(List<GTaskList> lists) {
		if (lists.size() == 0)
			lists.add(newGTaskList("to do"));
        deletedLists.clear();
        for (GTaskList list : lists)
            if (list.isDeleted)
                deletedLists.add(list);
        for (GTaskList list : deletedLists)
            lists.remove(list);
        memory.setActiveLists(lists);
        refreshView();
    }

    @Subscribe
    public void refreshView(RefreshView event) {
        refreshView();
    }

    public void refreshView() {
        Log.d(LOGTAG, "refreshView :: activelists.size() = '" + memory.getActiveLists().size() + "'");
        final Activity activity = getActivity();
        if (activity != null) {
            adapter = new GTasklistAdapter(activity,
                    android.R.layout.simple_list_item_1, memory.getActiveLists());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listsView.setAdapter(adapter);
                }
            });
            if (adapter.getCount() > 0) {
                if (selectedListPosition >= adapter.getCount())
                    selectedListPosition = 0;
                adapter.setSelectedItem(selectedListPosition);
                GTaskList list = adapter.getItem(selectedListPosition);
                memory.setActiveList(list);
                bus.post(new Object[] {TasksFragment.SETUP_LIST, list, activity});
                bus.post(new String[] {Reminders.CHANGE_TITLE, list.title});
            }
        }
    }

    private List<GTaskList> getActiveAndDeletedLists() {
        List<GTaskList> lists = new ArrayList<GTaskList>(memory.getActiveLists().size() + deletedLists.size());
        lists.addAll(memory.getActiveLists());
        lists.addAll(deletedLists);
        return lists;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isNetworkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (!isNetworkAvailable)
            Toast.makeText(getActivity(), getString(R.string.no_network_connection), Toast.LENGTH_LONG).show();
        return isNetworkAvailable;
    }

    private void syncItems(final LoadItems loadItems, final SaveItems saveItems) {
        final View progressBar = getActivity().findViewById(R.id.title_refresh_progress);
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        final List<GTaskList> items = new ArrayList<GTaskList>(50);
        new Thread(new Runnable() {
            public void run() {
                if (saveItems != null)
                    saveItems.run();
                items.clear();
                items.addAll(loadItems.getItems());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTaskLists(items);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void sync() {
        List<GTaskList> lists = getActiveAndDeletedLists();
        final boolean syncWithGoogle = isSyncWithGTasksEnabled && isNetworkAvailable();
        SaveItems saveItems = new SaveItems(getActivity(), syncWithGoogle, selectedAccountName, lists);
        LoadItems loadItems = new LoadItems(getActivity(), syncWithGoogle, selectedAccountName);
        syncItems(loadItems, saveItems);
    }

    private void loadLists() {
        final boolean syncWithGoogle = isSyncWithGTasksEnabled && isNetworkAvailable();
        LoadItems loadItems = new LoadItems(getActivity(), syncWithGoogle, selectedAccountName);
        syncItems(loadItems, null);
    }

    private void saveLists() {
        List<GTaskList> lists = getActiveAndDeletedLists();
        final boolean syncWithGoogle = isSyncWithGTasksEnabled && isNetworkAvailable();
        new Thread(new SaveItems(getActivity(), syncWithGoogle, selectedAccountName, lists)).start();
    }

    public static final String TASKS_SEPARATOR_IN_EMAIL = "\n";

    private List<GTask> getTasksFromEmail(String emailBody, GTaskList list) {
        String[] strTasks = emailBody.split(TASKS_SEPARATOR_IN_EMAIL);
        List<GTask> tasks = new ArrayList<GTask>(strTasks.length);
        for (String strTask : strTasks) {
            if (strTask.equals(""))
                continue;
            GTask task = new GTask();
            task.parse(strTask);
            task.googleId = "";
            task.accountName = accounts[getAccountIndex()].name;
            task.list = list;
            task.updated = list.updated;
            tasks.add(task);
        }
        return tasks;
    }

    private final BlockingQueue<Long> emailsToBeRemoved = new ArrayBlockingQueue<Long>(30);

    @Override
    public void importEmailsAsTasks(boolean isToBeDeleted, List<MailEntry> mailEntries) {
        Log.d(LOGTAG, "importEmailsAsTasks() begin");
        List<GTaskList> lists = new ArrayList<GTaskList>(mailEntries.size());
        for (MailEntry mailEntry : mailEntries) {
            GTaskList list = new GTaskList();
            list.title = mailEntry.getSubject();
            list.googleId = "";
            list.accountName = selectedAccountName;
            list.updated = System.currentTimeMillis();
            list.tasks = getTasksFromEmail(mailEntry.getBody(), list);
            lists.add(list);
            if (isToBeDeleted)
                try {
                    emailsToBeRemoved.put(mailEntry.getUID());
                } catch (InterruptedException e) {
                    Log.e(LOGTAG, "error removing email from GMail server", e);
                }
        }
        new Thread(new SaveItemsInDbAndSetReminders(getActivity(), lists)).start();
        memory.getActiveLists().addAll(lists);
        adapter = new GTasklistAdapter(getActivity(), android.R.layout.simple_list_item_1, memory.getActiveLists());
        listsView.setAdapter(adapter);
        adapter.setSelectedItem(selectedListPosition);
        adapter.notifyDataSetChanged();
        Log.d(LOGTAG, "importEmailsAsTasks() end");
    }

    private static class GMailHandler extends Handler {
        private final FragmentManager fragmentManager;
        private final WeakReference<GMailDialogListener> listenerRef;

        public GMailHandler(FragmentManager fragmentManager, GMailDialogListener listener) {
            super();
            this.fragmentManager = fragmentManager;
            listenerRef = new WeakReference<GMailDialogListener>(listener);
        }

        public void handleMessage(Message m) {
            Log.d(LOGTAG, "handleMessage() begin");
            ArrayList<MailEntry> entries =
                    (ArrayList<MailEntry>) m.getData().getSerializable("emails");
            if (entries.size() > 0) {
                Log.d(LOGTAG, "handleMessage() entries.size() > 0 -- begin");
                GMailDialogListener listener = listenerRef.get();
                if (listener != null) {
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    Fragment prev = fragmentManager.findFragmentByTag("ChooseListFromEmailDialog");
                    if (prev != null)
                        ft.remove(prev);
                    ft.addToBackStack(null);
                    ft.commit();
                    EmailImportDialog dialog = new EmailImportDialog();
                    dialog.setListFromEmail(entries).setOnClickListener(listener)
                            .show(fragmentManager, "ChooseListFromEmailDialog");
                    Log.d(LOGTAG, "handleMessage() entries.size() > 0 -- end");
                }
            }
        }
    }

    private Thread gmailImapRemover = null;
    private Thread gmailImapReader = null;
    private Thread gmailSmtpWriter = null;
    private static final long NO_DELAY = 0L, FIVE_SECOND_DELAY = 5000L;

    public static class TasksListInfo {
        public List<GTaskList> lists;
        public String recipientEmailAddress;
    }

    private final BlockingQueue<TasksListInfo> emailsToSend = new ArrayBlockingQueue<TasksListInfo>(10);

    private class OnGMailTokenAcquired implements AccountManagerCallback<Bundle> {
        private final long delayInMillis;

        public OnGMailTokenAcquired(long delayInMillis) {
            this.delayInMillis = delayInMillis;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Log.d(LOGTAG, "OnGMailTokenAcquired::run() begin");
                GMailHandler myHandler = new GMailHandler(getFragmentManager(), ListManager.this);
                Bundle bundle = result.getResult();
                final String oAuth2Token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                gmailImapReader = new Thread(new GMailImapReader(delayInMillis,
                        accounts[getAccountIndex()].name, myHandler, oAuth2Token));
                gmailImapReader.start();
                gmailImapRemover = new Thread(new DeleteEmailViaImap(accounts[getAccountIndex()].name,
                        oAuth2Token, emailsToBeRemoved));
                gmailImapRemover.start();
                gmailSmtpWriter = new Thread(new ShareByEmail(accounts[getAccountIndex()].name,
                        oAuth2Token, emailsToSend));
                gmailSmtpWriter.start();
                Log.d(LOGTAG, "OnGMailTokenAcquired::run() end");
            } catch (Exception e) {
                Log.d(LOGTAG, "OnTokenAcquired()", e);
            }
        }
    }

    private void startCollaborationThreads(long delayInMillis) {
        if (isNetworkAvailable()) {
            Log.d(LOGTAG, "startCollaborationThreads() begin");
            AccountManager am = AccountManager.get(getActivity());
            am.getAuthToken(accounts[getAccountIndex()], "oauth2:https://mail.google.com/",
                    null, getActivity(), new OnGMailTokenAcquired(delayInMillis), null);
            Log.d(LOGTAG, "startCollaborationThreads() end");
        }
    }

    private void stopCollaborationThreads() {
        interruptThread(gmailSmtpWriter);
        interruptThread(gmailImapReader);
        interruptThread(gmailImapRemover);
    }

    private void interruptThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SharedPreferences settings = activity.getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        Account[] gAccounts = AccountManager.get(activity).getAccountsByType("com.google");
        if (gAccounts.length == 0) {
            isSyncWithGTasksEnabled = false;
            isSyncSwitchEnabled = false;
            selectedAccountName = NO_ACCOUNT_SETUP;
            accounts = new Account[1];
            accounts[0] = new Account(NO_ACCOUNT_SETUP, "com.danielebufarini");
        } else {
            accounts = new Account[gAccounts.length];
            int i = 0;
            for (Account account : gAccounts)
                if (EMAIL_ADDRESS.matcher(account.name).matches())
                    accounts[i++] = account;
            selectedAccountName = settings.getString(PREF_ACCOUNT_NAME, accounts[0].name);
            isSyncWithGTasksEnabled = settings.getBoolean(PREF_SYNC_GTASKS, false);
            isSyncSwitchEnabled = true;
        }
        isPremium = settings.getBoolean(PREF_PREMIUM, false);
        isCollaborationEnabled = isPremium && settings.getBoolean(PREF_COLLABORATION, false);
        showCollaborationDialog = settings.getBoolean(PREF_SHOW_COLLABORATION, true);
        long initialValue = settings.getLong(PREF_UNIQUE_LONG_ID, Long.MIN_VALUE);
        memory.setAtomicLong(new AtomicLong(initialValue));
        memory.setAccountName(selectedAccountName);
        selectedListPosition = settings.getInt(PREF_LIST_SELECTED_ITEM, 0);
        imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isCollaborationEnabled)
            startCollaborationThreads(FIVE_SECOND_DELAY);
        loadLists();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tasks_manager, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        setupWidgets();
    }

    @Override
    public void onStop() {
        stopCollaborationThreads();
        bus.post(SHOW_CONTENT);
        saveLists();
        SharedPreferences settings = getActivity().getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, selectedAccountName);
        editor.putInt(PREF_LIST_SELECTED_ITEM, selectedListPosition);
        editor.putBoolean(PREF_SYNC_GTASKS, isSyncWithGTasksEnabled);
        editor.putBoolean(PREF_COLLABORATION, isCollaborationEnabled);
        editor.putBoolean(PREF_SHOW_COLLABORATION, showCollaborationDialog);
        editor.putLong(PREF_UNIQUE_LONG_ID, memory.getNextLong());
        editor.apply();
        super.onStop();
    }

    private int getAccountIndex() {
        for (int i = 0; i < accounts.length; ++i)
            if (selectedAccountName.equals(accounts[i].name))
                return i;
        selectedAccountName = accounts[0].name;
        return 0;
    }

    private interface IfAlreadyAuthorised {
        void doAction();
    }

    private void authorise(final String accountName, final int requestCode,
                           final IfAlreadyAuthorised ifAlreadyAuthorised, final ImageButton syncButton) {
        if (!isAccountAuthorised(accountName)) {
            final Activity activity = getActivity();
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(activity, TASKS_SCOPES);
            credential.setSelectedAccountName(accountName);
            final Tasks googleService =
                    new Tasks.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                            .setApplicationName(DateUtils.getAppName(activity)).build();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        googleService.tasklists().list().execute();
                        saveAuthorisationForAccount(accountName);
                        isSyncWithGTasksEnabled = true;
                        ifAlreadyAuthorised.doAction();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                syncButton.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (UserRecoverableAuthIOException userRecoverableException) {
                        startActivityForResult(userRecoverableException.getIntent(), requestCode);
                    } catch (IOException e) {
                        Log.e(LOGTAG, "authorise() :: cannot contact google servers for account +\""
                                + accountName + "\"", e);
                    }
                }
            }).start();
        } else
            ifAlreadyAuthorised.doAction();
    }

    private void switchAccount(final int position) {
        saveLists();
        selectedAccountName = accounts[position].name;
        memory.setAccountName(selectedAccountName);
        selectedListPosition = 0;
        bus.post(new String[] { SET_ACCOUNT_NAME, selectedAccountName });
		loadLists();
    }

    private void showList(int position) {
        selectedListPosition = position;
        adapter.setSelectedItem(position);
        GTaskList list = adapter.getItem(selectedListPosition);
        MemoryStore memoryStore = MemoryStore.getInstance();
        memoryStore.setActiveList(list);
        bus.post(new Object[] { TasksFragment.SETUP_LIST, list, getActivity() });
        bus.post(new String[] { Reminders.CHANGE_TITLE, adapter.getItem(position).title });
        bus.post(SHOW_CONTENT);
    }

    private String[] extractAccountNames() {
        String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accounts.length; ++i)
            accountNames[i] = accounts[i].name;
        return accountNames;
    }

    private void setupOnCloseSlidingMenuListener(final ActionMode actionMode) {
        SlidingMenu slidingMenu = ((Reminders)getActivity()).getSlidingMenu();
        slidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {
            @Override
            public void onClose() {
                actionMode.finish();
                adapter.removeSelections();
                adapter.setSelectedItem(selectedListPosition);
            }
        });
    }

    public List<ContactInfo> getContactsInfo() {
        List<ContactInfo> results = new ArrayList<ContactInfo>();
        HashSet<ContactInfo> contactsHash = new HashSet<ContactInfo>();
        String[] PROJECTION = new String[] { ContactsContract.RawContacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_ID,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Photo.CONTACT_ID };
        String order = "CASE WHEN "
                + ContactsContract.Contacts.DISPLAY_NAME
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + ContactsContract.Contacts.DISPLAY_NAME
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE";
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
        ContentResolver cr = getActivity().getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                PROJECTION, filter, null, order);
        if (cursor.moveToFirst())
            do {
                String name = cursor.getString(1);
                String email = cursor.getString(3);
                ContactInfo info = new ContactInfo(email, name);

                if (contactsHash.add(info))
                    results.add(info);
            } while (cursor.moveToNext());
        cursor.close();

        return results;
    }

    private void setupWidgets() {
        bus.post(new String[] { SET_ACCOUNT_NAME, selectedAccountName });
        if (checkGooglePlayServicesAvailable() == false)
            isSyncWithGTasksEnabled = false;

        final ImageButton syncButton = (ImageButton) getActivity().findViewById(R.id.syncButton);
        syncButton.setVisibility(isSyncWithGTasksEnabled || isCollaborationEnabled ? View.VISIBLE : View.GONE);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSyncWithGTasksEnabled)
                    sync();
                if (isCollaborationEnabled) {
                    stopCollaborationThreads();
                    startCollaborationThreads(NO_DELAY);
                }
            }
        });

        ArrayAdapter<String> names =
                new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, extractAccountNames());
        // new ArrayAdapter<String>(getActivity(),
        // android.R.layout.simple_spinner_item, accountNames);
        names.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner accountName = (Spinner) getActivity().findViewById(R.id.accountName);
        accountName.setAdapter(names);
        accountName.setSelection(getAccountIndex());
        accountName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       final int position, long id) {
                if (!selectedAccountName.equals(accounts[position].name))
                    authorise(accounts[position].name, (10 * position) + CHANGED_GOOGLE_ACCOUNT,
                            new IfAlreadyAuthorised() {
                                @Override
                                public void doAction() {
                                    switchAccount(position);
                                }
                            }, syncButton);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Switch collaborationSwitch = (Switch) getActivity().findViewById(R.id.collaboration_switch);
        collaborationSwitch.setChecked(isCollaborationEnabled);
        collaborationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCollaborationEnabled = isChecked;
                if (isChecked) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    if (showCollaborationDialog) {
                        DialogFragment shareDialog = new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                LayoutInflater inflater = getActivity().getLayoutInflater();
                                View dialog = inflater.inflate(R.layout.about_collaboration, null);
                                WebView webView = (WebView) dialog.findViewById(R.id.webView);
                                webView.loadUrl(getActivity().getResources()
                                        .getString(R.string.about_collaboration_html_path));
                                final CheckBox dontShowDialogAgain = (CheckBox) dialog.findViewById(R.id.dontShowAgain);
                                dontShowDialogAgain.setOnCheckedChangeListener(
                                        new CompoundButton.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                if (isChecked)
                                                    showCollaborationDialog = false;
                                            }
                                        }
                                );
                                builder.setView(dialog).setPositiveButton(android.R.string.ok, null);
                                return builder.create();
                            }
                        };
                        shareDialog.show(getFragmentManager(), "about collaboration");
                    }
                    startCollaborationThreads(NO_DELAY);
                }
                else
                    stopCollaborationThreads();
                syncButton.setVisibility(isSyncWithGTasksEnabled || isCollaborationEnabled ? View.VISIBLE : View.GONE);
            }
        });

        Switch syncSwitch = (Switch) getActivity().findViewById(R.id.gtaskSync);
        if (isSyncSwitchEnabled) {
            syncSwitch.setChecked(isSyncWithGTasksEnabled);
            syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    isSyncWithGTasksEnabled = isChecked && isAccountAuthorised(selectedAccountName);
                    if (isChecked) {
                        authorise(selectedAccountName,
                                CHANGED_SYNCHRONISATION_STATUS,
                                new IfAlreadyAuthorised() {
                                    @Override
                                    public void doAction() {
                                        sync();
                                    }
                                },
                                syncButton);
                    }
                    isSyncWithGTasksEnabled = isChecked && isAccountAuthorised(selectedAccountName);
                    syncButton.setVisibility(isSyncWithGTasksEnabled || isCollaborationEnabled ? View.VISIBLE : View.GONE);
                }
            });
        } else
            syncSwitch.setEnabled(false);

        listsView = (ListView) getActivity().findViewById(R.id.list);
        listsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showList(position);
            }
        });
        listsView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listsView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            private List<GTaskList> selectedLists = new ArrayList<GTaskList>();
            private Menu menu;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
            {
                if (checked) {
                    adapter.setMultipleSelectedItem(position);
                    selectedLists.add(adapter.getItem(position));
                } else {
                    adapter.removeMultipleSelectedItem(position);
                    selectedLists.remove(adapter.getItem(position));
                }
                menu.findItem(R.id.delete_list).setVisible(memory.getActiveLists().size() > 1 ? true : false);
                menu.findItem(R.id.rename_list).setVisible(selectedLists.size() > 1 ? false : true);
                menu.findItem(R.id.merge_list).setVisible(selectedLists.size() > 1 ? true : false);
                menu.findItem(R.id.share_list).setVisible(isCollaborationEnabled);
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                switch (item.getItemId()) {
                    case R.id.merge_list:
                        DialogFragment mergeDialog = new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                LayoutInflater inflater = getActivity().getLayoutInflater();
                                View dialog = inflater.inflate(R.layout.rename_list_dialog, null);
                                final EditText edit = (EditText) dialog.findViewById(R.id.renameListEditText);
                                builder.setView(dialog).setTitle(R.string.asksForNewListName)
                                        .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                String title = edit.getText().toString();
                                                if (!title.equals("")) {
                                                    GTaskList list = new GTaskList();
                                                    list.title = title;
                                                    list.googleId = "";
                                                    list.accountName = selectedAccountName;
                                                    list.updated = System.currentTimeMillis();
                                                    List<GTask> tasks = new ArrayList<GTask>(36);
                                                    for (GTaskList oldList: selectedLists) {
                                                        for (GTask sourceTask: oldList.tasks) {
                                                            GTask task = new GTask(sourceTask);
                                                            task.id = GTask.generateId();
                                                            task.googleId = "";
                                                            task.list = list;
                                                            task.updated = System.currentTimeMillis();
                                                            tasks.add(task);
                                                        }
                                                        oldList.isDeleted = true;
                                                        oldList.updated = System.currentTimeMillis();
                                                        deletedLists.add(oldList);
                                                        memory.getActiveLists().remove(adapter.getPosition(oldList));
                                                        removeTasksReminders(oldList);
                                                    }
                                                    list.tasks = tasks;
                                                    memory.getActiveLists().add(list);
                                                    List<GTaskList> listContainer = new ArrayList<GTaskList>(1);
                                                    listContainer.add(list);
                                                    new Thread(
                                                            new SaveItemsInDbAndSetReminders(getActivity(), listContainer)
                                                    ).start();
                                                    adapter.notifyDataSetChanged();
                                                    showList(adapter.getPosition(memory.getActiveLists().iterator().next()));
                                                    mode.finish();
                                                }
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mode.finish();
                                    }
                                });
                                return builder.create();
                            }
                        };
                        mergeDialog.show(getFragmentManager(), "merge list");
                        return true;
                    case R.id.share_list:
                        DialogFragment shareDialog = new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                LayoutInflater inflater = getActivity().getLayoutInflater();
                                View dialog = inflater.inflate(R.layout.share_dialog, null);
                                final EditText edit = (EditText) dialog.findViewById(R.id.shareEditText);
                                final ListView contacts = (ListView) dialog.findViewById(R.id.shareList);
                                final FilterableContactsAdapter adapter =
                                        new FilterableContactsAdapter(getActivity(), R.layout.filterable_contact, getContactsInfo());
                                edit.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        if (s.length() > 0) {
                                            contacts.setVisibility(View.VISIBLE);
                                            adapter.getFilter().filter(s);
                                        } else
                                            contacts.setVisibility(View.GONE);
                                    }
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                    }
                                });
                                contacts.setAdapter(adapter);
                                contacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        edit.setText(((ContactInfo)adapter.getItem(position)).getEmail());
                                        contacts.setVisibility(View.GONE);
                                    }
                                });
                                builder.setView(dialog).setTitle(R.string.shareWith)
                                        .setPositiveButton(R.string.sendByEmail, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                String recipientEmailAddress = edit.getText().toString();
                                                if (EMAIL_ADDRESS.matcher(recipientEmailAddress).matches()) {
                                                    try {
                                                        TasksListInfo info = new TasksListInfo();
                                                        info.recipientEmailAddress = recipientEmailAddress;
                                                        info.lists = selectedLists;
                                                        emailsToSend.put(info);
                                                        Toast toast = Toast.makeText(getActivity(),
                                                                getString(R.string.listShared) + " " + recipientEmailAddress,
                                                                Toast.LENGTH_LONG);
                                                        toast.show();
                                                    } catch (InterruptedException e) {
                                                        Log.e(LOGTAG, "error sending tasks' list as an email", e);
                                                    }
                                                } else {
                                                    StringBuilder msg = new StringBuilder(300)
                                                            .append(getString(R.string.invalidEmailAddress));
                                                    if (recipientEmailAddress.length() > 0)
                                                        msg.append(' ').append("\"").append(recipientEmailAddress)
                                                                .append("\"");
                                                    Toast toast = Toast.makeText(getActivity(), msg.toString(),
                                                            Toast.LENGTH_LONG);
                                                    toast.show();
                                                }
                                                mode.finish();
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mode.finish();
                                    }
                                });
                                return builder.create();
                            }
                        };
                        shareDialog.show(getFragmentManager(), "share list");
                        return true;
                    case R.id.delete_list:
                        if (selectedLists.size() < adapter.getCount()) {
                            builder.setTitle(R.string.deleteList)
                                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            for (GTaskList list : selectedLists) {
                                                Log.d(LOGTAG, "list = " + list + ", adapter.getPosition(list)='"
                                                        + adapter.getPosition(list) + "'");
                                                list.isDeleted = true;
                                                list.updated = System.currentTimeMillis();
                                                deletedLists.add(list);
                                                memory.getActiveLists().remove(list);
                                                removeTasksReminders(list);
                                            }
										/*adapter = new GTasklistAdapter(getActivity(),
											android.R.layout.simple_list_item_1, memory.getActiveLists());*/
                                            adapter.notifyDataSetChanged();
                                            showList(adapter.getPosition(memory.getActiveLists().iterator().next()));
                                            mode.finish();
                                        }
                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mode.finish();
                                }
                            });
                            if (selectedLists.size() == 1)
                                builder.setMessage(String.format(
                                        getResources().getString(R.string.deleteListMessage), selectedLists.get(0).title));
                            else
                                builder.setMessage(getResources().getString(R.string.deleteSelectedists));
                            builder.create().show();
                        }
                        return true;
                    case R.id.rename_list:
                        final GTaskList list = selectedLists.get(0);
                        DialogFragment renameDialog = new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                LayoutInflater inflater = getActivity().getLayoutInflater();
                                View dialog = inflater.inflate(R.layout.rename_list_dialog, null);
                                final EditText edit = (EditText) dialog.findViewById(R.id.renameListEditText);
                                edit.setText(list.title);
                                builder.setView(dialog).setTitle(R.string.renameTo)
                                        .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                String title = edit.getText().toString();
                                                if (!title.trim().equals("")) {
                                                    renameListTo(title, adapter.getPosition(list));
                                                    mode.finish();
                                                    bus.post(new String[] { Reminders.CHANGE_TITLE, title });
                                                }
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mode.finish();
                                    }
                                });
                                return builder.create();
                            }
                        };
                        renameDialog.show(getFragmentManager(), "rename list");
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                this.menu = menu;
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.list_manager_actionmode, menu);
                ListManager.this.setupOnCloseSlidingMenuListener(mode);
                selectedLists.clear();
                adapter.setSelectedItem(GTasklistAdapter.NO_SELECTED_ITEM);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adapter.removeSelections();
                adapter.setSelectedItem(selectedListPosition);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
        });
        newListText = (EditText) getActivity().findViewById(R.id.newListText);
        addListButton = (ImageButton) getActivity().findViewById(R.id.addListButton);
        addListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String listName = newListText.getText().toString().trim();
                if (!listName.equals("")) {
                    adapter.add(newGTaskList(listName));
                    adapter.notifyDataSetChanged();
                    newListText.setText("");
                }
                imm.hideSoftInputFromWindow(newListText.getWindowToken(), 0);
            }
        });
    }

    private void renameListTo(String title, int position) {
        final GTaskList list = adapter.getItem(position);
        list.title = title;
        list.isModified = true;
        list.updated = System.currentTimeMillis();
        adapter.notifyDataSetChanged();
    }

    private void removeTasksReminders(GTaskList list) {
        for (GTask task : list.tasks)
            if (task.reminderDate > 0 || task.reminderInterval > 0)
                NotificationUtils.cancelReminder(getActivity(), task);
    }

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, getActivity(),
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    private boolean isAccountAuthorised(String accountName) {
        SharedPreferences settings = getActivity().getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        return settings.getBoolean(accountName, false);
    }

    private void saveAuthorisationForAccount(String accountName) {
        SharedPreferences settings = getActivity().getApplicationContext()
                .getSharedPreferences(Reminders.class.getName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(accountName, true);
        editor.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode % 10) {
            case CHANGED_SYNCHRONISATION_STATUS:
                if (resultCode == Reminders.RESULT_OK) {
                    saveAuthorisationForAccount(selectedAccountName);
                    isSyncWithGTasksEnabled = true;
                    sync();
                }
                break;
            case CHANGED_GOOGLE_ACCOUNT:
                if (resultCode == Reminders.RESULT_OK) {
                    int position = requestCode / 10;
                    saveAuthorisationForAccount(accounts[position].name);
                    //isSyncWithGTasksEnabled = true;
                    if (!selectedAccountName.equals(accounts[position].name))
                        switchAccount(position);
                }
                break;
        }
    }
}
