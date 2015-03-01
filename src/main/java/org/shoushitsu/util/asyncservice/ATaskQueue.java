package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ATaskQueue {

	protected final ReentrantLock lock = new ReentrantLock();

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

	protected abstract boolean isEmpty();

	protected abstract Task<?> poll();

	protected abstract void drainTo(Collection<Task<?>> sink);

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
