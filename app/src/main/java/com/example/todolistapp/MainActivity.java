package com.example.todolistapp;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_TASK = 1001;
    private static final int REQUEST_NOTIF_PERMISSION = 2001;

    FloatingActionButton fabAddTask;
    RecyclerView recyclerViewTasks;
    TaskAdapter taskAdapter;

    ImageButton btnFilterDate;
    TextView textSelectedFilterDate;
    TextView textClearFilter;

    private final List<Task> allTasks = new ArrayList<>();
    private Long filterStartOfDay = null;
    private Long filterEndOfDay = null;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable overdueChecker = new Runnable() {
        @Override
        public void run() {
            applyFilter();
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fabAddTask = findViewById(R.id.fabAddTask);
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        btnFilterDate = findViewById(R.id.btnFilterDate);
        textSelectedFilterDate = findViewById(R.id.textSelectedFilterDate);
        textClearFilter = findViewById(R.id.textClearFilter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(new ArrayList<>());
        recyclerViewTasks.setAdapter(taskAdapter);

        loadTasks();

        taskAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskCompletedChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                FirestoreTaskRepository.saveTask(task);
                applyFilter();
            }

            @Override
            public void onTaskDelete(Task task) {
                allTasks.remove(task);
                FirestoreTaskRepository.deleteTask(task);
                applyFilter();
            }
        });

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, add_task.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
        });

        btnFilterDate.setOnClickListener(v -> openDatePicker());
        textClearFilter.setOnClickListener(v -> clearFilter());

        createNotificationChannels();
        requestNotificationPermissionIfNeeded();
    }

    // ---------- FIRESTORE LOAD ----------
    private void loadTasks() {
        FirestoreTaskRepository.loadTasks(tasks -> {
            allTasks.clear();
            allTasks.addAll(tasks);
            applyFilter();
        });
    }

    // ---------- ADD TASK ----------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_TASK && resultCode == RESULT_OK && data != null) {

            String title = data.getStringExtra("title");
            long deadlineMillis = data.getLongExtra("deadlineMillis", -1);

            if (title != null && deadlineMillis > 0) {

                Task task = new Task(
                        String.valueOf(System.currentTimeMillis()),
                        title,
                        deadlineMillis,
                        false
                );

                allTasks.add(task);
                FirestoreTaskRepository.saveTask(task);
                applyFilter();

                scheduleNotifications(task);
            }
        }
    }

    // -------------------------------------
    //     УВЕДОМЛЕНИЯ
    // -------------------------------------

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel soon = new NotificationChannel(
                    "todo_soon",
                    "Напоминание",
                    NotificationManager.IMPORTANCE_HIGH
            );
            soon.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.malovrem), attrs);

            NotificationChannel deadline = new NotificationChannel(
                    "todo_deadline",
                    "Дедлайн",
                    NotificationManager.IMPORTANCE_HIGH
            );
            deadline.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.opazdal), attrs);

            manager.createNotificationChannel(soon);
            manager.createNotificationChannel(deadline);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF_PERMISSION
                );
            }
        }
    }

    // ---------- ALARMS ----------
    private void scheduleNotifications(Task task) {
        long now = System.currentTimeMillis();
        long due = task.getDueDateMillis();

        if (due <= now) return;

        long soon = due - 30 * 60 * 1000;

        if (soon > now)
            scheduleAlarm(soon, task, DeadlineSoonReceiver.class, 0);

        scheduleAlarm(due, task, DeadlineReachedReceiver.class, 1);
    }

    private void scheduleAlarm(long time, Task task, Class<?> cls, int type) {
        Intent i = new Intent(this, cls);
        i.putExtra("taskName", task.getTitle());

        int code = (task.getId() + "_" + type).hashCode();

        PendingIntent pi = PendingIntent.getBroadcast(
                this, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC_WAKEUP, time, pi);
    }

    // ---------- FILTER ----------
    private void openDatePicker() {
        final Calendar c = Calendar.getInstance();

        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, y, m, d) -> {

                    Calendar cal = Calendar.getInstance();
                    cal.set(y, m, d, 0, 0);

                    filterStartOfDay = cal.getTimeInMillis();

                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);

                    filterEndOfDay = cal.getTimeInMillis();

                    textSelectedFilterDate.setText(d + "." + (m + 1) + "." + y);
                    textClearFilter.setVisibility(TextView.VISIBLE);

                    applyFilter();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dlg.show();
    }

    private void clearFilter() {
        filterStartOfDay = null;
        filterEndOfDay = null;
        textSelectedFilterDate.setText("Все задачи");
        textClearFilter.setVisibility(TextView.GONE);
        applyFilter();
    }

    private void applyFilter() {
        List<Task> filtered = new ArrayList<>();

        for (Task t : allTasks) {

            if (filterStartOfDay == null) {
                filtered.add(t);
                continue;
            }

            long time = t.getDueDateMillis();

            if (time >= filterStartOfDay && time <= filterEndOfDay)
                filtered.add(t);
        }

        taskAdapter.setTasks(filtered);
    }
}
