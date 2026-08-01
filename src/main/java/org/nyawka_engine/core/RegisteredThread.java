package org.nyawka_engine.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;

@MultithreadSystemComponent
public sealed class RegisteredThread permits RegisteredGLCompiler {
    private final Thread thread;
    protected final int id;

    private long currentContext = 0;

    @MultithreadSystemComponent
    protected volatile long lastHeartbeat = System.nanoTime();

    @MultithreadSystemComponent
    protected volatile boolean running = false;

    @MultithreadSystemComponent
    protected final BlockingQueue<GLCommand> commands = new LinkedBlockingQueue<>();

    public RegisteredThread(Runnable task, GLThreadRegistry gtr) {
        this.thread = new Thread(wrapTask(task, gtr));
        this.id = gtr.registerThread(this);
    }

    protected Runnable wrapTask(Runnable task, GLThreadRegistry gtr) {
        return () -> {
            try {
                while (running) {
                    try {
                        while (!commands.isEmpty()) {
                            GLCommand command = commands.poll(1, TimeUnit.MILLISECONDS);

                            if (command != null)
                                command.execute(this);
                        }

                        task.run();

                        lastHeartbeat = System.nanoTime();
                    } catch (Throwable e) {
                        e.printStackTrace();

                        running = false;
                    }
                }
            } finally {
                gtr.removeThread(this);
            }
        };
    }

    @Getter
    public Thread getThread() {
        return thread;
    }

    @Getter
    public int getId() {
        return id;
    }

    public void submit(GLCommand command) {
        commands.add(command);
    }

    public void start() {
        running = true;
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public void setCurrentContext(long id) {
        this.currentContext = id;
    }

    public long getCurrentContext() {
        return this.currentContext;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public boolean isRunning() {
        return running;
    }


    public void requestStop() {
        running = false;
    }
}
