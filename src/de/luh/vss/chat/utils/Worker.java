/**
 * An abstract class representing a worker that can be run in a separate thread.
 * The worker can be paused, resumed, and safely stopped.
 * It also ensures cleanup by registering a shutdown hook.
 * 
 * <p>This class implements the {@link Runnable} and {@link AutoCloseable} interfaces.</p>
 * 
 */
package de.luh.vss.chat.utils;

import java.io.IOException;

public abstract class Worker implements Runnable, AutoCloseable {
    protected volatile boolean exit = false; // To stop the thread safely
    protected volatile boolean paused = false; // To pause the thread
    protected final String workerName;

    public Worker(String workerName) {
        super();

        this.workerName = workerName;

        // Register a shutdown hook to ensure cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            close();
        }));
    }

    public synchronized void pauseThread() {
        paused = true;
    }

    public synchronized void resumeThread() {
        paused = false;
        notifyAll();
    }

    @Override
    public void run() {

        while (!exit) {
            synchronized (this) {
                while (paused) {
                    try {
                        wait(); // Pause the thread
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            try {
                if (task() != 0)
                    break;
            } catch (InterruptedException e) {
                System.out.printf("Thread %s - interrupted and stopping...\n", this.workerName);
            }
        }

        if (!exit)
            this.close();
        // System.out.printf("Thread %s - exited cleanly.\n", this.workerName);
    }

    abstract public int task() throws InterruptedException;

    abstract protected void refresh() throws IOException;

    abstract protected void clean() throws IOException;

    @Override
    public void close() {
        try {
            this.clean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.exit = true;
    }

}