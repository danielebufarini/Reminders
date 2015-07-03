package com.bufarini.reminders.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.bufarini.R;
import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.MemoryStore;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.ui.taskdetail.TaskDetailDialog;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class InsertTextFragment extends Fragment
{
	public static final String SET_ACCOUNT_NAME = "setAccountName";
	private static final String EMPTY_STRING = "";
	private static final String LOGTAG = InsertTextFragment.class.getName();
	private static final Bus bus = BusProvider.getInstance();
	private String selectedAccountName;
	private AlertDialog dialog;
	private SpeechRecognizer sr;
	private EditText addItemEditText;
	private InputMethodManager imm;

    public InsertTextFragment() {
        bus.register(this);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Subscribe
	public void setSelectedAccountName(String[] args) {
		if (SET_ACCOUNT_NAME.equals(args[0]))
			selectedAccountName = args[1];
	}
	
	public static class InsertTaskEvent {
		private Context context;
		private GTask task;
		
		public InsertTaskEvent(Context context, GTask task) {
			super();
			this.context = context;
			this.task = task;
		}
		public Context getContext() {
			return context;
		}
		public GTask getTask() {
			return task;
		}
	}

    private GTask createEmptyTask() {
		GTask task = new GTask();
        task.title = "";
        task.googleId = "";
		task.accountName = selectedAccountName;
		task.notes = EMPTY_STRING;
        task.priority = 0;
        task.level = 0;
        task.updated = System.currentTimeMillis();
        task.isDeleted = false;
        return task;
    }

	private void insertNewTask(String newItem) {
        GTask task = createEmptyTask();
        task.title = newItem;
		bus.post(new InsertTaskEvent(getActivity(), task));
	}

	private void addNewTask(EditText addItemEditText) {
		String newItemText = addItemEditText.getText().toString();
		if (newItemText != null && !"".equals(newItemText)) {
			insertNewTask(newItemText);
			addItemEditText.setText("");
		} else {
            TaskDetailDialog taskDetailDialog = new TaskDetailDialog();
            GTask task = createEmptyTask();
            task.list = MemoryStore.getInstance().getActiveList();
            if (task.list != null) {
                if (task.list.tasks == null)
                    task.list.tasks = new ArrayList<GTask>(30);
                task.list.tasks.add(task);
                taskDetailDialog.task = task;
                taskDetailDialog.show(getFragmentManager(), "TaskDetail");
            }
        }
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.insert_text_fragment, container, false);
		addItemEditText = (EditText) view.findViewById(R.id.addItemEditText);
		addItemEditText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN)
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						imm.hideSoftInputFromWindow(addItemEditText.getWindowToken(), 0);
						addNewTask(addItemEditText);
						return true;
					}
				return false;
			}
		});

		final ImageButton addButton = (ImageButton) view.findViewById(R.id.insertItemButton);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				imm.hideSoftInputFromWindow(addItemEditText.getWindowToken(), 0);
				addNewTask(addItemEditText);
			}
		});

		final ImageButton voiceButton = (ImageButton) view.findViewById(R.id.insertItemVoiceButton);
		final String className = this.getClass().getName();
		voiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.speakNow).create();
				dialog.show();
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, className);
				intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
				sr.startListening(intent);
			}
		});
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		sr = SpeechRecognizer.createSpeechRecognizer(activity);
		sr.setRecognitionListener(new VoiceRecognitionListener());
		imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
	}
	
	class VoiceRecognitionListener implements RecognitionListener {
		public void onReadyForSpeech(Bundle params) {
			// Log.d(LOGTAG, "onReadyForSpeech");
		}

		public void onBeginningOfSpeech() {
			// Log.d(LOGTAG, "onBeginningOfSpeech");
		}

		public void onRmsChanged(float rmsdB) {
			// Log.d(LOGTAG, "onRmsChanged");
		}

		public void onBufferReceived(byte[] buffer) {
			// Log.d(LOGTAG, "onBufferReceived");
		}

		public void onEndOfSpeech() {
			// Log.d(LOGTAG, "onEndofSpeech");
		}

		public void onError(int error) {
			Log.d(LOGTAG, "error " + error);
			dialog.dismiss();
			//addItemEditText.setText("error " + error);
		}

		public void onResults(Bundle results) {
			sr.stopListening();
			String str = new String();
			Log.d(LOGTAG, "onResults " + results);
			ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			for (int i = 0; i < data.size(); i++) {
				Log.d(LOGTAG, "result '" + data.get(i) + "'");
				str += data.get(i);
			}
			dialog.dismiss();
			insertNewTask(data.get(0));
		}

		public void onPartialResults(Bundle partialResults) {
			// Log.d(LOGTAG, "onPartialResults");
		}

		public void onEvent(int eventType, Bundle params) {
			// Log.d(LOGTAG, "onEvent " + eventType);
		}
	}
}
