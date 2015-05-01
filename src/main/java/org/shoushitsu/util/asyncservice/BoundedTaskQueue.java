package org.shoushitsu.util.asyncservice;

import java.util.Collection;

/**
 * A simple bounded task queue with a single {@linkplain #sink sink}.
 */
public final class BoundedTaskQueue extends ATaskQueue {

	private final BoundedArrayQueue tasks;

	/**
	 * The sink that feeds into this queue.
	 */
	public final TaskSink sink;

	/**
	 * Create a bounded task queue.
	 *
	 * @param externalCapacityLog2 binary logarithm of the desired maximum queue size.
	 *
	 * @throws java.lang.IllegalArgumentException if {@code externalCapacityLog2 < 0 || externalCapacityLog2 > 30}.
	 */
	public BoundedTaskQueue(int externalCapacityLog2) {
		tasks = new BoundedArrayQueue(externalCapacityLog2);
		sink = createSink(new BoundedArrayQueueSinkImpl(tasks));
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
		sink.addAll(tasks.drain());
	}

}
