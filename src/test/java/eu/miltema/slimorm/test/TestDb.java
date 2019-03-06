package eu.miltema.slimorm.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.miltema.slimorm.Db;

public class TestDb {

	@Test
	public void testConstructor() {
		assertTrue(new Db() != null);
	}
}
