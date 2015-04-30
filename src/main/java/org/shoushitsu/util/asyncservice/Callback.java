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
		};
	}

}
