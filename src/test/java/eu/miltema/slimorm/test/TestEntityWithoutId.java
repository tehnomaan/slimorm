package eu.miltema.slimorm.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.*;

public class TestEntityWithoutId extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testInsert() throws Exception {
		deleteAll();
		SlimTestEntityWithoutId e = new SlimTestEntityWithoutId();
		e.firstName = "John";
		e.count = 7;
		db.insert(e);
		List<SlimTestEntityWithoutId> list = db.listAll(SlimTestEntityWithoutId.class);
		assertEquals(1, list.size());
		e = list.get(0);
		assertEquals("John", e.firstName);
		assertEquals(7, e.count.intValue());
	}
	
	@Test
	public void testBulkInsert() throws Exception {
		deleteAll();
		db.bulkInsert(Stream.of(new SlimTestEntityWithoutId("Mary", 3), new SlimTestEntityWithoutId("Ann", null)).collect(toList()));
		assertEquals(2, db.listAll(SlimTestEntityWithoutId.class).size());
	}

	@Test
	public void testWhere() throws Exception {
		deleteAll();
		db.bulkInsert(Stream.of(new SlimTestEntityWithoutId("Mary", 3), new SlimTestEntityWithoutId("Marco", 1), new SlimTestEntityWithoutId("Ann", 5)).collect(toList()));
		assertEquals(2, db.where("name LIKE ?", "Ma%").list(SlimTestEntityWithoutId.class).size());
	}
}
