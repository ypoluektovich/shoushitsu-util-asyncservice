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
 *     <li>Implement the abstract methods of this class. Synchronization of access to these methods <em>by the
 *     AsynchronousService</em> has been taken care of for you. You should never call these methods directly.</li>
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
	 * <p>Creates a properly synchronized task sink that feeds into this queue.</p>
	 *
	 * @param implementation the underlying implementation of the sink.
	 *
	 * @return a new task sink.
	 */
	protected final TaskSink createSink(TaskSinkImplementation implementation) {
		return new TaskSink(lock, running::get, implementation, notFullOrTerminated, notEmptyOrTerminated);
	}

	final void terminate() {
		lock.lock();
		try {
			running.set(false);
			notEmptyOrTerminated.signalAll();
			notFullOrTerminated.signalAll();
		} finally {
			lock.unlock();
		}
	}

}
