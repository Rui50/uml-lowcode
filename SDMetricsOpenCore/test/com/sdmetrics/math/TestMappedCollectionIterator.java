package com.sdmetrics.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

public class TestMappedCollectionIterator {

	@Test
	public void emptySet() {
		Map<Long, Collection<String>> map = new HashMap<>();
		MappedCollectionsIterator<String> it = new MappedCollectionsIterator<>(
				map);
		assertFalse(it.hasNext());
	}

	@Test
	public void iteration() {
		Map<String, Collection<String>> map = new LinkedHashMap<>();
		map.put("1", new ArrayList<String>());
		map.put("2", Arrays.asList("a", "b"));
		map.put("3", new ArrayList<String>());
		map.put("4", Arrays.asList("c", "d"));
		map.put("5", new ArrayList<String>());

		MappedCollectionsIterator<String> it = new MappedCollectionsIterator<>(
				map);
		assertEquals("a", it.next());
		assertEquals("b", it.next());
		assertEquals("c", it.next());
		assertEquals("d", it.next());
		assertFalse(it.hasNext());
		assertFalse(it.hasNext());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void removeNotSupported() {
		Map<Long, Collection<String>> map = new HashMap<>();
		map.put(Long.valueOf(1), Arrays.asList("one"));
		MappedCollectionsIterator<String> it = new MappedCollectionsIterator<>(
				map);
		it.remove();
	}

	@Test(expected = NoSuchElementException.class)
	public void runPastLastElement() {
		Map<Long, Collection<String>> map = new HashMap<>();
		map.put(Long.valueOf(1), Arrays.asList("one"));
		MappedCollectionsIterator<String> it = new MappedCollectionsIterator<>(
				map);
		assertEquals("one", it.next());
		it.next();
	}
}
