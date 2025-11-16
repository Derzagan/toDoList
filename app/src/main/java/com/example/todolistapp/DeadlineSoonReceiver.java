package com.example.todolistapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class DeadlineSoonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("taskName");

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "todo_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("До дедлайна осталось 30 минут")
                    .setContentText(taskName)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0,500,500,500});

            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
