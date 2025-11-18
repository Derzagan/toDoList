package com.example.todolistapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class add_task extends AppCompatActivity {

    EditText inputTaskTitle;
    Button btnPickDateTime, btnSaveTask;
    TextView textSelectedDate;
    Calendar calendar = Calendar.getInstance();
    boolean dateChosen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputTaskTitle = findViewById(R.id.inputTaskTitle);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        btnPickDateTime = findViewById(R.id.btnPickDate);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        btnPickDateTime.setOnClickListener(v -> showDateTimePicker());

        btnSaveTask.setOnClickListener(v -> {
            String taskName = inputTaskTitle.getText().toString().trim();
            if (taskName.isEmpty()) {
                inputTaskTitle.setError("Введите название задачи");
                return;
            }
            if (!dateChosen) {
                textSelectedDate.setText("Сначала выберите дату и время");
                return;
            }

            long deadlineTime = calendar.getTimeInMillis();

            Intent data = new Intent();
            data.putExtra("title", taskName);
            data.putExtra("deadlineMillis", deadlineTime);
            setResult(RESULT_OK, data);

            finish();
        });
    }

    private void showDateTimePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    calendar.set(Calendar.YEAR, y);
                    calendar.set(Calendar.MONTH, m);
                    calendar.set(Calendar.DAY_OF_MONTH, d);

                    showTimePicker();
                }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, h, m) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, h);
                    calendar.set(Calendar.MINUTE, m);
                    calendar.set(Calendar.SECOND, 0);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    textSelectedDate.setText(sdf.format(calendar.getTime()));
                    dateChosen = true;
                }, hour, minute, true);

        timePickerDialog.show();
    }
}
