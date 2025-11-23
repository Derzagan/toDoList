package com.example.todolistapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Task {

    private String id;
    private String title;
    private long dueDateMillis;
    private boolean completed;
    private String deadlineString;  // <-- новое поле

    public Task() { }

    public Task(String id, String title, long dueDateMillis, boolean completed) {
        this.id = id;
        this.title = title;
        this.dueDateMillis = dueDateMillis;
        this.completed = completed;

        // Генерация строки формата даты
        this.deadlineString = formatMillis(dueDateMillis);
    }

    private String formatMillis(long millis) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date(millis));
    }

    public String getDeadlineString() { return deadlineString; }
    public void setDeadlineString(String deadlineString) { this.deadlineString = deadlineString; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getDueDateMillis() { return dueDateMillis; }
    public void setDueDateMillis(long dueDateMillis) {
        this.dueDateMillis = dueDateMillis;
        this.deadlineString = formatMillis(dueDateMillis); // обновляем строку
    }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isOverdue() {
        return !completed && dueDateMillis < System.currentTimeMillis();
    }
}
