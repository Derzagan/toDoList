package com.example.todolistapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class DeadlineReachedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("taskName");

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

            Uri soundUri = Uri.parse("android.resource://" +
                    context.getPackageName() + "/" + R.raw.opazdal);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "todo_deadline")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Время вышло!")
                    .setContentText(taskName)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 700, 700, 700})
                    .setSound(soundUri);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
