package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.io.*;
import java.sql.Statement;
import java.util.List;
import static java.util.stream.Collectors.*;
import java.util.stream.*;

import org.junit.*;

import eu.miltema.slimorm.Database;

/**
 * Tests INSERT/UPDATE/DELETE functionality. Prerequisite is that database slimtest exists and user slimuser has access to it (password slim1234)
 *
 * @author Margus
 *
 */
public class TestWrite {

	private static Database db;

	@BeforeClass
	public static void setupClass() throws Exception {
		db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/slimtest", "slimuser", "slim1234");
		String sql;
		try(BufferedReader r = new BufferedReader(new InputStreamReader(TestWrite.class.getResourceAsStream("/pg.sql")))) {
			sql = r.lines().collect(Collectors.joining("\r\n"));
		}
		db.transaction((db, connection) -> {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute(sql);
				return 5;
			}
		});
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
		List<SlimTestEntity> entities = db.insertBatch(Stream.of(e1, e2).collect(toList()));
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
		db.insertBatch(list);
	}
}
