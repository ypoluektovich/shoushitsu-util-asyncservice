package org.shoushitsu.util.asyncservice;

public final class BoundedArrayQueueSinkImpl implements TaskSinkImplementation {

	private final BoundedArrayQueue tasks;

	public BoundedArrayQueueSinkImpl(BoundedArrayQueue tasks) {
		this.tasks = tasks;
	}

	@Override
	public final boolean offer(Task<?> task) {
		return tasks.offer(task);
	}

}
