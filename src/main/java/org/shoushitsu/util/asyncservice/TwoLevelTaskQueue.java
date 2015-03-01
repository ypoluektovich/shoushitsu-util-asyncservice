package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public final class TwoLevelTaskQueue extends ATaskQueue {

	private final BoundedArrayQueue externalQueue;

	private final Queue<Task<?>> internalQueue;

	public final TaskSink externalSink;

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
	protected final void drainTo(Collection<Task<?>> sink) {
		lock.lock();
		try {
			sink.addAll(internalQueue);
			internalQueue.clear();

			sink.addAll(externalQueue.drain());
		} finally {
			lock.unlock();
		}
	}

}
