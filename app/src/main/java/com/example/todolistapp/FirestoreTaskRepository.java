package com.example.todolistapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreTaskRepository {

    private static final String COLLECTION = "tasks";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface LoadTasksCallback {
        void onLoaded(List<Task> tasks);
    }

    // Сохранение/обновление задачи
    public static void saveTask(Task task) {
        db.collection(COLLECTION)
                .document(task.getId())
                .set(task);
    }

    // Удаление задачи
    public static void deleteTask(Task task) {
        db.collection(COLLECTION)
                .document(task.getId())
                .delete();
    }

    // Загрузка всех задач из Firestore
    public static void loadTasks(LoadTasksCallback callback) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(query -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot d : query) {
                        Task t = d.toObject(Task.class);
                        tasks.add(t);
                    }
                    callback.onLoaded(tasks);
                });
    }
}
