package eu.miltema.slimorm.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.*;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.miltema.slimorm.SlimormInitException;

public class TestRead extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test(expected = SlimormInitException.class)
	public void testEntityWithIdOnly() throws Exception {
		db.getDialect().getProperties(EntityWithIdOnly.class);
	}

	@Test(expected = SlimormInitException.class)
	public void testEntityWithoutFields() throws Exception {
		db.getDialect().getProperties(EntityWithoutFields.class);
	}

	@Test
	public void testGetById() throws Exception {
		Entity e = db.insert(new Entity("John", null));
		assertNotNull(db.getById(Entity.class, e.id));
	}

	@Test
	public void testListAll() throws Exception {
		deleteAll();
		Entity e1 = new Entity("Mary", null);
		Entity e2 = new Entity("Ann", null);
		db.bulkInsert(Stream.of(e1, e2).collect(toList()));
		List<Entity> list = db.listAll(Entity.class);
		assertEquals(2, list.size());
	}

	@Test
	public void testListWhere() throws Exception {
		deleteAll();
		db.bulkInsert(IntStream.rangeClosed(1, 10).mapToObj(i -> new Entity("Mary", i)).collect(toList()));
		List<Entity> list = db.where("count>=? AND count<=?", 3, 7).list(Entity.class);
		assertEquals(5, list.size());
	}

	@Test
	public void testReadSql() throws Exception {
		deleteAll();
		db.insert(new Entity("John", 123));
		EntityForSql e = db.sql("SELECT * FROM entity").list(EntityForSql.class).get(0);
		assertEquals("John", e.name);
		assertEquals(123, e.count.intValue());
	}
}
