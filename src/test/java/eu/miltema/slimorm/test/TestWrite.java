package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.util.List;
import static java.util.stream.Collectors.*;
import java.util.stream.*;

import org.junit.*;

import eu.miltema.slimorm.RecordNotFoundException;
import eu.miltema.slimorm.TransactionException;

/**
 * Tests INSERT/UPDATE/DELETE functionality. Prerequisite is that database slimtest exists and user slimuser has access to it (password slim1234)
 *
 * @author Margus
 *
 */
public class TestWrite extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testInsertReturnsKey() throws Exception {
		assertNotNull(db.insert(new Entity("John", null)).id);
	}
	
	@Test
	public void testInsertedFields() throws Exception {
		Entity e = db.insert(new Entity("John", null));
		e = db.getById(Entity.class, e.id);
		assertEquals("John", e.name);
		assertNull(e.count);
	}

	@Test
	public void testIdWithoutAnnotations() throws Exception {
		EntityDefaultId e = new EntityDefaultId();
		e.name = "Mike";
		e = db.getById(EntityDefaultId.class, db.insert(e).id);
		assertEquals("Mike", e.name);
	}

	@Test
	public void testUpdate() throws Exception {
		Entity e = db.insert(new Entity("John", null));
		e.name = "Peter";
		e.count = 4;
		db.update(e);
		e = db.getById(Entity.class, e.id);
		assertEquals("Peter", e.name);
		assertEquals((Integer) 4, e.count);
		e.count = null;
		db.update(e);
		e = db.getById(Entity.class, e.id);
		assertNull(e.count);
	}

	@Test(expected = RecordNotFoundException.class)
	public void testUpdateNonExistingEntity() throws Exception {
		Entity e = db.insert(new Entity("John", null));
		e.id = 999999999;
		e.name = "Peter";
		e.count = 4;
		db.update(e);
	}

	@Test
	public void testBulk() throws Exception {
		List<Entity> entities = db.bulkInsert(Stream.of(new Entity("Mary", 3), new Entity("Ann", null)).collect(toList()));
		assertEquals("Mary", entities.get(0).name);
		assertEquals("Ann", entities.get(1).name);
		assertNotNull(entities.get(0).id);
		assertNotNull(entities.get(1).id);
		Entity e1 = db.getById(Entity.class, entities.get(0).id);
		assertEquals("Mary", e1.name);
		assertEquals((Integer) 3, e1.count);
		Entity e2 = db.getById(Entity.class, entities.get(1).id);
		assertEquals("Ann", e2.name);
		assertNull(e2.count);
	}

	@Test
	public void testLargeBulkInsert() throws Exception {
		List<Entity> list = IntStream.rangeClosed(1, 10000).mapToObj(i -> new Entity("nimi" + i, i)).collect(toList());
		list = db.bulkInsert(list);
		assertEquals(10000, list.stream().map(e -> e.id).collect(toSet()).size());
	}

	@Test(expected = RecordNotFoundException.class)
	public void testDelete() throws Exception {
		Integer id = db.insert(new Entity("John", null)).id;
		try {
			db.delete(Entity.class, id);
		}
		catch(Exception x) {
			throw new Exception("Deletion should have been successful");
		}
		db.getById(Entity.class, id);// expecting RecordNotFoundException
	}

	@Test(expected = RecordNotFoundException.class)
	public void testDeleteNonExistingEntity() throws Exception {
		db.delete(Entity.class, 999999999);
	}

	@Test
	public void testDeleteWhere() throws Exception {
		deleteAll();
		db.bulkInsert(IntStream.rangeClosed(1, 10).mapToObj(i -> new Entity("Mary", i)).collect(toList()));
		assertEquals(4, db.deleteWhere(Entity.class, "count>=?", 7));
	}

	@Test
	public void testSuccessfulTransaction() throws Exception {
		Integer id = db.transaction((db, connection) -> {
			Entity e = db.insert(new Entity("John", null));
			e.name = "Peter";
			e.count = 4;
			db.update(e);
			return e.id;
		});
		Entity e = db.getById(Entity.class, id);
		assertEquals(4, e.count.intValue());
	}
	
	@Test(expected = TransactionException.class)
	public void testFailedTransaction() throws Exception {
		deleteAll();
		try {
			db.transaction((db, connection) -> {
				Entity e = db.insert(new Entity("John", 2));
				e.name = "Peter";
				e.count = null;
				db.update(e);
				throw new RuntimeException();
			});
		}
		catch(Exception x) {
			assertEquals(0, db.listAll(Entity.class).size());
			throw x;
		}
	}
}
