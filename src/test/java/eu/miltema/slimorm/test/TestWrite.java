package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.util.List;
import static java.util.stream.Collectors.*;
import java.util.stream.*;

import org.junit.*;

/**
 * Tests INSERT/UPDATE/DELETE functionality. Prerequisite is that database slimtest exists and user slimuser has access to it (password slim1234)
 *
 * @author Margus
 *
 */
public class TestWrite extends AbstractDatabaseTest {

	private class TestWriteException extends Exception {
		
	}

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testInsertReturnsKey() throws Exception {
		assertNotNull(db.insert(new SlimTestEntity("John", null)).id);
	}
	
	@Test
	public void testInsertedFields() throws Exception {
		SlimTestEntity e = db.insert(new SlimTestEntity("John", null));
		e = db.getById(SlimTestEntity.class, e.id);
		assertEquals("John", e.name);
		assertNull(e.count);
	}

	@Test
	public void testUpdate() throws Exception {
		SlimTestEntity e = db.insert(new SlimTestEntity("John", null));
		e.name = "Peter";
		e.count = 4;
		db.update(e);
		e = db.getById(SlimTestEntity.class, e.id);
		assertEquals("Peter", e.name);
		assertEquals((Integer) 4, e.count);
		e.count = null;
		db.update(e);
		e = db.getById(SlimTestEntity.class, e.id);
		assertNull(e.count);
	}

	@Test
	public void testInsertBatch() throws Exception {
		List<SlimTestEntity> entities = db.bulkInsert(Stream.of(new SlimTestEntity("Mary", 3), new SlimTestEntity("Ann", null)).collect(toList()));
		assertEquals("Mary", entities.get(0).name);
		assertEquals("Ann", entities.get(1).name);
		assertNotNull(entities.get(0).id);
		assertNotNull(entities.get(1).id);
		SlimTestEntity e1 = db.getById(SlimTestEntity.class, entities.get(0).id);
		assertEquals("Mary", e1.name);
		assertEquals((Integer) 3, e1.count);
		SlimTestEntity e2 = db.getById(SlimTestEntity.class, entities.get(1).id);
		assertEquals("Ann", e2.name);
		assertNull(e2.count);
	}

	@Test
	public void testLargeBatch() throws Exception {
		List<SlimTestEntity> list = IntStream.range(1, 10000).mapToObj(i -> new SlimTestEntity("nimi" + i, i)).collect(toList());
		db.bulkInsert(list);
	}

	@Test
	public void testDelete() throws Exception {
		Integer id = db.insert(new SlimTestEntity("John", null)).id;
		db.delete(SlimTestEntity.class, id);
		assertNull(db.getById(SlimTestEntity.class, id));
	}

	@Test
	public void testDeleteWhere() throws Exception {
		deleteAll();
		db.bulkInsert(IntStream.rangeClosed(1, 10).mapToObj(i -> new SlimTestEntity("Mary", i)).collect(toList()));
		assertEquals(4, db.deleteWhere(SlimTestEntity.class, "count>=?", 7));
	}

	@Test
	public void testSuccessfulTransaction() throws Exception {
		Integer id = db.transaction((db, connection) -> {
			SlimTestEntity e = db.insert(new SlimTestEntity("John", null));
			e.name = "Peter";
			e.count = 4;
			db.update(e);
			return e.id;
		});
		SlimTestEntity e = db.getById(SlimTestEntity.class, id);
		assertEquals(4, e.count.intValue());
	}
	
	@Test(expected = TestWriteException.class)
	public void testFailedTransaction() throws Exception {
		deleteAll();
		try {
			db.transaction((db, connection) -> {
				SlimTestEntity e = db.insert(new SlimTestEntity("John", 2));
				e.name = "Peter";
				e.count = null;
				db.update(e);
				throw new TestWriteException();
			});
		}
		catch(TestWriteException x) {
			assertEquals(0, db.listAll(SlimTestEntity.class).size());
			throw x;
		}
	}
}
