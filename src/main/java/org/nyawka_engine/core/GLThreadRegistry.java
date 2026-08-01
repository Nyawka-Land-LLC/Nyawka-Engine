package org.nyawka_engine.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

public final class GLThreadRegistry {
    private static final int MAX_SHADER_PER_TICK = 64;

    private final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    @MultithreadSystemComponent
    private final AtomicInteger pointerToID = new AtomicInteger(0);

    @MultithreadSystemComponent
    private final Map<Thread, RegisteredThread> registeredThreads = new ConcurrentHashMap<>(MAX_THREADS);

    @MultithreadSystemComponent
    private final Map<Integer, RegisteredThread> idToRegisteredThread = new ConcurrentHashMap<>(MAX_THREADS);

    private final RegisteredThreadCellState[] registeredThreadCellStates = new RegisteredThreadCellState[MAX_THREADS];

    private final long contextID;

    private final long TIMEOUT_NANOS = 40_000_000_000L; // 40 seconds

    private final long LIFECYCLE_TIMEOUT_NANOS = 60_000_000_000L; // 60 seconds

    @MultithreadSystemComponent
    private final AtomicLong lastLifecycleHeartbeat = new AtomicLong(System.nanoTime());

    @MultithreadSystemComponent
    private volatile boolean running = false;

    @MultithreadSystemComponent
    private volatile int pointerToThreadCompiler = 0;

    private final BlockingQueue<ShaderProgram> shaderStack = new LinkedBlockingQueue<>();

    private final HashSet<ShaderProgram> consumed = new LinkedHashSet<>();

    @MultithreadSystemComponent
    private final Thread lifecycle = new Thread(() -> {
        try {
            while (running) {

                // Lifecycle сам жив
                lastLifecycleHeartbeat.set(System.nanoTime());

                long dtime = System.nanoTime() - lastLifecycleHeartbeat.get();

                if (dtime > LIFECYCLE_TIMEOUT_NANOS) {

                    if (shaderStack.isEmpty()) {
                        new Thread(
                            GLThreadRegistry.this::shutdown,
                            "GLThreadRegistry shutdown"
                        ).start();
                    } else {
                        new Thread(
                            GLThreadRegistry.this::restart,
                            "GLThreadRegistry restart"
                        ).start();
                    }

                    break;
                }


                /*
                * Проверяем зарегистрированные потоки
                */
                for (Map.Entry<Thread, RegisteredThread> entry :
                        registeredThreads.entrySet()) {

                    Thread thread = entry.getKey();
                    RegisteredThread registered = entry.getValue();


                    if (!registered.isRunning()) {
                        registeredThreads.remove(thread, registered);
                        continue;
                    }


                    long heartbeatAge =
                            System.nanoTime()
                            - registered.getLastHeartbeat();


                    if (heartbeatAge > TIMEOUT_NANOS) {

                        registered.requestStop();

                        registeredThreads.remove(thread, registered);
                    }
                }


                /*
                * Распределяем shader jobs
                *
                * Ограничиваем количество задач,
                * чтобы lifecycle не зависал навсегда
                */
                int processedShaders = 0;

                while (!shaderStack.isEmpty()
                        && processedShaders < MAX_SHADER_PER_TICK) {

                    ShaderProgram shader = shaderStack.poll();

                    if (shader == null)
                        break;


                    RegisteredGLCompiler compiler = null;


                    /*
                    * Ищем следующий доступный compiler thread
                    */
                    for (int i = 0; i < MAX_THREADS; i++) {

                        int id = pointerToThreadCompiler;

                        pointerToThreadCompiler =
                                (pointerToThreadCompiler + 1)
                                % MAX_THREADS;


                        RegisteredThread thread =
                                idToRegisteredThread.get(id);


                        if (thread instanceof RegisteredGLCompiler glCompiler
                                && thread.isRunning()) {
                            System.out.println("compiler set to " + i);
                            compiler = glCompiler;
                            break;
                        }
                    }


                    /*
                    * Нет свободного compiler-а:
                    * возвращаем shader обратно
                    */
                    if (compiler == null) {
                        shaderStack.add(shader);
                        break;
                    }


                    System.out.println("Submitted");
                    compiler.submitShader(shader);

                    processedShaders++;
                }


                Thread.sleep(16);
            }


        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println(
                "GLThreadRegistry lifecycle interrupted"
            );


        } finally {


            /*
            * Мягкая остановка всех потоков
            */
            for (RegisteredThread registered :
                    registeredThreads.values()) {

                registered.requestStop();

                Thread thread = registered.getThread();

                if (thread != null) {
                    thread.interrupt();
                }
            }


            registeredThreads.clear();
            idToRegisteredThread.clear();
        }

    }, "GLThreadRegistry lifecycle");

    public GLThreadRegistry() {
        for (int i = 0; i < MAX_THREADS; i++) {
            registeredThreadCellStates[i] = new RegisteredThreadCellState();
        }

        glfwInit();
        glfwMakeContextCurrent(0);
        contextID = glfwCreateWindow(1, 1, "", 0, 0);
        GLThreshold.bindContext(contextID);
        init();
    }

    private void init() {
        for (int i = 0; i < MAX_THREADS; i++) {
            RegisteredThread thread = new RegisteredGLCompiler(() -> {
                // No-op task for the GL compiler thread
            }, this);
            thread.start();
            
        }

        running = true;
        lifecycle.start();
    }

    public void restart() {
        shutdown();
        clear();
        init();
    }

    public void shutdown() {
        running = false;
        lifecycle.interrupt();
    }

    private void clear() {
        for (Map.Entry<Thread, RegisteredThread> entry : registeredThreads.entrySet()) {
            removeThread(entry.getValue());
        }
    }

    public boolean noThreads() {
        return registeredThreads.isEmpty();
    }

    public int registerThread(RegisteredThread thread) {
        if (pointerToID.get() == -1) {
            throw new IllegalStateException(
                "No available thread slots in GLThreadRegistry"
            );
        }
        registeredThreads.put(thread.getThread(), thread);
        int init = pointerToID.get();
        idToRegisteredThread.put(init, thread);
        registeredThreadCellStates[init].setTaken(true);
        boolean flag = false;
        for (int i = 0; i < MAX_THREADS; i++) {
            if (!registeredThreadCellStates[i].isTaken()) {
                pointerToID.set(i);
                flag = true;
                break;
            }
        }
        if (!flag) {
            pointerToID.set(-1);
        }
        return init;
    }

    public int removeThread(RegisteredThread thread) {
        detachThread(thread);
        forgetThread(thread);
        putThePointerOnThePlaceOfTheRemovedThread(thread);
        return thread.getId();
    }

    public void detachThread(RegisteredThread thread) {
        // removing thread from registeredThreads and idToRegisteredThread
        registeredThreads.remove(thread.getThread());
        idToRegisteredThread.remove(thread.getId());
    }

    public void forgetThread(RegisteredThread thread) {
        // removing thread from states
        registeredThreadCellStates[thread.getId()].setTaken(false);
        registeredThreadCellStates[thread.getId()].setHasGLContext(false);
    }

    public void putThePointerOnThePlaceOfTheRemovedThread(RegisteredThread thread) {
        // new thread will be registered in the place of the removed thread
        pointerToID.set(thread.getId());
    }

    public long getSharedGLContext() {
        return contextID;
    }

    public boolean hasGLContext(Thread thread) {
        RegisteredThread registeredThread = getThreadFromRegistry(thread);
        return obtainGLContextStateFromThread(registeredThread);
    }

    public RegisteredThread getThreadFromRegistry(Thread thread) {
        // returns the RegisteredThread associated with the given Thread
        return registeredThreads.get(thread);
    }

    public boolean isThreadRegistered(RegisteredThread thread) {
        if (thread == null) return false;
        return true;
    }

    public boolean obtainGLContextStateFromThread(RegisteredThread thread) {
        if (!isThreadRegistered(thread)) {
            return false;
        }
        return registeredThreadCellStates[thread.getId()].hasGLContext();
    }
    
    public void consumeShader(ShaderProgram shader) {
        if (!consumed.contains(shader)) {
            System.out.println("SHADER CONSUMED");
            shaderStack.offer(shader);
            consumed.add(shader);
        }
    }

    public void shareContext(RegisteredThread thread) {
        registeredThreadCellStates[thread.getId()].setHasGLContext(true);
    }
}
