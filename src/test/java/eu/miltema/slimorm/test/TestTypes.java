package eu.miltema.slimorm.test;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;

import org.junit.*;

public class TestTypes  extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testString() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fString = "abc"));
		assertEquals("abc", db.getById(Entity2.class, e.id).fString);
	}

	@Test
	public void testByte() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fByte1 = 23));
		assertEquals(23, db.getById(Entity2.class, e.id).fByte1);
		e = db.insert(new Entity2(x -> x.fByte2 = 34));
		assertEquals(34, db.getById(Entity2.class, e.id).fByte2.byteValue());
	}

	@Test
	public void testShort() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fShort1 = 23));
		assertEquals(23, db.getById(Entity2.class, e.id).fShort1);
		e = db.insert(new Entity2(x -> x.fShort2 = 34));
		assertEquals(34, db.getById(Entity2.class, e.id).fShort2.shortValue());
	}

	@Test
	public void testInt() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fInt1 = 23));
		assertEquals(23, db.getById(Entity2.class, e.id).fInt1);
		e = db.insert(new Entity2(x -> x.fInt2 = 34));
		assertEquals(34, db.getById(Entity2.class, e.id).fInt2.intValue());
	}

	@Test
	public void testLong() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fLong1 = 23));
		assertEquals(23L, db.getById(Entity2.class, e.id).fLong1);
		e = db.insert(new Entity2(x -> x.fLong2 = 34L));
		assertEquals(34L, db.getById(Entity2.class, e.id).fLong2.longValue());
	}

	@Test
	public void testFloat() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fFloat1 = 23.78f));
		assertEquals(23.78f, db.getById(Entity2.class, e.id).fFloat1, .0001f);
		e = db.insert(new Entity2(x -> x.fFloat2 = 34.11f));
		assertEquals(34.11f, db.getById(Entity2.class, e.id).fFloat2.floatValue(), .0001f);
	}

	@Test
	public void testDouble() throws Exception {
		Entity2 e = db.insert(new Entity2(x -> x.fDouble1 = 23.28d));
		assertEquals(23.28d, db.getById(Entity2.class, e.id).fDouble1, .0001d);
		e = db.insert(new Entity2(x -> x.fDouble2 = 34.11d));
		assertEquals(34.11d, db.getById(Entity2.class, e.id).fDouble2.doubleValue(), .0001d);
	}

	@Test
	public void testBigDecimal() throws Exception {
		BigDecimal b = new BigDecimal("1234567890123456789012345678901234567890.55");
		Entity2 e = db.insert(new Entity2(x -> x.fBigDecimal = b));
		assertEquals(b, db.getById(Entity2.class, e.id).fBigDecimal);
	}

	@Test
	public void testByteArray() throws Exception {
		byte[] ba = {4, 7, 9, -5};
		Entity2 e = db.insert(new Entity2(x -> x.fByteArray = ba));
		assertArrayEquals(ba, db.getById(Entity2.class, e.id).fByteArray);
	}

	@Test
	public void testTimestampWithoutTimezone() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		Timestamp ts = Timestamp.from(i);
		Entity2 e = db.insert(new Entity2(x -> x.fTimestamp = ts));
		assertEquals(ts, db.getById(Entity2.class, e.id).fTimestamp);
	}

	@Test
	public void testInstant() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		Entity2 e = db.insert(new Entity2(x -> x.fInstant = i));
		assertEquals(i, db.getById(Entity2.class, e.id).fInstant);
	}

	@Test
	public void testZonedDateTime() throws Exception {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("Asia/Tokyo"));
		Entity2 e = db.insert(new Entity2(x -> x.fZonedDateTime = zdt));
		assertEquals(zdt.toInstant(), db.getById(Entity2.class, e.id).fZonedDateTime.toInstant());
	}

	@Test
	public void testLocalDate() throws Exception {
		LocalDate ld = LocalDate.parse("2012-12-23");
		Entity2 e = db.insert(new Entity2(x -> x.fLocalDate = ld));
		assertEquals(ld, db.getById(Entity2.class, e.id).fLocalDate);
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDateTime ldt = LocalDateTime.parse("2007-12-03T10:15:30.20");
		Entity2 e = db.insert(new Entity2(x -> x.fLocalDateTime = ldt));
		assertEquals(ldt, db.getById(Entity2.class, e.id).fLocalDateTime);
	}

	@Test
	public void testJSon() throws Exception {
		TestStruct s = new TestStruct();
		s.attr1 = "abc";
		s.attr2 = 4.76;
		s.attr3 = true;
		Entity2 e = db.insert(new Entity2(x -> x.fJson2 = new String[] {"abc", "def"}, x -> x.fJson1 = s));
		e = db.getById(Entity2.class, e.id);
		assertEquals(4.76, e.fJson1.attr2, .0001);
		assertEquals("def", e.fJson2[1]);
	}
}
