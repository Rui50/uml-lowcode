package com.sdmetrics.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestGlossary {

	Glossary entry = new Glossary("Test");

	@Test
	public void toStringNoLocation() {
		assertEquals("glossary entry Test", entry.toString());
	}
}
