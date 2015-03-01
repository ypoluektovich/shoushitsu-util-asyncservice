package org.shoushitsu.util.asyncservice;

import java.util.function.Consumer;

public interface Callback<R> {

	void success(R data);

	void failure(Throwable exception);

	void terminated();


	static <R> Callback<R> madeOf(
			Consumer<R> onSuccess,
			Consumer<Throwable> onFailure,
			Runnable onTermination
	) {
		return new Callback<R>() {
			@Override
			public void success(R data) {
				onSuccess.accept(data);
			}

			@Override
			public void failure(Throwable exception) {
				onFailure.accept(exception);
			}

			@Override
			public void terminated() {
				onTermination.run();
			}
		};
	}

}
