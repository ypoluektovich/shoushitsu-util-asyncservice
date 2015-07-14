package org.shoushitsu.util.asyncservice;

import org.testng.annotations.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class CallbackOverrideTest {

	@SuppressWarnings("unchecked")
	@Test
	public void defaultMethod() {
		Callback<Object> innerCallback = spy(new Callback<Object>() {
			@Override
			public void success(Object data) {
			}

			@Override
			public void failure(Throwable exception) {
			}

			@Override
			public void terminated() {
			}
		});
		Consumer<Object> overrider = mock(Consumer.class);
		Callback<Object> overriddenCallback = innerCallback.overrideSuccess(overrider);

		Object data = "data";
		overriddenCallback.success(data);

		verify(innerCallback).overrideSuccess(overrider);
		verifyNoMoreInteractions(innerCallback);
		verify(overrider).accept(data);
	}

	@Test
	public void defaultMethodWithNull() {
		Callback<Object> innerCallback = spy(new Callback<Object>() {
			@Override
			public void success(Object data) {
			}

			@Override
			public void failure(Throwable exception) {
			}

			@Override
			public void terminated() {
			}
		});
		Callback<Object> overriddenCallback = innerCallback.overrideSuccess(null);

		Object data = "data";
		overriddenCallback.success(data);

		verify(innerCallback).overrideSuccess(null);
		verifyNoMoreInteractions(innerCallback);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void madeOf() {
		Consumer<Object> innerSuccess = mock(Consumer.class);
		Callback<Object> innerCallback = Callback.madeOf(innerSuccess, null, null);
		Consumer<Object> overrider = mock(Consumer.class);
		Callback<Object> overriddenCallback = innerCallback.overrideSuccess(overrider);

		Object data = "data";
		overriddenCallback.success(data);

		verifyZeroInteractions(innerSuccess);
		verify(overrider).accept(data);
	}

}
