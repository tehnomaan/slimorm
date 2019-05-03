package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.sql.Statement;
import java.util.*;
import static java.util.stream.Collectors.*;

import org.junit.*;
import eu.miltema.slimorm.Database;
import eu.miltema.slimorm.UnauthorizedException;

public class TestRestrictionsAndAuthorization extends AbstractDatabaseTest {

	static class RstDatabase extends Database {
		public RstDatabase() throws Exception {
			super("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/slimtest", "slimuser", "slim1234");
		}
		@Override
		protected String injectIntoWhereExpression(Class<?> entityClass, String whereExpression) {
			return (whereExpression == null ? "count=?" : "(" + whereExpression + ") AND count=?");
		}
		@Override
		protected Object[] injectWhereParameters(Class<?> entityClass, Object[] whereParameters) {
			if (whereParameters != null) {
				whereParameters = Arrays.copyOf(whereParameters, whereParameters.length + 1);
				whereParameters[whereParameters.length - 1] = 123;
				return whereParameters;
			}
			else return new Object[] {123};
		}
		@Override
		protected void authorize(Object entity) throws UnauthorizedException {
			if (((Entity) entity).count.intValue() != 123)
				throw new UnauthorizedException();
		}
		
	}

	private static RstDatabase rdb;
	private static int id1, id3;

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
		db.transaction((db, connection) -> {
			try(Statement stmt = connection.createStatement()) {
				stmt.executeUpdate("TRUNCATE entity");
				return null;
			}
		});
		id1 = db.insert(new Entity("John", 123)).id;
		db.insert(new Entity("Jack", 123));
		id3 = db.insert(new Entity("Jack", 456)).id;
		rdb = new RstDatabase();
	}

	@Test
	public void testInjectedList() throws Exception {
		List<Entity> list = rdb.listAll(Entity.class);
		assertEquals(2, list.size());
		assertEquals(123, list.get(0).count.intValue());
		assertEquals(123, list.get(1).count.intValue());
	}

	@Test
	public void testInjectedListWehere() throws Exception {
		List<Entity> list = rdb.listWhere(Entity.class, "name=?", "Jack");
		assertEquals(1, list.size());
		assertEquals(123, list.get(0).count.intValue());
		assertEquals("Jack", list.get(0).name);
	}

	@Test
	public void testInjectedStream() throws Exception {
		List<Entity> list = rdb.streamWhere(Entity.class, "name=?", "Jack").collect(toList());
		assertEquals(1, list.size());
		assertEquals(123, list.get(0).count.intValue());
		assertEquals("Jack", list.get(0).name);
	}

	@Test
	public void testAuthorized() throws Exception {
		Entity e = new Entity("John", 123);
		e.id = id1;
		rdb.update(e);
	}

	@Test(expected = UnauthorizedException.class)
	public void testUnauthorized1() throws Exception {
		Entity e = new Entity("Jim", 456);
		e.id = id3;
		rdb.update(e);
	}

	@Test(expected = UnauthorizedException.class)
	public void testUnauthorized2() throws Exception {
		Entity e = new Entity("Peter", 456);
		rdb.insert(e);
	}
}
