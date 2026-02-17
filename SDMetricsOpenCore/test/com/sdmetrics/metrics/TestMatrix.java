package com.sdmetrics.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sdmetrics.test.MetricTestContext;

public class TestMatrix {

	MetricTestContext mtc;

	@Before
	public void loadTestFixture() throws Exception {
		mtc = MetricTestContext.getStandardContext04();
	}

	@Test
	public void toStringWithLocation() {
		Matrix m = mtc.getMatrix("PackageToEvent");
		assertEquals(
				"relation matrix PackageToEvent [package X event] (line 158)",
				m.toString());
	}

	@Test
	public void toStringNoLocation() {
		Matrix m = new Matrix("Test", mtc.getType("class"),
				mtc.getType("operation"));
		assertEquals("relation matrix Test [class X operation]", m.toString());
	}
}
