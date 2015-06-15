package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Base class of task queues for
 * {@linkplain org.shoushitsu.util.asyncservice.AsynchronousService asynchronous services}.</p>
 *
 * <p>You may extend this class to provide a special type of queue the behaviour of which suits your needs.
 * To implement your own task queue, you need to do the following:</p>
 *
 * <ul>
 *     <li>Implement the abstract methods of this class and override non-final methods where necessary.
 *     Synchronization of access to these methods <em>by the AsynchronousService</em> has been taken care of for you.
 *     You should never call these methods directly.</li>
 *     <li>Provide {@link org.shoushitsu.util.asyncservice.TaskSink}s using {@link #createSink(TaskSinkImplementation)
 *     createSink()}. Your implementation should document how to obtain the sink(s) for a particular queue instance.</li>
 * </ul>
 */
public abstract class ATaskQueue {

	final ReentrantLock lock = new ReentrantLock();

	private final Condition notFullOrTerminated = lock.newCondition();

	private final Condition notEmptyOrTerminated = lock.newCondition();

	private final AtomicBoolean running = new AtomicBoolean(true);

	final Task<?> takeIfNotTerminated() throws InterruptedException {
		while (isEmpty() && running.get()) {
			notEmptyOrTerminated.await();
		}
		Task<?> task = poll();
		if (task != null) {
			notFullOrTerminated.signal();
		}
		return task;
	}

	/**
	 * Check whether this queue is empty.
	 *
	 * @return {@code true} if the queue is empty, otherwise {@code false}.
	 */
	protected abstract boolean isEmpty();

	/**
	 * Get a task from the head of this queue.
	 *
	 * @return the first task in the queue, or {@code null} if the queue is empty.
	 */
	protected abstract Task<?> poll();

	/**
	 * <p>This method is called after the execution completes of a task
	 * that was previously taken from this queue.</p>
	 *
	 * <p>The ordering of invocation between this method and the computation's callback is unspecified.</p>
	 *
	 * @apiNote This method was introduced to allow the implementation of
	 * {@link org.shoushitsu.util.asyncservice.SplittingTaskQueue},
	 * which provides an example of a non-trivial implementation.
	 *
	 * @implNote This implementation simply returns false.
	 *
	 * @param task the task from this queue that was completed.
	 *
	 * @return {@code true} if the state of the queue may have changed after this invocation,
	 * {@code false} otherwise.
	 */
	protected boolean afterCallback(Task<?> task) {
		return false;
	}

	final void drainTo(Collection<Task<?>> sink) {
		lock.lock();
		try {
			doDrainTo(sink);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Put all the tasks in this queue into the provided collection, and clear this queue.
	 *
	 * @param sink the collection to drain the tasks to.
	 */
	protected abstract void doDrainTo(Collection<Task<?>> sink);

	/**
	 * Run the specified action with the queue synchronization lock being held.
	 *
	 * @param action the action to run.
	 */
	protected final void doWithLock(Runnable action) {
		lock.lock();
		try {
			action.run();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * <p>Creates a properly synchronized task sink that feeds into this queue.</p>
	 *
	 * @param implementation the underlying implementation of the sink.
	 *
	 * @return a new task sink.
	 */
	protected final TaskSink createSink(TaskSinkImplementation implementation) {
		return new TaskSink(lock, running::get, implementation, notFullOrTerminated, notEmptyOrTerminated);
	}

	final void signalAll() {
		notEmptyOrTerminated.signalAll();
		notFullOrTerminated.signalAll();
	}

	final void terminate() {
		lock.lock();
		try {
			running.set(false);
			signalAll();
		} finally {
			lock.unlock();
		}
	}

}
