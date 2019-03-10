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
		SlimTestEntity e = new SlimTestEntity();
		e.name = "John";
		db.insert(e);
		assertNotNull(e.id);
	}
	
	@Test
	public void testInsertedFields() throws Exception {
		SlimTestEntity e = new SlimTestEntity();
		e.name = "John";
		db.insert(e);
		e = db.getById(SlimTestEntity.class, e.id);
		assertEquals("John", e.name);
		assertNull(e.count);
	}

	@Test
	public void testUpdate() throws Exception {
		SlimTestEntity e = new SlimTestEntity();
		e.name = "John";
		db.insert(e);
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
		SlimTestEntity e1 = new SlimTestEntity();
		e1.name = "Mary";
		e1.count = 3;
		SlimTestEntity e2 = new SlimTestEntity();
		e2.name = "Ann";
		List<SlimTestEntity> entities = db.bulkInsert(Stream.of(e1, e2).collect(toList()));
		assertEquals(e1.name, entities.get(0).name);
		assertNotNull(e1.id);
		assertNotNull(e2.id);
		e1 = db.getById(SlimTestEntity.class, e1.id);
		assertEquals("Mary", e1.name);
		assertEquals((Integer) 3, e1.count);
		e2 = db.getById(SlimTestEntity.class, e2.id);
		assertEquals("Ann", e2.name);
		assertNull(e2.count);
	}

	@Test
	public void testLargeBatch() throws Exception {
		List<SlimTestEntity> list = IntStream.range(1, 10000).mapToObj(i -> {
			SlimTestEntity ste = new SlimTestEntity();
			ste.name = "nimi" + i;
			ste.count = i;
			return ste;
		}).collect(toList());
		db.bulkInsert(list);
	}

	@Test
	public void testDelete() throws Exception {
		SlimTestEntity e = new SlimTestEntity();
		e.name = "John";
		db.insert(e);
		db.delete(SlimTestEntity.class, e.id);
		assertNull(db.getById(SlimTestEntity.class, e.id));
	}

	@Test
	public void testDeleteWhere() throws Exception {
		deleteAll();
		db.bulkInsert(IntStream.rangeClosed(1, 10).mapToObj(i -> {
			SlimTestEntity e = new SlimTestEntity();
			e.name = "Mary";
			e.count = i;
			return e;
		}).collect(toList()));
		assertEquals(4, db.deleteWhere(SlimTestEntity.class, "count>=?", 7));
	}

	@Test
	public void testSuccessfulTransaction() throws Exception {
		SlimTestEntity e1 = db.transaction((db, connection) -> {
			SlimTestEntity e = new SlimTestEntity();
			e.name = "John";
			db.insert(e);
			e.name = "Peter";
			e.count = 4;
			db.update(e);
			return e;
		});
		e1 = db.getById(SlimTestEntity.class, e1.id);
		assertEquals(4, e1.count.intValue());
	}
	
	@Test(expected = TestWriteException.class)
	public void testFailedTransaction() throws Exception {
		deleteAll();
		try {
			db.transaction((db, connection) -> {
				SlimTestEntity e = new SlimTestEntity();
				e.name = "John";
				e.count = 2;
				db.insert(e);
				e.name = "Peter";
				e.count = 4;
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
