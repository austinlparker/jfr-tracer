/*
 * Copyright 2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentracing.contrib.jfrtracer.impl.wrapper;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * A tracer that records context information into the JDK Flight Recorder, making it possible to
 * correlate interesting findings in distributed traces with detailed information in the flight
 * recordings.
 * <p>
 * For scopes and spans the following will be recorded:
 * <ul>
 * <li>Trace Id</li>
 * <li>Span Id</li>
 * <li>Parent Id</li>
 * </ul>
 */
public final class TracerWrapper implements Tracer {

	private final Tracer delegate;
	private final ScopeManagerWrapper scopeManager;

	public TracerWrapper(Tracer delegate) {
		this.delegate = requireNonNull(delegate);
		this.scopeManager = new ScopeManagerWrapper(delegate.scopeManager());
	}

	@Override
	public ScopeManager scopeManager() {
		return scopeManager;
	}

	@Override
	@Deprecated
	public Span activeSpan() {
		Scope active = scopeManager.active();
		return isNull(active) ? null : active.span();
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		return new SpanBuilderWrapper(this, operationName, delegate.buildSpan(operationName));
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		delegate.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return delegate.extract(format, carrier);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Scope activateSpan(Span span) {
		return scopeManager.activate(span, true);
	}
}
