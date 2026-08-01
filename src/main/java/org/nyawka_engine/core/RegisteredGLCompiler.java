package org.nyawka_engine.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@MultithreadSystemComponent
public final class RegisteredGLCompiler extends RegisteredThread {

    @MultithreadSystemComponent
    private final BlockingQueue<ShaderProgram> shadersToCompile = new LinkedBlockingQueue<>();

    public RegisteredGLCompiler(Runnable task, GLThreadRegistry gtr) {
        super(task, gtr);
    }

    @Override
    protected Runnable wrapTask(Runnable task, GLThreadRegistry gtr) {
        return () -> {
            long worker = GLThreshold.bindContext(gtr.getSharedGLContext());

            setCurrentContext(worker);

            try {
                while (running) {
                    try {
                        while (!commands.isEmpty()) {
                            GLCommand command = commands.poll(1, TimeUnit.MILLISECONDS);

                            if (command != null)
                                command.execute(this);
                        }
                        if (!shadersToCompile.isEmpty()) {;
                            ShaderProgram shader = shadersToCompile.poll();
                            if (shader.compile(gtr)) {
                                this.submitShader(shader);
                            };
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

    public void submitShader(ShaderProgram shader) {
        shadersToCompile.offer(shader);
    }
}
