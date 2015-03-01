package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>A task queue with two "levels": <em>external</em>, which is bounded, and <em>internal</em>, which is unbounded.
 * When taking tasks out of this queue, internal queue has priority over the external one.
 * This task queue is intended for services that may produce recursive computations during their operation ("internally"),
 * yet still want to apply backpressure on "external" users of the service.</p>
 */
public final class TwoLevelTaskQueue extends ATaskQueue {

	private final BoundedArrayQueue externalQueue;

	private final Queue<Task<?>> internalQueue;

	/**
	 * The sink to be used by "external" clients of the asynchronous service.
	 */
	public final TaskSink externalSink;

	/**
	 * The sink to be used by "internal" clients of the asynchronous service.
	 */
	public final TaskSink internalSink;

	public TwoLevelTaskQueue(int externalCapacityLog2) {
		externalQueue = new BoundedArrayQueue(externalCapacityLog2);
		internalQueue = new LinkedList<>();
		externalSink = createSink(new BoundedArrayQueueSinkImpl(externalQueue));
		internalSink = createSink(new UnboundedQueueSinkImpl(internalQueue));
	}

	@Override
	protected final boolean isEmpty() {
		return internalQueue.isEmpty() && externalQueue.isEmpty();
	}

	@Override
	protected final Task<?> poll() {
		if (!internalQueue.isEmpty()) {
			return internalQueue.poll();
		} else if (!externalQueue.isEmpty()) {
			return externalQueue.poll();
		} else {
			return null;
		}
	}

	@Override
	protected final void doDrainTo(Collection<Task<?>> sink) {
		sink.addAll(internalQueue);
		internalQueue.clear();

		sink.addAll(externalQueue.drain());
	}

}
