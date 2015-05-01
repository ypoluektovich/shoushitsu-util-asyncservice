package org.shoushitsu.util.asyncservice;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Objects of this class encapsulate submitted computations.</p>

 * @param <R> the type of the encapsulated computation's result.
 */
public final class Task<R> {

	private final Callable<? extends R> computation;
	private final Callback<? super R> callback;
	private final AtomicBoolean completed = new AtomicBoolean();

	Task(Callable<? extends R> computation, Callback<? super R> callback) {
		this.computation = computation;
		this.callback = callback;
	}

	/**
	 * <p>Get the enclosed computation functor.</p>
	 *
	 * <p>This method is intended for task queues that handle different kinds of computations differently.
	 * See, for example, {@link org.shoushitsu.util.asyncservice.SplittingTaskQueue}.</p>
	 *
	 * @return the computation.
	 */
	public final Callable<? extends R> getComputation() {
		return computation;
	}

	final void run() {
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
