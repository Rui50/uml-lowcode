package com.sdmetrics.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sdmetrics.test.Utils;
import com.sdmetrics.util.XMLParser;

public class TestElementFilter {

	// Test-element
	private ModelElement me;

	@Before
	public void createTestModelElement() throws Exception {
		// read metamodel
		MetaModel mm = new MetaModel();
		new XMLParser().parse(Utils.TEST_MODEL_DIR + "testMetaModel03.xml",
				mm.getSAXParserHandler());
		MetaModelElement packageType = mm.getType("package");

		// create test element
		ModelElement test = addTestElement(packageType, "test", null);
		ModelElement pckJava = addTestElement(packageType, "java", test);
		me = addTestElement(packageType, "util", pckJava);
	}

	private ModelElement addTestElement(MetaModelElement type, String name,
			ModelElement owner) {
		ModelElement newElem = new ModelElement(type);
		newElem.setAttribute(MetaModelElement.NAME, name);
		newElem.setRefAttribute(MetaModelElement.CONTEXT, owner);
		return newElem;
	}


	@Test
	public void matches() {
		ElementFilters ef = new ElementFilters(new String[] { "#.java.lang", "#.java.util" });
		assertTrue(ef.matches(me));
		
		ef = new ElementFilters(new String[] { "#.java.util.#" });
		assertFalse(ef.matches(me));
	}

	@Test
	public void emptyFilter() {
		ElementFilters ef = new ElementFilters(new String[0]);
		assertFalse(ef.matches(me));
	}
}
