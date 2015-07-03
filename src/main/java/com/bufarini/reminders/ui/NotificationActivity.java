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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.model.GTask;

public class NotificationActivity extends Activity {
    private static final long TEN_MINUTES = 10 * 60 * 1000L;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        final Bundle extras = intent.getExtras();
        finish();
        if (extras != null) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String action = intent.getStringExtra("notificaton action");
            long notificationId = intent.getExtras().getLong(NotificationUtils.ID);
            if ("snooze".equals(action)) {
                final String title = extras.getString(NotificationUtils.TITLE);
                final long dueDateInMillis = extras.getLong(NotificationUtils.DUE_DATE);
                notificationManager.cancel(NotificationUtils.getNotificationId(notificationId));
                NotificationUtils.setReminder(
                        getBaseContext(),
                        notificationId,
                        title,
                        dueDateInMillis,
                        extras.getLong(NotificationUtils.LIST_ID),
                        System.currentTimeMillis() + TEN_MINUTES,
                        0
                );
            } else if ("done".equals(action)) {
                GTask task = (GTask) intent.getSerializableExtra("task");
                NotificationUtils.cancelReminder(this, task);
                notificationManager.cancel(NotificationUtils.getNotificationId(notificationId));
                if (task != null) {
                    Intent resultIntent = new Intent(this, Reminders.class);
                    resultIntent.putExtra("action", "refresh list if active");
                    resultIntent.putExtra("task", task);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(resultIntent);

                }
            }
        }
    }
}
