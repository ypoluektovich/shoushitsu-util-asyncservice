package org.shoushitsu.util.asyncservice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for asynchronous service.
 *
 * @param <Q> the type of the {@linkplain #queue task queue} this service uses.
 */
public abstract class AsynchronousService<Q extends ATaskQueue> implements AutoCloseable {

	/**
	 * The task queue for this service. Use its {@linkplain org.shoushitsu.util.asyncservice.TaskSink task sinks}
	 * to {@link org.shoushitsu.util.asyncservice.TaskSink#offer(java.util.concurrent.Callable, Callback) offer}
	 * or {@link org.shoushitsu.util.asyncservice.TaskSink#put(java.util.concurrent.Callable, Callback) put}
	 * tasks into the queue.
	 * (The quantity, meaning, and how to obtain the sinks is specified by the task queue implementation.)
	 *
	 * @see org.shoushitsu.util.asyncservice.UnboundedTaskQueue
	 */
	public final Q queue;

	/**
	 * How long to wait for the workers to finish processing the queue
	 * when {@linkplain #close() closing} the service, in milliseconds.
	 */
	private final int terminationTimeout;

	private final FixedLoopingRunnablePool workers;

	/**
	 * Create a new asynchronous service.
	 *
	 * @param taskQueue the queue to take tasks from.
	 * @param threading specifies how many threads to use and how to create them.
	 * @param terminationTimeout how long to wait for the workers to complete pending tasks
	 * when {@linkplain #close() closing} the service.
	 */
	protected AsynchronousService(Q taskQueue, Threading threading, int terminationTimeout) {
		queue = taskQueue;
		this.terminationTimeout = terminationTimeout;
		workers = new FixedLoopingRunnablePool(
				new WorkerIteration(taskQueue),
				threading.threadCount,
				threading.createThreadFactory(),
				() -> taskQueue.terminate()
		);
	}

	/**
	 * Create a new asynchronous service.
	 *
	 * @param taskQueue the queue to take tasks from.
	 * @param threadCount how many worker threads this service will have.
	 * @param terminationTimeout how long to wait for the workers to complete pending tasks
	 * when {@linkplain #close() closing} the service.
	 *
	 * @deprecated in favor of the {@linkplain #AsynchronousService(ATaskQueue, Threading, int) constructor
	 * with Threading specifier}. This constructor may be removed in a future major release.
	 */
	@Deprecated
	protected AsynchronousService(Q taskQueue, int threadCount, int terminationTimeout) {
		this(taskQueue, Threading.defaultThreads(threadCount), terminationTimeout);
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

	/**
	 * Close this service.
	 *
	 * <strong>Note:</strong> when overriding this method, don't forget to call {@link #closeAsynchronousService()}!

	 * @throws Exception this implementation doesn't actually throw anything. The throws clause is to allow
	 * service implementations to throw checked exceptions.
	 */
	@Override
	public void close() throws Exception {
		closeAsynchronousService();
	}

	/**
	 * <p>Closes the service, waiting some time for the workers to complete.</p>
	 *
	 * <p>When this method is invoked, new tasks stop being accepted into the {@linkplain #queue}.
	 * Then the service waits until all workers finish processing the leftover tasks in the queue and terminate,
	 * or until the {@linkplain #terminationTimeout termination timeout} occurs. Once any of that happens,
	 * this method returns.</p>
	 *
	 * <p>If the invoking thread is interrupted while waiting for the workers, the {@link InterruptedException} is
	 * suppressed and the {@linkplain Thread#isInterrupted() interrupted status} is set to {@code true}.</p>
	 */
	protected final void closeAsynchronousService() {
		try {
			workers.close(terminationTimeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException ignore) {
			List<Task<?>> incompleteTasks = new ArrayList<>();
			queue.drainTo(incompleteTasks);
			incompleteTasks.forEach(Task::terminate);
		}
	}

}
