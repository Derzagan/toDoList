package com.example.todolistapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
            }
        }
    }

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
