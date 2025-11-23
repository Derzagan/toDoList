package com.example.todolistapp;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskCompletedChanged(Task task, boolean isCompleted);
        void onTaskDelete(Task task);
    }

    private List<Task> tasks;
    private OnTaskActionListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.textTaskTitle.setText(task.getTitle());
        holder.textTaskDate.setText(dateFormat.format(new Date(task.getDueDateMillis())));

        boolean isOverdue = task.isOverdue();
        boolean isCompleted = task.isCompleted();

        // Иконка чекбокса
        holder.iconDone.setImageResource(
                isCompleted ? android.R.drawable.checkbox_on_background
                        : android.R.drawable.checkbox_off_background
        );

        // Стиль текста (зачёркнуто)
        if (isOverdue || isCompleted) {
            holder.textTaskTitle.setPaintFlags(
                    holder.textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
            holder.textTaskTitle.setTextColor(0xFFB0BEC5);
        } else {
            holder.textTaskTitle.setPaintFlags(
                    holder.textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
            holder.textTaskTitle.setTextColor(0xFFFFFFFF);
        }

        // Клик по чекбоксу
        holder.iconDone.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskCompletedChanged(task, !task.isCompleted());
            }
        });

        // Кнопка удаления
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView iconDone;
        TextView textTaskTitle;
        TextView textTaskDate;
        ImageButton btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            iconDone = itemView.findViewById(R.id.iconDone);
            textTaskTitle = itemView.findViewById(R.id.textTaskTitle);
            textTaskDate = itemView.findViewById(R.id.textTaskDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
