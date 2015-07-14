package org.shoushitsu.util.asyncservice;

import java.util.function.Consumer;

/**
 * <p>Interface for asynchronous service callbacks.</p>
 *
 * <p>You specify a callback when submitting a computation task to an asynchronous service
 * (via a {@link org.shoushitsu.util.asyncservice.TaskSink}).
 * One of the callback methods will be called to report the result of the computation.</p>
 *
 * <p><strong>NB! Mind the thread safety</strong> when writing callbacks! Any of the callback's methods may be called
 * on any thread, including but not limited to a worker thread of the asynchronous service, or a special callback
 * handling thread used by the particular service, or even the thread that submits the computation to the service.</p>
 *
 * @param <R> the type of the result of an asynchronous computation that will be reported to this callback.
 */
public interface Callback<R> {

	/**
	 * Called when a computation completes successfully.
	 *
	 * @param data the result of the computation.
	 */
	void success(R data);

	/**
	 * Called if a computation throws an exception. This includes {@link java.lang.InterruptedException}s,
	 * if the computation supports interrupts (which it should).
	 *
	 * @param exception the exception thrown by the computation.
	 */
	void failure(Throwable exception);

	/**
	 * <p>Called if the asynchronous service was closed before the computation could finish.</p>
	 *
	 * <p>Note that the computation might still be running when this method is invoked.
	 * In such a case, regardless of whether the computation succeeds or fails,
	 * the corresponding callback methods <em>will not</em> be called.</p>
	 */
	void terminated();


	/**
	 * Override this callback's {@linkplain #success(Object) success handler} with the specified consumer.
	 * If {@code onSuccess == null}, the resulting callback will do nothing when success is reported to it
	 * (but will still delegate non-success handling to this callback).
	 *
	 * @implSpec
	 * The default implementation returns a new object that keeps a reference to this callback and uses it
	 * to handle non-success.
	 *
	 * @param onSuccess the new success handler.
	 *
	 * @return a new Callback with the specified success handler and this callback's non-success handlers.
	 *
	 * @since 1.5.0
	 */
	default Callback<R> overrideSuccess(Consumer<? super R> onSuccess) {
		return new Callback<R>() {
			@Override
			public void success(R data) {
				if (onSuccess != null) {
					onSuccess.accept(data);
				}
			}

			@Override
			public void failure(Throwable exception) {
				Callback.this.failure(exception);
			}

			@Override
			public void terminated() {
				Callback.this.terminated();
			}
		};
	}


	/**
	 * A factory method that makes a callback out of three functional objects (for use with lambdas).
	 * Any of the parameters can be {@code null}; that is equivalent to a do-nothing method.
	 *
	 * @param onSuccess will be called if the asynchronous task {@linkplain #success(Object) succeeds}.
	 * @param onFailure will be called if the asynchronous task {@linkplain #failure(Throwable) fails}.
	 * @param onTermination will be called if the asynchronous service is {@linkplain #terminated() terminated}
	 * before the task is finished.
	 *
	 * @param <R> the type of the result of the asynchronous computation.
	 *
	 * @return a callback object that calls one of the provided functions depending on the situation.
	 *
	 * @see #onSuccess(java.util.function.Consumer)
	 * @see #onFailure(java.util.function.Consumer)
	 * @see #onTermination(Runnable)
	 */
	static <R> Callback<R> madeOf(Consumer<R> onSuccess, Consumer<Throwable> onFailure, Runnable onTermination) {
		return new Callback<R>() {
			@Override
			public void success(R data) {
				if (onSuccess != null) {
					onSuccess.accept(data);
				}
			}

			@Override
			public void failure(Throwable exception) {
				if (onFailure != null) {
					onFailure.accept(exception);
				}
			}

			@Override
			public void terminated() {
				if (onTermination != null) {
					onTermination.run();
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public Callback<R> overrideSuccess(Consumer<? super R> onSuccessOverride) {
				return madeOf((Consumer<R>) onSuccessOverride, onFailure, onTermination);
			}
		};
	}

	/**
	 * A factory method that makes a callback that only meaningfully reacts to a successful computation
	 * and does nothing in case of either failure or termination.
	 *
	 * @param onSuccess will be called if the asynchronous task {@linkplain #success(Object) succeeds}.
	 *
	 * @param <R> the type of the result of the asynchronous computation.
	 *
	 * @return a callback object that calls the provided function on computation success.
	 *
	 * @see #madeOf(java.util.function.Consumer, java.util.function.Consumer, Runnable)
	 * @see #onFailure(java.util.function.Consumer)
	 * @see #onTermination(Runnable)
	 */
	static <R> Callback<R> onSuccess(Consumer<R> onSuccess) {
		return madeOf(onSuccess, null, null);
	}

	/**
	 * A factory method that makes a callback that only meaningfully reacts to a failed computation
	 * and does nothing in case of either success or termination.
	 *
	 * @param onFailure will be called if the asynchronous task {@linkplain #failure(Throwable) fails}.
	 *
	 * @param <R> the type of the result of the asynchronous computation.
	 *
	 * @return a callback object that calls the provided function on computation failure.
	 *
	 * @see #madeOf(java.util.function.Consumer, java.util.function.Consumer, Runnable)
	 * @see #onSuccess(java.util.function.Consumer)
	 * @see #onTermination(Runnable)
	 */
	static <R> Callback<R> onFailure(Consumer<Throwable> onFailure) {
		return madeOf(null, onFailure, null);
	}

	/**
	 * A factory method that makes a callback that only meaningfully reacts to
	 * the service being terminated before it can complete the computation
	 * and does nothing in case of either success or failure.
	 *
	 * @param onTermination will be called if the asynchronous service is {@linkplain #terminated() terminated}
	 *
	 * @param <R> the type of the result of the asynchronous computation.
	 *
	 * @return a callback object that calls the provided function on service termination.
	 *
	 * @see #madeOf(java.util.function.Consumer, java.util.function.Consumer, Runnable)
	 * @see #onSuccess(java.util.function.Consumer)
	 * @see #onFailure(java.util.function.Consumer)
	 */
	static <R> Callback<R> onTermination(Runnable onTermination) {
		return madeOf(null, null, onTermination);
	}

}
