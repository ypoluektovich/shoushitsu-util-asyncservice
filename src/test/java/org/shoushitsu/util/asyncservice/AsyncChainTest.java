package org.shoushitsu.util.asyncservice;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AsyncChainTest {

	static class Service {
		void asyncCompute(Object input, Callback<? super Integer> callback) {
			int output;
			if (input == null) {
				output = 1;
			} else if (input instanceof Integer) {
				output = ((Integer) input) + 1;
			} else {
				throw new IllegalArgumentException(input.toString());
			}
			callback.success(output);
		}

		void asyncCompute(Object input, Object dummy, Callback<? super Integer> callback) {
			asyncCompute(input, callback);
		}

		void asyncFailure(Object input, Callback<? super Integer> callback) {
			callback.failure(null);
		}
	}

	@Mock private Consumer<Throwable> defaultOnFailure;
	@Mock private Runnable defaultOnTermination;
	@Mock private Consumer<Throwable> customOnFailure;
	@Mock private Runnable customOnTermination;
	@Mock private Consumer<Object> onSuccess;
	@Spy private Service service;
	private InOrder inOrder;

	@BeforeMethod
	public void setUpMocks() {
		MockitoAnnotations.initMocks(this);
		inOrder = Mockito.inOrder(service);
	}

	private void verifyShort(Object input) {
		inOrder.verify(service).asyncCompute(eq(input), any());
	}

	private void verifyLong(Object input, Object dummy) {
		inOrder.verify(service).asyncCompute(eq(input), eq(dummy), any());
		verifyShort(input);
	}

	private void verifySuccess(Object expectedResult) {
		inOrder.verifyNoMoreInteractions();
		verify(onSuccess).accept(expectedResult);
		verifyZeroInteractions(defaultOnFailure, defaultOnTermination, customOnFailure, customOnTermination);
	}

	private void verifyFailure(Boolean custom) {
		inOrder.verify(service).asyncFailure(any(), any());
		inOrder.verifyNoMoreInteractions();
		if (custom == null) {
			verifyZeroInteractions(onSuccess, defaultOnFailure, defaultOnTermination, customOnFailure, customOnTermination);
		} else if (custom) {
			verify(customOnFailure).accept(any());
			verifyZeroInteractions(onSuccess, defaultOnFailure, defaultOnTermination, customOnTermination);
		} else {
			verify(defaultOnFailure).accept(any());
			verifyZeroInteractions(onSuccess, defaultOnTermination, customOnFailure, customOnTermination);
		}
	}

	@Test
	public void chain0() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.execute(onSuccess);

		verifySuccess(null);
	}

	@Test
	public void chain1Function() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(service::asyncCompute)
				.execute(onSuccess);

		verifyShort(null);
		verifySuccess(1);
	}

	@Test
	public void chain1Supplier() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.execute(onSuccess);

		verifyShort(null);
		verifySuccess(1);
	}

	@Test
	public void chain2() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call(service::asyncCompute)
				.execute(onSuccess);

		verifyShort(null);
		verifyShort(1);
		verifySuccess(2);
	}

	@Test
	public void chain3() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call((i, c) -> service.asyncCompute(i, 1, c))
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyLong(1, 1);
		verifyLong(2, 2);
		verifySuccess(3);
	}

	@Test
	public void chain3Discard1() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.callAndDiscard(service::asyncCompute)
				.call((i, c) -> service.asyncCompute(i, 1, c))
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyLong(null, 1);
		verifyLong(1, 2);
		verifySuccess(2);
	}

	@Test
	public void chain3Discard2() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.callAndDiscard((i, c) -> service.asyncCompute(i, 1, c))
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyLong(1, 1);
		verifyLong(1, 2);
		verifySuccess(2);
	}

	@Test
	public void chain3Discard3() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call((i, c) -> service.asyncCompute(i, 1, c))
				.callAndDiscard((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyLong(1, 1);
		verifyLong(2, 2);
		verifySuccess(2);
	}

	@Test
	public void fail1() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncFailure(null, c))
				.call((i, c) -> service.asyncCompute(i, 1, c))
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyFailure(false);
	}

	@Test
	public void fail2() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call((i, c) -> service.asyncFailure(i, c))
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyFailure(false);
	}

	@Test
	public void fail3() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call((i, c) -> service.asyncCompute(i, 1, c))
				.call((i, c) -> service.asyncFailure(i, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyLong(1, 1);
		verifyFailure(false);
	}

	@Test
	public void fail1Custom() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call((i, c) -> service.asyncFailure(i, c), customOnFailure, customOnTermination)
				.execute(onSuccess);

		verifyFailure(true);
	}

	@Test
	public void fail1Null() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call((i, c) -> service.asyncFailure(i, c), null, customOnTermination)
				.execute(onSuccess);

		verifyFailure(null);
	}

	@Test
	public void fail2Custom() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call(c -> service.asyncCompute(null, c))
				.call((i, c) -> service.asyncFailure(i, c), customOnFailure, customOnTermination)
				.call((i, c) -> service.asyncCompute(i, 2, c))
				.execute(onSuccess);

		verifyShort(null);
		verifyFailure(true);
	}

	@Test
	public void failExplicitDefault() {
		AsyncChain.withDefaults(defaultOnFailure, defaultOnTermination)
				.call((i, c) -> service.asyncFailure(i, c), AsyncChain.DEFAULT_ON_FAILURE, customOnTermination)
				.execute(onSuccess);

		verifyFailure(false);
	}

}
