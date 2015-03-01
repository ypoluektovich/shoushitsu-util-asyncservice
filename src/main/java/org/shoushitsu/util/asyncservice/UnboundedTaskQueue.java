package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple unbounded task queue with a single {@linkplain #sink sink}.
 */
public final class UnboundedTaskQueue extends ATaskQueue {

	private final Queue<Task<?>> tasks = new LinkedList<>();

	/**
	 * The sink that feeds into this queue.
	 */
	public final TaskSink sink;

	public UnboundedTaskQueue() {
		sink = createSink(new UnboundedQueueSinkImpl(tasks));
	}

	@Override
	protected final boolean isEmpty() {
		return tasks.isEmpty();
	}

	@Override
	protected final Task<?> poll() {
		return tasks.poll();
	}

	@Override
	protected final void doDrainTo(Collection<Task<?>> sink) {
		sink.addAll(tasks);
		tasks.clear();
	}

}
