package rkr.simplekeyboard.inputmethod.latin.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DelayedTaskQueue {

    private final Queue<Runnable> taskQueue;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private static final int DELAY_MS = 1000;

    public DelayedTaskQueue() {
        taskQueue = new ConcurrentLinkedQueue<>();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void addTask(Runnable task) {
        // Clear the queue and cancel the previous task if it exists
        taskQueue.clear();
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        // Add the new task and schedule it
        taskQueue.add(task);
        scheduledTask = scheduler.schedule(this::executeNextTask, DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void executeNextTask() {
        Runnable task = taskQueue.poll();
        if (task != null) {
            task.run();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
