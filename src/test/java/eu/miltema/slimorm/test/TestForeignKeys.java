package eu.miltema.slimorm.test;

import static org.junit.Assert.assertEquals;

import org.junit.*;

public class TestForeignKeys extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testReadAndWrite() throws Exception {
		Entity e = new Entity("John", 15);
		EntityFKey ef = new EntityFKey("Jack", e);
		db.transaction((db, conn) -> {
			db.insert(e);
			db.insert(ef);
			return null;
		});
		assertEquals(e.id, db.getById(EntityFKey.class, ef.id).entity.id);
	}
}
