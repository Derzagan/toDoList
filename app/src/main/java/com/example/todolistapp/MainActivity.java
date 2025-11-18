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
            applyFilter();                      // пересчитать isOverdue() и обновить UI
            handler.postDelayed(this, 30_000);  // повторять каждые 30 секунд
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

        taskAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskCompletedChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                applyFilter();
            }

            @Override
            public void onTaskDelete(Task task) {
                allTasks.remove(task);
                applyFilter();
            }
        });

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, add_task.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
        });

        btnFilterDate.setOnClickListener(v -> openDatePicker());
        textClearFilter.setOnClickListener(v -> clearFilter());

        // Каналы уведомлений и разрешение
        createNotificationChannels();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(overdueChecker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(overdueChecker);
    }

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
                applyFilter();

                // после добавления задачи ставим будильники
                scheduleNotifications(task);
            }
        }
    }

    // ---------- КАНАЛЫ УВЕДОМЛЕНИЙ ----------

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            // Канал для напоминания за 30 минут (звук malovrem)
            Uri soonSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.malovrem);
            NotificationChannel channelSoon = new NotificationChannel(
                    "todo_soon",
                    "Напоминание за 30 минут",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelSoon.setDescription("Уведомления за 30 минут до дедлайна");
            channelSoon.setSound(soonSound, audioAttributes);

            // Канал для уведомления о дедлайне (звук opazdal)
            Uri deadlineSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.opazdal);
            NotificationChannel channelDeadline = new NotificationChannel(
                    "todo_deadline",
                    "Уведомление о дедлайне",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelDeadline.setDescription("Уведомления в момент наступления дедлайна");
            channelDeadline.setSound(deadlineSound, audioAttributes);

            manager.createNotificationChannel(channelSoon);
            manager.createNotificationChannel(channelDeadline);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF_PERMISSION);
            }
        }
    }

    // ---------- ПЛАНИРОВАНИЕ УВЕДОМЛЕНИЙ ----------

    private void scheduleNotifications(Task task) {
        long now = System.currentTimeMillis();
        long due = task.getDueDateMillis();

        if (due <= now) {
            // дедлайн уже прошёл — уведомления не ставим
            return;
        }

        // За 30 минут до дедлайна
        long soonTime = due - 30 * 60 * 1000L;
        if (soonTime > now) {
            scheduleAlarm(soonTime, task, DeadlineSoonReceiver.class, 0);
        }

        // В момент дедлайна
        scheduleAlarm(due, task, DeadlineReachedReceiver.class, 1);
    }

    private void scheduleAlarm(long triggerAtMillis,
                               Task task,
                               Class<?> receiverClass,
                               int type) {

        Intent intent = new Intent(this, receiverClass);
        intent.putExtra("taskName", task.getTitle());

        int requestCode = (task.getId() + "_" + type).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            // Без setExact / setExactAndAllowWhileIdle — не нужен SCHEDULE_EXACT_ALARM
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- ФИЛЬТР И СПИСОК ----------

    private void openDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, y);
                    cal.set(Calendar.MONTH, m);
                    cal.set(Calendar.DAY_OF_MONTH, d);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);

                    filterStartOfDay = cal.getTimeInMillis();

                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);

                    filterEndOfDay = cal.getTimeInMillis();

                    String dateText = d + "." + (m + 1) + "." + y;
                    textSelectedFilterDate.setText(dateText);
                    textClearFilter.setVisibility(TextView.VISIBLE);

                    applyFilter();
                },
                year, month, day);

        dialog.show();
    }

    private void clearFilter() {
        filterStartOfDay = null;
        filterEndOfDay = null;
        textSelectedFilterDate.setText("Все задачи");
        textClearFilter.setVisibility(TextView.GONE);
        applyFilter();
    }

    private void applyFilter() {
        List<Task> toShow = new ArrayList<>();

        for (Task task : allTasks) {
            if (filterStartOfDay == null || filterEndOfDay == null) {
                toShow.add(task);
            } else {
                long t = task.getDueDateMillis();
                if (t >= filterStartOfDay && t <= filterEndOfDay) {
                    toShow.add(task);
                }
            }
        }

        taskAdapter.setTasks(toShow);
    }
}
