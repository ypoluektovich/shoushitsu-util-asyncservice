package org.shoushitsu.util.asyncservice;

import java.util.Optional;
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
		return new Threading(threadCount, null, null);
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
		return new Threading(threadCount, index -> String.format(namePattern, index), null);
	}

	/**
	 * Use a single thread with the specified name.
	 *
	 * @param name the name of the thread.
	 *
	 * @return a threading specification object.
	 *
	 * @throws IllegalArgumentException if {@code name == null}.
	 */
	public static Threading singleThread(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name must be non-null");
		}
		return new Threading(1, ix -> name, null);
	}

	final int threadCount;

	private final IntFunction<String> threadNameByIndex;

	private final Optional<ClassLoader> contextClassLoader;

	/**
	 * @throws IllegalArgumentException if {@code threadCount} is not positive.
	 */
	private Threading(int threadCount, IntFunction<String> threadNameByIndex, Optional<ClassLoader> contextClassLoader) {
		if (threadCount < 1) {
			throw new IllegalArgumentException("thread count must be positive");
		}
		this.threadCount = threadCount;
		this.threadNameByIndex = threadNameByIndex;
		this.contextClassLoader = contextClassLoader;
	}

	/**
	 * Set a {@linkplain Thread#getContextClassLoader() context class loader} for the threading specification.
	 *
	 * @param classLoader the class loader to use for the created threads. To use the system class loader,
	 * pass {@code null} (it will override the default context class loader setting mechanism).
	 *
	 * @return a threading specification object that has the same specs as this, except for the context class loader.
	 */
	public Threading withContextClassLoader(ClassLoader classLoader) {
		return new Threading(this.threadCount, this.threadNameByIndex, Optional.ofNullable(classLoader));
	}

	final ThreadFactory createThreadFactory() {
		return new SpecThreadFactory();
	}

	private final class SpecThreadFactory implements ThreadFactory {

		private final AtomicInteger threadIndex = new AtomicInteger();

		@Override
		public Thread newThread(Runnable target) {
			Thread thread;
			if (threadNameByIndex == null) {
				thread = new Thread(target);
			} else {
				thread = new Thread(target, threadNameByIndex.apply(threadIndex.getAndIncrement()));
			}
			if (contextClassLoader != null) {
				thread.setContextClassLoader(contextClassLoader.orElse(null));
			}
			return thread;
		}

	}

}
