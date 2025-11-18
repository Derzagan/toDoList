package com.example.todolistapp;

public class Task {

    private String id;
    private String title;
    private long dueDateMillis;
    private boolean completed;

    public Task() { }

    public Task(String id, String title, long dueDateMillis, boolean completed) {
        this.id = id;
        this.title = title;
        this.dueDateMillis = dueDateMillis;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getDueDateMillis() { return dueDateMillis; }
    public void setDueDateMillis(long dueDateMillis) { this.dueDateMillis = dueDateMillis; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isOverdue() {
        return !completed && dueDateMillis < System.currentTimeMillis();
    }
}
