package eu.miltema.slimorm.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestRead extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEntityWithIdOnly() throws Exception {
		db.getDialect().getProperties(EntityWithIdOnly.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEntityWithoutFields() throws Exception {
		db.getDialect().getProperties(EntityWithoutFields.class);
	}

	@Test
	public void testGetById() throws Exception {
		SlimTestEntity e = new SlimTestEntity();
		e.name = "John";
		db.insert(e);
		assertNotNull(db.getById(SlimTestEntity.class, e.id));
	}

	@Test
	public void testListAll() throws Exception {
		deleteAll();
		SlimTestEntity e1 = new SlimTestEntity();
		e1.name = "Mary";
		SlimTestEntity e2 = new SlimTestEntity();
		e2.name = "Ann";
		db.bulkInsert(Stream.of(e1, e2).collect(toList()));
		List<SlimTestEntity> list = db.listAll(SlimTestEntity.class);
		assertEquals(2, list.size());
	}

	@Test
	public void testListWhere() throws Exception {
		deleteAll();
		db.bulkInsert(IntStream.rangeClosed(1, 10).mapToObj(i -> {
			SlimTestEntity e = new SlimTestEntity();
			e.name = "Mary";
			e.count = i;
			return e;
		}).collect(toList()));
		List<SlimTestEntity> list = db.where("count>=? AND count<=?", 3, 7).list(SlimTestEntity.class);
		assertEquals(5, list.size());
	}
}
