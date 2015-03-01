package org.shoushitsu.util.asyncservice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AsynchronousService<Q extends ATaskQueue> implements AutoCloseable {

	public final Q queue;

	private final int terminationTimeout;

	private final FixedLoopingRunnablePool workers;

	protected AsynchronousService(Q taskQueue, int threadCount, int terminationTimeout) {
		queue = taskQueue;
		this.terminationTimeout = terminationTimeout;
		workers = new FixedLoopingRunnablePool(
				new WorkerIteration(queue),
				threadCount,
				Thread::new,
				() -> queue.terminate()
		);
	}

	private static final class WorkerIteration implements Runnable {

		private final ATaskQueue queue;

		public WorkerIteration(ATaskQueue queue) {
			this.queue = queue;
		}

		@Override
		public void run() {
			Task<?> task;
			try {
				queue.lock.lockInterruptibly();
				try {
					task = queue.takeIfNotTerminated();
				} finally {
					queue.lock.unlock();
				}
			} catch (InterruptedException e) {
				return;
			}
			if (task == null) {
				return;
			}
			task.run();
		}

	}

	@Override
	public void close() throws Exception {
		try {
			workers.close(terminationTimeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException ignore) {
			List<Task<?>> incompleteTasks = new ArrayList<>();
			queue.drainTo(incompleteTasks);
			incompleteTasks.forEach(Task::terminate);
		}
	}

}
