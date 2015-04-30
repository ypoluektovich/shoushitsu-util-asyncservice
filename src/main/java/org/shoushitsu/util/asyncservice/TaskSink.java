package org.shoushitsu.util.asyncservice;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BooleanSupplier;

/**
 * <p>Task sinks are used to submit and order computations for asynchronous services.</p>
 *
 * <p>Obtain the instances of this class from the task queue they feed into.
 * If you are implementing a task queue, see
 * {@link org.shoushitsu.util.asyncservice.ATaskQueue#createSink(TaskSinkImplementation)}.</p>
 */
public final class TaskSink {

	private final Lock lock;

	private final BooleanSupplier isRunning;

	private final TaskSinkImplementation implementation;

	private final Condition notFullOrTerminated;

	private final Condition notEmptyOrTerminated;

	TaskSink(
			Lock lock,
			BooleanSupplier isRunning,
			TaskSinkImplementation implementation,
			Condition notFullOrTerminated,
			Condition notEmptyOrTerminated
	) {
		this.lock = lock;
		this.isRunning = isRunning;
		this.implementation = implementation;
		this.notFullOrTerminated = notFullOrTerminated;
		this.notEmptyOrTerminated = notEmptyOrTerminated;
	}

	/**
	 * Submit a computation if there is space in the queue.
	 *
	 * @return {@code false} if the task queue is overflowing and the task can't be processed
	 * (that is, no method on the callback will be invoked), {@code true} otherwise.
	 */
	public final <R> boolean offer(Callable<? extends R> computation, Callback<? super R> callback) {
		lock.lock();
		try {
			if (isRunning.getAsBoolean()) {
				boolean success = implementation.offer(new Task<>(computation, callback));
				if (success) {
					notEmptyOrTerminated.signal();
				}
				return success;
			}
		} finally {
			lock.unlock();
		}
		if (callback != null) {
			callback.terminated();
		}
		return true;
	}

	/**
	 * Submit a computation, waiting for the queue to have space if necessary.
	 *
	 * @throws InterruptedException if interrupted while waiting for the space in the queue to become available.
	 */
	public final <R> void put(Callable<? extends R> computation, Callback<? super R> callback) throws InterruptedException {
		lock.lock();
		try {
			Task<R> task = new Task<>(computation, callback);
			while (isRunning.getAsBoolean()) {
				if (implementation.offer(task)) {
					notEmptyOrTerminated.signal();
					return;
				} else {
					notFullOrTerminated.await();
				}
			}
		} finally {
			lock.unlock();
		}
		if (callback != null) {
			callback.terminated();
		}
	}

}
