package org.shoushitsu.util.asyncservice;

/**
 * Asynchronous service with no additional features,
 * intended for use when object composition is preferable to inheritance.
 */
public final class DummyAsynchronousService<Q extends ATaskQueue> extends AsynchronousService<Q> {

    /**
     * Publicly accessible version of a corresponding
     * {@linkplain AsynchronousService#AsynchronousService(ATaskQueue, Threading, int) parent constructor}.
     */
    public DummyAsynchronousService(Q taskQueue, Threading threading, int terminationTimeout) {
        super(taskQueue, threading, terminationTimeout);
    }

    /**
     * Close the asynchronous service. This implementation simply calls {@link #closeAsynchronousService()}.
     */
    @Override
    public final void close() {
        closeAsynchronousService();
    }

}
