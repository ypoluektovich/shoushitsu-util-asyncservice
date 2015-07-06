package org.shoushitsu.util.asyncservice;

import java.util.function.Consumer;

/**
 * Facility for clutter-free chaining of asynchronous service calls.
 *
 * <p>The starting point of the async chain API is the static {@link #withDefaults(Consumer, Runnable) withDefaults()}
 * method. It provides an empty chain, which you can build off using the instance methods. Finally, when the chain</p>
 *
 * @param <I> the type of the result of the previous invocation in the chain.
 */
public interface AsyncChain<I> {

	/**
	 * Chain an invocation with default non-success callbacks.
	 *
	 * @param computation the invocation to add to the chain.
	 *
	 * @param <O> the type of the specified computation's result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #call(Function, Consumer, Runnable)
	 * @see #call(Supplier)
	 * @see #callAndDiscard(Function)
	 */
	default <O> AsyncChain<O> call(Function<? super I, O> computation) {
		return call(computation, DEFAULT_ON_FAILURE, DEFAULT_ON_TERMINATION);
	}

	/**
	 * Chain an invocation with custom non-success callbacks. Instead of the default non-failure callbacks specified
	 * at the chain's start, the ones specified as parameters to this method will be called in case of
	 * failure and termination respectively.
	 *
	 * @param computation the invocation to add to the chain.
	 * @param onFailure the custom failure callback.
	 * @param onTermination the custom termination callback.
	 *
	 * @param <O> the type of the specified computation's result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #DEFAULT_ON_FAILURE
	 * @see #DEFAULT_ON_TERMINATION
	 * @see #call(Function)
	 * @see #call(Supplier, Consumer, Runnable)
	 * @see #callAndDiscard(Function, Consumer, Runnable)
	 */
	<O> AsyncChain<O> call(Function<? super I, O> computation, Consumer<Throwable> onFailure, Runnable onTermination);

	/**
	 * Convenience method for chaining computations that disregard the previous results.
	 *
	 * @param computation the invocation to add to the chain.
	 *
	 * @param <O> the type of the specified computation's result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #call(Supplier, Consumer, Runnable)
	 * @see #call(Function)
	 * @see #callAndDiscard(Function)
	 */
	default <O> AsyncChain<O> call(Supplier<O> computation) {
		return call(computation, DEFAULT_ON_FAILURE, DEFAULT_ON_TERMINATION);
	}

	/**
	 * Convenience method for chaining computations that disregard the previous results.
	 *
	 * @param computation the invocation to add to the chain.
	 * @param onFailure the custom failure callback.
	 * @param onTermination the custom termination callback.
	 *
	 * @param <O> the type of the specified computation's result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #DEFAULT_ON_FAILURE
	 * @see #DEFAULT_ON_TERMINATION
	 * @see #call(Supplier)
	 * @see #call(Function, Consumer, Runnable)
	 * @see #callAndDiscard(Function, Consumer, Runnable)
	 */
	<O> AsyncChain<O> call(Supplier<O> computation, Consumer<Throwable> onFailure, Runnable onTermination);

	/**
	 * Chain an invocation, but ignore its result, substituting the result of this chain. Effectively chains a
	 * "side-effect" computation, similar to {@link java.util.stream.Stream#peek(Consumer)}.
	 *
	 * @param computation the invocation to add to the chain.
	 *
	 * @param <O> the type of the specified computation's (ignored) result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #callAndDiscard(Function, Consumer, Runnable)
	 * @see #call(Function)
	 * @see #call(Supplier)
	 */
	default <O> AsyncChain<I> callAndDiscard(Function<? super I, O> computation) {
		return callAndDiscard(computation, DEFAULT_ON_FAILURE, DEFAULT_ON_TERMINATION);
	}

	/**
	 * Chain an invocation, but ignore its result, substituting the result of this chain. Effectively chains a
	 * "side-effect" computation, similar to {@link java.util.stream.Stream#peek(Consumer)}.
	 *
	 * @param computation the invocation to add to the chain.
	 * @param onFailure the custom failure callback.
	 * @param onTermination the custom termination callback.
	 *
	 * @param <O> the type of the specified computation's (ignored) result.
	 *
	 * @return the new chain object, which represents this chain with the specified computation appended to the end.
	 *
	 * @see #DEFAULT_ON_FAILURE
	 * @see #DEFAULT_ON_TERMINATION
	 * @see #callAndDiscard(Function)
	 * @see #call(Function, Consumer, Runnable)
	 * @see #call(Supplier, Consumer, Runnable)
	 */
	<O> AsyncChain<I> callAndDiscard(Function<? super I, O> computation, Consumer<Throwable> onFailure, Runnable onTermination);

	/**
	 * Execute this chain, reporting the final result to the specified consumer.
	 *
	 * @param onSuccess the consumer to feed the final result to.
	 */
	void execute(Consumer<? super I> onSuccess);


	/**
	 * Functional interface that encapsulates an invocation of an asynchronous service with one parameter.
	 *
	 * @param <I> the type of the invocation's parameter.
	 * @param <O> the type of the invocation's result.
	 */
	interface Function<I, O> {
		void compute(I input, Callback<? super O> callback) throws Exception;
	}

	/**
	 * Functional interface that encapsulates an invocation of an asynchronous service with no parameters.
	 *
	 * @param <O> the type of the invocation's result.
	 */
	interface Supplier<O> {
		void compute(Callback<? super O> callback) throws Exception;
	}

	/**
	 * Start a new chain with specified default non-success callbacks.
	 *
	 * @param defaultOnFailure the default failure callback for this chain.
	 * @param defaultOnTermination the default termination callback for this chain.
	 *
	 * @return the object that represents the new empty chain.
	 */
	static AsyncChain<Void> withDefaults(Consumer<Throwable> defaultOnFailure, Runnable defaultOnTermination) {
		class ChainImpl<P, I> implements AsyncChain<I> {
			private final ChainImpl<?, P> prevChain;
			private final Function<? super P, I> computation;
			private final Consumer<Throwable> onFailure;
			private final Runnable onTermination;

			ChainImpl(ChainImpl<?, P> prevChain, Function<? super P, I> computation, Consumer<Throwable> onFailure, Runnable onTermination) {
				this.prevChain = prevChain;
				this.computation = computation;
				this.onFailure = onFailure == DEFAULT_ON_FAILURE ? defaultOnFailure : onFailure;
				this.onTermination = onTermination == DEFAULT_ON_TERMINATION ? defaultOnTermination : onTermination;
			}

			@Override
			public <O> AsyncChain<O> call(Function<? super I, O> computation, Consumer<Throwable> onFailure, Runnable onTermination) {
				return new ChainImpl<>(this, computation, onFailure, onTermination);
			}

			@Override
			public <O> AsyncChain<O> call(Supplier<O> computation, Consumer<Throwable> onFailure, Runnable onTermination) {
				return new ChainImpl<>(this, (__, c) -> computation.compute(c), onFailure, onTermination);
			}

			@Override
			public <O> AsyncChain<I> callAndDiscard(Function<? super I, O> computation, Consumer<Throwable> onFailure, Runnable onTermination) {
				return new ChainImpl<>(
						this,
						(i, c) -> computation.compute(
								i,
								Callback.madeOf(
										__ -> c.success(i),
										c::failure,
										c::terminated
								)
						),
						onFailure,
						onTermination
				);
			}

			@Override
			public void execute(Consumer<? super I> onSuccess) {
				execute(Callback.madeOf(
						onSuccess,
						onFailure,
						onTermination
				));
			}

			private void execute(Callback<? super I> callback) {
				if (computation == null) {
					if (callback != null) {
						callback.success(null);
					}
				} else if (prevChain == null) {
					if (callback != null) {
						callback.success(null);
					}
				} else {
					prevChain.execute(new Callback<P>() {
						@Override
						public void success(P data) {
							try {
								computation.compute(data, callback);
							} catch (Exception e) {
								failure(e);
							}
						}

						@Override
						public void failure(Throwable exception) {
							Consumer<Throwable> prevFailure = prevChain.onFailure;
							if (prevFailure != null) {
								prevFailure.accept(exception);
							}
						}

						@Override
						public void terminated() {
							Runnable prevTermination = prevChain.onTermination;
							if (prevTermination != null) {
								prevTermination.run();
							}
						}
					});
				}
			}
		}
		return new ChainImpl<>(null, null, DEFAULT_ON_FAILURE, DEFAULT_ON_TERMINATION);
	}

	/**
	 * A marker for non-success callback overriding methods that disables overriding of failure callback.
	 * Intended to be used if you want to override only the termination callback.
	 */
	Consumer<Throwable> DEFAULT_ON_FAILURE = new Consumer<Throwable>() {
		@Override
		public void accept(Throwable throwable) {
			// no implementation, this thing is just a marker
		}
	};

	/**
	 * A marker for non-success callback overriding methods that disables overriding of termination callback.
	 * Intended to be used if you want to override only the failure callback.
	 */
	Runnable DEFAULT_ON_TERMINATION = new Runnable() {
		@Override
		public void run() {
			// no implementation, this thing is just a marker
		}
	};

}
