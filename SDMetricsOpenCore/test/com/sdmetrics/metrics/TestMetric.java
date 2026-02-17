package com.sdmetrics.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sdmetrics.test.MetricTestContext;

public class TestMetric {

	MetricTestContext mtc;

	@Before
	public void loadTestFixture() throws Exception {
		mtc = MetricTestContext.getStandardContext04();
	}

	@Test
	public void toStringWithLocation() {
		Metric m = mtc.getMetric("NumAttr", "class");
		assertEquals("metric NumAttr for elements of type class (line 6)",
				m.toString());
	}

	@Test
	public void toStringNoLocation() {
		Metric m = new Metric("Test", mtc.getType("package"), "testing");
		assertEquals("metric Test for elements of type package", m.toString());
	}
}
