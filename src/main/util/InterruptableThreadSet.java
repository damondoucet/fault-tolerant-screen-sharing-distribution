package main.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

/**
 * A set of threads that can be started/stopped.
 *
 * Work should be given through Runnables that do a small unit of work and
 * then return. The threads will run them in a loop, checking that it should
 * continue to execute at the end of each iteration.
 */
public class InterruptableThreadSet {
    // The size of this work defines how frequently the thread can check
    // whether it should execute.
    private final List<Runnable> smallUnitsOfWork;

    private final AtomicReference<Exception> error;

    private AtomicBoolean shouldExecute;

    public InterruptableThreadSet(List<Runnable> smallUnitsOfWork,
                                  AtomicReference<Exception> error) {
        this.smallUnitsOfWork = smallUnitsOfWork;
        this.error = error;
        this.shouldExecute = null;
    }

    public void start() {
        checkState(shouldExecute == null,
                "Should not start broadcaster again without stopping first");
        shouldExecute = new AtomicBoolean(true);

        for (Runnable runnable : smallUnitsOfWork)
            new Thread(() -> run(runnable, shouldExecute)).start();
    }

    private void run(Runnable smallUnitOfWork, AtomicBoolean shouldExecute) {
        try {
            while (shouldExecute.get()) {
                smallUnitOfWork.run();
                Util.sleepMillis(10);
            }
        } catch (Exception e) {
            shouldExecute.set(false);
            onException(e);
        }
    }

    private void onException(Exception e) {
        if (error == null) {
            Util.printException(
                    "Exception handler for interruptable thread not set.", e);
        } else
            error.set(e);
    }

    public void stop() {
        // Set the value to false (which the threads should see and stop).
        // We want to use different instances of AtomicBoolean for different
        // sets of threads so that two sets of threads aren't both executing
        // because they're reading true.
        shouldExecute.set(false);
        shouldExecute = null;
    }
}
