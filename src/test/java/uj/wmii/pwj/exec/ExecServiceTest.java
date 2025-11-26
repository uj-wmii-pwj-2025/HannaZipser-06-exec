package uj.wmii.pwj.exec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class ExecServiceTest {
    @Test
    void TestInvokeAll() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        List<Future<String>>results = new ArrayList<>();
        results = s.invokeAll(list, 100, TimeUnit.MILLISECONDS);

        assertTrue(results.get(0).isDone());
        assertEquals("A", results.get(0).get());

        assertTrue(results.get(1).isDone());
        assertEquals("B", results.get(1).get());

        assertTrue(results.get(2).isDone());
        assertEquals("C", results.get(2).get());
    }

    @Test
    void TestInvokeAllException() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5, true));
        list.add(new StringCallable("C", 5));
        List<Future<String>>results = new ArrayList<>();
        results = s.invokeAll(list, 100, TimeUnit.MILLISECONDS);

        assertTrue(results.get(0).isDone());
        assertEquals("A", results.get(0).get());

        assertTrue(results.get(1).isDone());
        assertThrows(ExecutionException.class, results.get(1)::get);

        assertTrue(results.get(2).isDone());
        assertEquals("C", results.get(2).get());

    }

    @Test
    void TestInvokeAllTimeOut() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 1000));
        List<Future<String>> results = new ArrayList<>();
        results = s.invokeAll(list, 500, TimeUnit.MILLISECONDS);

        assertTrue(results.get(0).isDone());
        assertEquals("A", results.get(0).get());

        assertTrue(results.get(1).isDone());
        assertEquals("B", results.get(1).get());

        assertTrue(results.get(2).isCancelled());
    }

    @Test
    void TestInvokeAllTimeOutCancelsAll() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 1000));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        List<Future<String>> results = new ArrayList<>();
        results = s.invokeAll(list, 500, TimeUnit.MILLISECONDS);

        assertTrue(results.get(0).isCancelled());

        assertTrue(results.get(1).isCancelled());

        assertTrue(results.get(2).isCancelled());
    }

    @Test
    void TestInvokeAllNoTimeout() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        List<Future<String>>results = new ArrayList<>();
        results = s.invokeAll(list);

        assertTrue(results.get(0).isDone());
        assertEquals("A", results.get(0).get());

        assertTrue(results.get(1).isDone());
        assertEquals("B", results.get(1).get());

        assertTrue(results.get(2).isDone());
        assertEquals("C", results.get(2).get());
    }

    @Test
    void TestInvokeAllNoTimeoutException() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5, true));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        List<Future<String>>results = new ArrayList<>();
        results = s.invokeAll(list);

        assertTrue(results.get(0).isDone());
        assertThrows(ExecutionException.class, results.get(0)::get);

        assertTrue(results.get(1).isDone());
        assertEquals("B", results.get(1).get());

        assertTrue(results.get(2).isDone());
        assertEquals("C", results.get(2).get());
    }

    @Test
    void TestInvokeAny() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        String result = s.invokeAny(list, 1000, TimeUnit.MILLISECONDS);
        assertEquals("A", result);
    }

    @Test
    void TestInvokeAnyTimeout() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 50));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        assertThrows(TimeoutException.class, () -> {
            s.invokeAny(list, 10, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    void TestInvokeAnyException() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5, true));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        String result = s.invokeAny(list, 1000, TimeUnit.MILLISECONDS);
        assertEquals("B", result);
    }

    @Test
    void TestInvokeAnyOnlyExceptions() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5, true));
        list.add(new StringCallable("B", 5, true));
        list.add(new StringCallable("C", 5, true));
        assertThrows(ExecutionException.class, () -> {
            s.invokeAny(list, 6000, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    void TestInvokeAnyNoTimeOut() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        String result = s.invokeAny(list);
        assertEquals("A", result);
    }

    @Test
    void TestInvokeAnyNoTimeoutException() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5, true));
        list.add(new StringCallable("B", 5));
        list.add(new StringCallable("C", 5));
        String result = s.invokeAny(list);
        assertEquals("B", result);
    }

    @Test
    void TestInvokeAnyNoTimeoutOnlyExceptions() throws Exception {
        MyExecService s = MyExecService.newInstance();
        List<Callable<String>> list = new ArrayList<>();
        list.add(new StringCallable("A", 5, true));
        list.add(new StringCallable("B", 5, true));
        list.add(new StringCallable("C", 5, true));
        assertThrows(ExecutionException.class, () -> {
            s.invokeAny(list);
        });
    }

    @Test
    void testProcessExecutesTasks() throws InterruptedException {
        MyExecService s = MyExecService.newInstance();
        TestRunnable r1 = new TestRunnable();
        TestRunnable r2 = new TestRunnable();

        s.submit(r1);
        s.submit(r2);

        Thread.sleep(50);

        assertTrue(r1.wasRun);
        assertTrue(r2.wasRun);
    }

    @Test
    void testSubmitCallable() throws Exception {
        MyExecService s = MyExecService.newInstance();
        StringCallable c = new StringCallable("Hello", 10);

        Future<String> f = s.submit(c);

        Thread.sleep(50);

        assertTrue(f.isDone());
        assertEquals("Hello", f.get());
    }

    @Test
    void testSubmitRunnableWithResult() throws Exception {
        MyExecService s = MyExecService.newInstance();
        TestRunnable r = new TestRunnable();
        String expected = "OK";

        Future<String> f = s.submit(r, expected);

        Thread.sleep(20);

        assertTrue(r.wasRun);
        assertTrue(f.isDone());
        assertEquals(expected, f.get());
    }

    @Test
    void testSubmitRunnableNoResult() throws Exception {
        MyExecService s = MyExecService.newInstance();
        TestRunnable r = new TestRunnable();

        Future<?> f = s.submit(r);

        Thread.sleep(20);

        assertTrue(r.wasRun);
        assertTrue(f.isDone());
        assertNull(f.get());
    }

    @Test
    void testSubmitRejected() {
        MyExecService s = MyExecService.newInstance();
        s.shutdown();

        assertThrows(RejectedExecutionException.class, () -> {
            s.submit(() -> System.out.println("task"));
        });
    }

    @Test
    void testShutdown() {
        ExecutorService s = MyExecService.newInstance();
        s.execute(new TestRunnable());
        doSleep(10);
        s.shutdown();
        assertThrows(
            RejectedExecutionException.class,
            () -> s.submit(new TestRunnable()));
    }

    @Test
    void testShutdownNowReturnsPendingTasks() throws Exception {
        MyExecService s = MyExecService.newInstance();
        StringCallable taskA = new StringCallable("A", 500);
        StringCallable taskB = new StringCallable("B", 500);
        StringCallable taskC = new StringCallable("C", 500);

        Future<String> fA = s.submit(taskA);
        Future<String> fB = s.submit(taskB);
        Future<String> fC = s.submit(taskC);

        List<Runnable> pending = s.shutdownNow();
        s.awaitTermination(500, TimeUnit.MILLISECONDS);

        assertTrue(s.isTerminated());
        assertTrue(s.isShutdown());

        assertTrue(pending.contains(fB));
        assertTrue(pending.contains(fC));
    }
    @Test
    void shouldExecuteMultipleCommands() {
        MyExecService s = MyExecService.newInstance();

        TestRunnable r1 = new TestRunnable();
        TestRunnable r2 = new TestRunnable();
        TestRunnable r3 = new TestRunnable();

        s.execute(r1);
        s.execute(r2);
        s.execute(r3);

        doSleep(1000);

        assertTrue(r1.wasRun);
        assertTrue(r2.wasRun);
        assertTrue(r3.wasRun);

    }

    @Test
    void testAwaitTermination() throws Exception {
        MyExecService s = MyExecService.newInstance();

        Future<String> f = s.submit(new StringCallable("A", 5));

        s.shutdown();
        boolean terminated = s.awaitTermination(2000, TimeUnit.MILLISECONDS);

        assertTrue(terminated);
        assertTrue(f.isDone());
    }
    @Test
    void testAwaitTerminationTimeout() throws Exception {
        MyExecService s = MyExecService.newInstance();
        s.submit(new StringCallable("A", 2000));

        s.shutdown();
        boolean terminated = s.awaitTermination(100, TimeUnit.MILLISECONDS);

        assertFalse(terminated);
    }


    static void doSleep(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

class StringCallable implements Callable<String> {

    private final String result;
    private final int milis;
    private final boolean shouldThrow;

    StringCallable(String result, int milis) {
        this.result = result;
        this.milis = milis;
        this.shouldThrow = false;
    }

    StringCallable(String result, int milis, boolean shouldThrow) {
        this.result = result;
        this.milis = milis;
        this.shouldThrow = shouldThrow;
    }

    @Override
    public String call() throws Exception {
        if (shouldThrow){
            throw new RuntimeException("Exception");
        }
        ExecServiceTest.doSleep(milis);
        return result;
    }
}
class TestRunnable implements Runnable {

    boolean wasRun;
    @Override
    public void run() {
        wasRun = true;
    }
}
