package org.shoushitsu.util.asyncservice;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <strong>This is an internal class.</strong>
 * This class is made public only for the purpose of implementing user-defined task queues.
 * It may be made non-public in a future major release.
 * Do not depend on any parts of its implementation, and do not call any methods on instances of this class
 * (outside of what is required for the normal operation of a task queue you're implementing — things like
 * {@code equals()} or {@code hashCode()} or {@code toString()}).
 */
public final class Task<R> implements Runnable {

	private final Callable<? extends R> computation;
	private final Callback<? super R> callback;
	private final AtomicBoolean completed = new AtomicBoolean();

	Task(Callable<? extends R> computation, Callback<? super R> callback) {
		this.computation = computation;
		this.callback = callback;
	}

	@Override
	public final void run() {
		R result = null;
		Throwable exception = null;
		try {
			result = computation.call();
		} catch (Throwable t) {
			exception = t;
		}
		if (completed.getAndSet(true)) {
			return;
		}
		if (callback != null) {
			if (exception != null) {
				callback.failure(exception);
			} else {
				callback.success(result);
			}
		}
	}

	final void terminate() {
		if (completed.compareAndSet(false, true) && callback != null) {
			callback.terminated();
		}
	}

}
