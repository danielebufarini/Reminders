package com.bufarini.reminders.collaboration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import com.bufarini.R;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

public class EmailImportDialog extends DialogFragment {
	private GMailDialogListener listener;
	private List<MailEntry> mailEntries;
	private CharSequence[] items;
	private DragSortListView emailsView;
	private EmailAdapter adapter;

	public EmailImportDialog setListFromEmail(List<MailEntry> mailEntries) {
		this.mailEntries = mailEntries;
		items = new String[mailEntries.size()];
		for (int i = 0; i < mailEntries.size(); ++i)
			items[i] = mailEntries.get(i).getSubject();
		return this;
	}
	
	public EmailImportDialog setOnClickListener(GMailDialogListener listener) {
		this.listener = listener;
		return this;
	}
	
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
	    @Override
	    public void remove(int which) {
			mailEntries.remove(which);
			/*adapter = new EmailAdapter(getActivity(), R.layout.email_single_item, mailEntries);
			emailsView.setAdapter(adapter);*/
			adapter.notifyDataSetChanged();
	    }
	};
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View dialog = inflater.inflate(R.layout.email_dialog, null);
		final CheckBox toBeDelete = (CheckBox) dialog.findViewById(R.id.deleteAfterImporting);
		emailsView = (DragSortListView) dialog.findViewById(R.id.emailsList);
		DragSortController controller = new DragSortController(emailsView);
		controller.setDragHandleId(R.id.drag_handle);
	    controller.setRemoveEnabled(true);
	    controller.setRemoveMode(DragSortController.FLING_REMOVE);
		emailsView.setFloatViewManager(controller);
	    emailsView.setOnTouchListener(controller);
	    emailsView.setRemoveListener(onRemove);
	    adapter = new EmailAdapter(getActivity(), R.layout.email_single_item, mailEntries);
	    emailsView.setAdapter(adapter);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(dialog)
			.setTitle(R.string.email_import_dialog)
			.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if (listener != null && mailEntries.size() > 0) {
							List<MailEntry> selectedEmails = new ArrayList<MailEntry>(mailEntries.size());
							for (MailEntry entry: mailEntries)
								if (entry.isToBeImported())
									selectedEmails.add(entry);
							listener.importEmailsAsTasks(toBeDelete.isChecked(), selectedEmails);
						}
					}
				});
		return builder.create();
	}
}
