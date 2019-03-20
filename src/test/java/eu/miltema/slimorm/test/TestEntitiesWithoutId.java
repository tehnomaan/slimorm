package eu.miltema.slimorm.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.*;

public class TestEntitiesWithoutId extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testInsert() throws Exception {
		deleteAll();
		EntityWithoutId e = new EntityWithoutId();
		e.firstName = "John";
		e.count = 7;
		db.insert(e);
		List<EntityWithoutId> list = db.listAll(EntityWithoutId.class);
		assertEquals(1, list.size());
		e = list.get(0);
		assertEquals("John", e.firstName);
		assertEquals(7, e.count.intValue());
	}
	
	@Test
	public void testBulkInsert() throws Exception {
		deleteAll();
		db.bulkInsert(Stream.of(new EntityWithoutId("Mary", 3), new EntityWithoutId("Ann", null)).collect(toList()));
		assertEquals(2, db.listAll(EntityWithoutId.class).size());
	}

	@Test
	public void testWhere() throws Exception {
		deleteAll();
		db.bulkInsert(Stream.of(new EntityWithoutId("Mary", 3), new EntityWithoutId("Marco", 1), new EntityWithoutId("Ann", 5)).collect(toList()));
		assertEquals(2, db.where("name LIKE ?", "Ma%").list(EntityWithoutId.class).size());
	}
}
