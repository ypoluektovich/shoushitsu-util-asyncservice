package org.shoushitsu.util.asyncservice;

import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FixedLoopingRunnablePool implements AutoCloseable {

	private static final int RUNNING_PHASE = 1;

	private final Runnable onTerminate;

	private final Phaser phaser;

	private final Looper[] loopers;

	/**
	 * @throws java.lang.IllegalArgumentException if {@code threadCount < 1}.
	 */
	public FixedLoopingRunnablePool(Runnable runnable, int threadCount, ThreadFactory threadFactory, Runnable onTerminate) {
		if (threadCount < 1) {
			throw new IllegalArgumentException("thread count is less than 1: " + threadCount);
		}
		this.onTerminate = onTerminate;
		phaser = new Phaser(threadCount + 1);
		loopers = new Looper[threadCount];
		try {
			for (int i = 0; i < threadCount; ++i) {
				Looper looper = loopers[i] = new Looper(phaser, runnable);
				(looper.thread = threadFactory.newThread(looper)).start();
			}
		} catch (Throwable t) {
			phaser.forceTermination();
			throw t;
		}
		phaser.arriveAndAwaitAdvance();
	}

	/**
	 * Not thread safe.
	 */
	@Override
	public final void close() {
		if (phaser.getPhase() != RUNNING_PHASE) {
			return;
		}
		try {
			// this ends RUNNING_PHASE; we wait in case any worker is tardy getting to it
			phaser.awaitAdvanceInterruptibly(phaser.arrive());
		} catch (InterruptedException e) {
			runOnTerminate();
			phaser.forceTermination();
			freeLoopers(true);
			Thread.currentThread().interrupt();
			return;
		}
		if (!runOnTerminate()) {
			freeLoopers(true);
			return;
		}
		try {
			// wait while all workers terminate
			phaser.awaitAdvanceInterruptibly(phaser.arriveAndDeregister());
		} catch (InterruptedException e) {
			freeLoopers(true);
			Thread.currentThread().interrupt();
			return;
		}
		freeLoopers(false);
	}

	/**
	 * Not thread safe.
	 */
	public final void close(long timeout, TimeUnit unit) throws TimeoutException {
		if (timeout < 0 || unit.toNanos(timeout) == Long.MAX_VALUE) {
			throw new IllegalArgumentException("timeout is negative or too large: " + timeout);
		}
		if (phaser.getPhase() != RUNNING_PHASE) {
			return;
		}
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		try {
			// this ends RUNNING_PHASE; we wait in case any worker is tardy getting to it
			phaser.awaitAdvanceInterruptibly(phaser.arrive(), deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			runOnTerminate();
			phaser.forceTermination();
			freeLoopers(true);
			Thread.currentThread().interrupt();
			return;
		} catch (TimeoutException e) {
			runOnTerminate();
			phaser.forceTermination();
			freeLoopers(true);
			return;
		}
		if (!runOnTerminate()) {
			freeLoopers(true);
			return;
		}
		try {
			// wait while all workers terminate
			phaser.awaitAdvanceInterruptibly(phaser.arriveAndDeregister(), deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			freeLoopers(true);
			Thread.currentThread().interrupt();
			return;
		}
		freeLoopers(false);
	}

	private boolean runOnTerminate() {
		if (onTerminate == null) {
			return true;
		}
		try {
			onTerminate.run();
		} catch (Throwable t) {
			return false;
		}
		return true;
	}

	private void freeLoopers(boolean interrupt) {
		for (Looper looper : loopers) {
			if (interrupt) {
				looper.thread.interrupt();
			}
			looper.thread = null;
		}
	}

	private static final class Looper implements Runnable {

		private final Phaser phaser;
		private final Runnable runnable;
		private volatile Thread thread;

		Looper(Phaser phaser, Runnable runnable) {
			this.phaser = phaser;
			this.runnable = runnable;
		}

		@Override
		public final void run() {
			if (phaser.arriveAndAwaitAdvance() < 0) {
				// pool terminated in constructor
				thread = null;
				return;
			}

			// after these arrives get executed by all loopers,
			// only one party will be needed to advance from RUNNING_PHASE
			// (that being the first arrive() call in close(...) methods)
			phaser.arrive();

			while (phaser.getPhase() == RUNNING_PHASE) {
				try {
					runnable.run();
				} catch (Throwable ignore) {
					// all exceptions should be handled by the inner runnable
				}
			}

			phaser.arriveAndDeregister();
		}

	}

}
