package org.shoushitsu.util.asyncservice;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Provides description of thread sets to be used by asynchronous services.
 */
public final class Threading {

	/**
	 * Use the specified amount of threads, creating them with the
	 * {@link Thread#Thread(Runnable) Thread(Runnable)} constructor.
	 *
	 * @param threadCount the amount of threads to use.
	 *
	 * @return a threading specification object.

	 * @throws IllegalArgumentException if {@code threadCount} is not positive.
	 */
	public static Threading defaultThreads(int threadCount) {
		return new Threading(threadCount, null);
	}

	/**
	 * Use the specified amount of threads, using the specified pattern to obtain thread names.
	 * Specifically, if {@code index} is the index of a thread to be created
	 * (from {@code 0}, inclusive, to {@code threadCount}, exclusive),
	 * then the name of the new thread will be the result of {@code String.format(namePattern, index)}.
	 * Note that an improper format string may result in {@link java.util.IllegalFormatException}s being thrown
	 * during the service creation, which will most likely abort the start-up of the service.
	 *
	 * @param threadCount the amount of threads to use.
	 * @param namePattern the pattern for thread names, to be passed as the first parameter to
	 * {@link String#format(String, Object...)}.
	 *
	 * @return a threading specification object.

	 * @throws IllegalArgumentException if {@code namePattern} is {@code null} or
	 * if {@code threadCount} is not positive.
	 */
	public static Threading formatThreadNames(int threadCount, String namePattern) {
		if (namePattern == null) {
			throw new IllegalArgumentException("namePattern must be non-null");
		}
		return new Threading(threadCount, index -> String.format(namePattern, index));
	}

	final int threadCount;

	private final IntFunction<String> threadNameByIndex;

	/**
	 * @throws IllegalArgumentException if {@code threadCount} is not positive.
	 */
	private Threading(int threadCount, IntFunction<String> threadNameByIndex) {
		if (threadCount < 1) {
			throw new IllegalArgumentException("thread count must be positive");
		}
		this.threadCount = threadCount;
		this.threadNameByIndex = threadNameByIndex;
	}

	final ThreadFactory createThreadFactory() {
		if (threadNameByIndex == null) {
			return Thread::new;
		}
		AtomicInteger threadIndex = new AtomicInteger();
		return target -> new Thread(target, threadNameByIndex.apply(threadIndex.getAndIncrement()));
	}

}
