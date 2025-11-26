package uj.wmii.pwj.exec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class MyExecService implements ExecutorService {
    private final BlockingQueue<Runnable> taskQueue;

    private final Thread processingThread;
    private boolean isShutdown = false;
    private boolean isTerminated = false;

    public MyExecService() {
        this.taskQueue = new LinkedBlockingQueue<>();

        this.processingThread = new Thread(this::process, "MyExecService processing thread");
        this.processingThread.start();
    }

    private void process() {
        try {
            while (!Thread.interrupted()) {
                try {
                    Runnable task = taskQueue.take();
                    task.run();
                    if (isShutdown && taskQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            isTerminated = true;
        }
    }
    static MyExecService newInstance() {
        return new MyExecService();
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;

        List<Runnable> notExecutedTasks = new ArrayList<>();
        taskQueue.drainTo(notExecutedTasks);
        processingThread.interrupt();

        return notExecutedTasks;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long millisTimeout = unit.toMillis(timeout);
        processingThread.join(millisTimeout);
        return !processingThread.isAlive();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (isShutdown) {
            throw new RejectedExecutionException("ExecutorService is shut down");
        }

        FutureTask<T> futureTask = new FutureTask<>(task);
        if (!taskQueue.offer(futureTask)) {
            throw new RejectedExecutionException("Task queue is full");
        }
        return futureTask;

    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (isShutdown) {
            throw new RejectedExecutionException("ExecutorService is shut down");
        }

        FutureTask<T> futureTask = new FutureTask<>(task, result);
        if (!taskQueue.offer(futureTask)) {
            throw new RejectedExecutionException("Task queue is full");
        }
        return futureTask;
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (isShutdown) {
            throw new RejectedExecutionException("ExecutorService is shut down");
        }

        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (!taskQueue.offer(futureTask)) {
            throw new RejectedExecutionException("Task queue is full");
        }

        return futureTask;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException("Tasks collection cannot be null");
        }

        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            try {
                Future<T> future = submit(task);
                futures.add(future);
            } catch (RejectedExecutionException e) {
                for (Future<T> future : futures) {
                    future.cancel(true);
                }
                throw e;
            }
        }

        for (Future<T> future : futures) {
            if (!future.isDone()) {
                try {
                    future.get();
                } catch (ExecutionException | CancellationException e){
                    System.out.println("Task failed with exception: " + e.getCause());
                }
            }
        }

        return futures;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException("Tasks collection cannot be null");
        }

        long timeoutMillis = unit.toMillis(timeout);
        long finish = System.currentTimeMillis() + timeoutMillis;

        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            try {
                Future<T> future = submit(task);
                futures.add(future);
            } catch (RejectedExecutionException e) {
                for (Future<T> future : futures) {
                    future.cancel(true);
                }
                throw e;
            }
        }
        boolean timedOut = false;
        for (Future<T> future : futures) {
            if (!future.isDone()) {
                long remaining = finish - System.currentTimeMillis();

                if (remaining <= 0) {
                    future.cancel(true);
                    timedOut = true;
                    break;
                } else {
                    try {
                        future.get(remaining, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException | CancellationException e) {
                        System.out.println("Task failed with exception: " + e.getCause());
                    } catch (TimeoutException e) {
                        future.cancel(true);
                    }
                }
            }
        }

        if (timedOut){
            for (Future<T> future : futures) {
                future.cancel(true);
            }
        }

        return futures;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (tasks == null) {
            throw new NullPointerException("Tasks collection cannot be null");
        }

        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            Future<T> future = submit(task);
            futures.add(future);
        }

        ExecutionException lastException = null;
        Set<Future<T>> processed = new HashSet<>();
        int exceptionCount = 0;

        try {
            while (true) {
                for (Future<T> future : futures) {
                    if (future.isDone()) {
                        processed.add(future);
                        try {
                            T result = future.get();
                            cancelRemaining(futures, future);
                            return result;
                        } catch (ExecutionException e) {
                            lastException = e;
                            exceptionCount++;
                        } catch (CancellationException e) {
                            exceptionCount++;
                        }
                    }
                }

                if (processed.size() == tasks.size() && exceptionCount == processed.size()) {
                    throw new ExecutionException(lastException);
                }
            }
        } finally {
            cancelRemaining(futures, null);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null) {
            throw new NullPointerException("Tasks collection cannot be null");
        }

        long timeoutMillis = unit.toMillis(timeout);
        long finish = System.currentTimeMillis() + timeoutMillis;

        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            Future<T> future = submit(task);
            futures.add(future);
        }

        ExecutionException lastException = null;
        Set<Future<T>> processed = new HashSet<>();
        int exceptionCount = 0;

        try {
            while (true) {
                for (Future<T> future : futures) {
                    if (future.isDone()) {
                        processed.add(future);
                        try {
                            T result = future.get();
                            cancelRemaining(futures, future);
                            return result;
                        } catch (ExecutionException e) {
                            lastException = e;
                            exceptionCount++;
                        } catch (CancellationException e) {
                            exceptionCount++;
                        }
                    }
                }

                if (processed.size() == tasks.size() && exceptionCount == processed.size()) {
                    throw new ExecutionException(lastException);
                }

                long remaining = finish - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException();
                }
            }

        } finally {
            cancelRemaining(futures, null);
        }
    }

    private <T> void cancelRemaining(List<Future<T>> futures, Future<T> exclude) {
        for (Future<T> f : futures) {
            if (f != exclude) {
                f.cancel(true);
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException("Command cannot be null");
        }

        if (isShutdown) {
            throw new RejectedExecutionException("ExecutorService is shut down");
        }

        if (!taskQueue.offer(command)) {
            throw new RejectedExecutionException("Task queue is full");
        }
    }
}
