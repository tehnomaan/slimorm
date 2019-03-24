package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.sql.Statement;
import java.util.List;

import org.junit.*;

public class TestForeignKeys extends AbstractDatabaseTest {

	private static final long EKEY1 = 15;
	private static final long EKEY2 = 16;
	private static int key1, key2;

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
		db.transaction((db, connection) -> {
			try(Statement stmt = connection.createStatement()) {
				stmt.executeUpdate("TRUNCATE entity_fkey");
				stmt.executeUpdate("TRUNCATE entity_with_types");
				stmt.executeUpdate("TRUNCATE entity_with_manual_id");

				EntityWithManualId e = new EntityWithManualId(EKEY1, "John", null);
				EntityFKey ef = new EntityFKey("Jack", e, null);
				db.insert(e);
				key1 = db.insert(ef).id;

				EntityWithTypes et = new EntityWithTypes(e1 -> e1.fString = "abc", e1 -> e1.fJson2 = new String[] {"xyz", "123"}, e1 -> e1.fInt1 = 445);
				e = new EntityWithManualId(EKEY2, "Mary", 80);
				ef = new EntityFKey("Ann", e, et);
				db.insert(e);
				db.insert(et);
				key2 = db.insert(ef).id;
				return null;
			}
		});
	}

	@Before
	public void setup() throws Exception {
		super.deleteAll();
	}

	@Test
	public void testForeignKey() throws Exception {
		assertEquals(EKEY1, db.getById(EntityFKey.class, key1).entity.id.intValue());
	}

	@Test
	public void testListAll() throws Exception {
		List<EntityFKey> list = db.listAll(EntityFKey.class);
		assertEquals(2, list.size());
		assertNotNull(list.get(0).entity.id);
		assertNotNull(list.get(1).entity.id);
		assertNull(list.get(0).entity.fInt1);//By default, listAll must not fetch anything except id in referenced entities
		assertNull(list.get(1).entity.fInt1);//By default, listAll must not fetch anything except id in referenced entities
	}

	@Test
	public void testFetchWithAllColumns() throws Exception {
		EntityFKey ef = db.where("id=?", key2).referencedColumns("*").fetch(EntityFKey.class);
		assertEquals("Mary", ef.entity.name);
		assertEquals(80, ef.entity.fInt1.intValue());
		assertEquals("abc", ef.entityWithTypes.fString);
		assertArrayEquals(new String[] {"xyz", "123"}, ef.entityWithTypes.fJson2);
	}

	@Test
	public void testListAllColumns() throws Exception {
		EntityFKey ef = db.where("id=?", key2).referencedColumns("*").list(EntityFKey.class).get(0);
		assertEquals("Mary", ef.entity.name);
		assertEquals(80, ef.entity.fInt1.intValue());
		assertEquals("abc", ef.entityWithTypes.fString);
		assertArrayEquals(new String[] {"xyz", "123"}, ef.entityWithTypes.fJson2);
	}

	@Test
	public void testListSpecificColumns() throws Exception {
		EntityFKey ef = db.where("id=?", key2).referencedColumns("f_int1").list(EntityFKey.class).get(0);
		assertNull("Mary", ef.entity.name);
		assertEquals(80, ef.entity.fInt1.intValue());
		assertNull(ef.entityWithTypes.fString);
		assertEquals(445, ef.entityWithTypes.fInt1);
	}

	public void testListSpecificClassColumns() throws Exception {
		EntityFKey ef = db.where("id=?", key2).
				referencedColumns(EntityWithTypes.class, "f_int1").//fetch f_int1 for EntityWithTypes only
				list(EntityFKey.class).get(0);
		assertNull("Mary", ef.entity.name);
		assertNull(ef.entity.fInt1);
		assertNull(ef.entityWithTypes.fString);
		assertEquals(445, ef.entityWithTypes.fInt1);
	}
}
