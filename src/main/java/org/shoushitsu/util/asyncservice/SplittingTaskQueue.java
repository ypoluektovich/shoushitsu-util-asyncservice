package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * <p>A task queue that splits tasks into buckets and only allows one computation from each bucket
 * to be executed at a given moment.</p>
 *
 * <p>The function that maps computations to buckets is specified at creation time.
 * When a task is offered for execution, its bucket becomes locked.
 * It is only unlocked after the task's computation completes, whether successfully or with a failure.
 * When a bucket is locked, no tasks that belong to it can be offered for execution;
 * otherwise, tasks are offered in the order they were submitted to the queue.</p>
 *
 * <p>This task queue has {@linkplain #sink only one sink}.</p>
 *
 * @implNote Currently all buckets of this queue are unbounded.
 * A way to determine bounds will be added in a future version.
 */
public final class SplittingTaskQueue extends ATaskQueue {

	private final Function<Callable<?>, ?> splitter;

	private final Queue<Task<?>> tasks = new LinkedList<>();

	/* This set must support null elements. */
	private final HashSet<Object> lockedBuckets = new HashSet<>();

	/**
	 * The sink that feeds into this queue.
	 */
	public final TaskSink sink;

	/**
	 * Creates a fully unbounded {@code SplittingTaskQueue}
	 * that splits tasks into buckets according to the specified {@code splitter}.
	 *
	 * @param splitter the splitter function. May return nulls.
	 * If {@code splitter == null}, all tasks will be put into the same bucket.
	 * The splitter's results must be suitable for use as {@link java.util.Map} keys;
	 * that is, they should be immutable and must correctly implement {@link #equals(Object)} and {@link #hashCode()}.
	 * Calls to the splitter from the asynchronous service will be synchronized properly.
	 * The splitter will be called often with the same arguments;
	 * keep that in mind, as its performance directly affects the performance of the queue and therefore the service.
	 */
	public SplittingTaskQueue(Function<Callable<?>, ?> splitter) {
		this.splitter = splitter == null ? (callable -> null) : splitter;
		sink = createSink(new UnboundedQueueSinkImpl(tasks));
	}

	@Override
	protected final boolean isEmpty() {
		if (tasks.isEmpty()) {
			return true;
		}
		for (Task<?> task : tasks) {
			if (!lockedBuckets.contains(splitter.apply(task.getComputation()))) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected final Task<?> poll() {
		if (tasks.isEmpty()) {
			return null;
		}
		for (Iterator<Task<?>> it = tasks.iterator(); it.hasNext(); ) {
			Task<?> task = it.next();
			Object bucket = splitter.apply(task.getComputation());
			if (!lockedBuckets.contains(bucket)) {
				lockedBuckets.add(bucket);
				it.remove();
				return task;
			}
		}
		return null;
	}

	@Override
	protected final boolean afterCallback(Task<?> task) {
		lockedBuckets.remove(splitter.apply(task.getComputation()));
		return true;
	}

	@Override
	protected final void doDrainTo(Collection<Task<?>> sink) {
		sink.addAll(tasks);
		tasks.clear();
	}

}
