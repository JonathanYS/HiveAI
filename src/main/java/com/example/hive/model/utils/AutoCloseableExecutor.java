package com.example.hive.model.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A helper that wraps an {@link ExecutorService} and shuts it down automatically when closed.
 */
public class AutoCloseableExecutor implements AutoCloseable {
    private final ExecutorService exec;

    /**
     * Creates a new executor with a fixed thread pool of the given size.
     *
     * @param threads the number of threads in the pool
     */
    public AutoCloseableExecutor(int threads) {
        exec = Executors.newFixedThreadPool(threads);
    }

    /**
     * Returns the wrapped {@link ExecutorService} for submitting tasks.
     *
     * @return the executor service
     */
    public ExecutorService service() { return exec; }

    /**
     * Shuts down the executor service when this resource is closed.
     * <p>
     * This method will first call {@link ExecutorService#shutdown()},
     * then wait up to 1 second for tasks to complete. If tasks are still
     * running after the timeout, it will invoke {@link ExecutorService#shutdownNow()}.
     * If the current thread is interrupted while waiting, it will also
     * call {@link ExecutorService#shutdownNow()} and re-interrupt the thread.
     * </p>
     */
    @Override
    public void close() {
        exec.shutdown();
        try {
            // wait up to 1 second for tasks to finish.
            boolean terminated = exec.awaitTermination(1, TimeUnit.SECONDS);
            if (!terminated) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ie) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
