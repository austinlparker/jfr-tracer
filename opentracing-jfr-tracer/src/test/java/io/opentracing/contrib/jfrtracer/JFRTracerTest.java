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
package io.opentracing.contrib.jfrtracer;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import oracle.jrockit.jfr.parser.FLREvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("deprecation")
public class JFRTracerTest {

	/**
	 * Test JFR gets the generated span
	 *
	 * @throws java.io.IOException
	 *             on error
	 */
	@Test
	public void basicEvent() throws IOException, InterruptedException {
		Path output = Files.createTempFile("opentracing", ".jfr");

		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = JfrTracerFactory.create(mockTracer);

			// Start JFR
			JFRTestUtils.startJFR();
			// Generate span
			tracer.buildSpan("test span").start().finish();

            // To be removed when test are fixed, it's used due to concurrency issue
			Thread.sleep(100);

			// Stop recording
			List<FLREvent> events = JFRTestUtils.stopJfr(output);

			// To be removed when test are fixed, it's used due to concurrency issue
			Thread.sleep(100);

			// Validate span was created and recorded in JFR
			assertEquals(1, mockTracer.finishedSpans().size());

			Map<String, MockSpan> finishedSpans = mockTracer.finishedSpans().stream()
					.collect(Collectors.toMap(e -> e.operationName(), e -> e));
			assertEquals(finishedSpans.size(), events.size());
			events.stream().forEach(e -> {
				MockSpan finishedSpan = finishedSpans.get(e.getValue("operationName").toString());
				assertNotNull(finishedSpan);
				assertEquals(Long.toString(finishedSpan.context().traceId()), e.getValue("traceId"));
				assertEquals(Long.toString(finishedSpan.context().spanId()), e.getValue("spanId"));
				assertEquals(finishedSpan.operationName(), e.getValue("operationName"));
			});

		} finally {
//			Files.delete(output);
		}
	}

	@Test
	public void noJFR() throws IOException {
		// Setup tracers
		MockTracer mockTracer = new MockTracer();
		Tracer tracer = JfrTracerFactory.create(mockTracer);

		// Generate span
		tracer.buildSpan("test span").start().finish();

		// Validate span was created and recorded in JFR
		assertEquals(1, mockTracer.finishedSpans().size());
	}
}
