package org.shoushitsu.util.asyncservice;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

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
		if (exception != null) {
			callback.failure(exception);
		} else {
			callback.success(result);
		}
	}

	final void terminate() {
		if (completed.compareAndSet(false, true)) {
			callback.terminated();
		}
	}

}
