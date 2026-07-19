package com.mengzhen.app.scheduler;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化任务信息到 SharedPreferences，设备重启后可恢复
 */
public class TaskStorage {

    private static final String PREFS_NAME = "alarm_tasks";
    private static TaskStorage instance;
    private final Context context;

    private TaskStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized TaskStorage getInstance(Context context) {
        if (instance == null) {
            instance = new TaskStorage(context);
        }
        return instance;
    }

    public void saveTask(TaskInfo task) {
        try {
            JSONArray arr = getAllJsonArray();
            // 先删除同 id 的旧任务
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.getString("taskId").equals(task.taskId)) {
                    filtered.put(obj);
                }
            }
            filtered.put(task.toJson());
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString("tasks", filtered.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeTask(String taskId) {
        try {
            JSONArray arr = getAllJsonArray();
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.getString("taskId").equals(taskId)) {
                    filtered.put(obj);
                }
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString("tasks", filtered.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TaskInfo> getAllTasks() {
        List<TaskInfo> list = new ArrayList<>();
        try {
            JSONArray arr = getAllJsonArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(TaskInfo.fromJson(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public TaskInfo getTask(String taskId) {
        for (TaskInfo task : getAllTasks()) {
            if (task.taskId.equals(taskId)) return task;
        }
        return null;
    }

    private JSONArray getAllJsonArray() {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("tasks", "[]");
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
