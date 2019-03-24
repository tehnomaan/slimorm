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
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fString = "abc"));
		assertEquals("abc", db.getById(EntityWithTypes.class, e.id).fString);
	}

	@Test
	public void testByte() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fByte1 = 23));
		assertEquals(23, db.getById(EntityWithTypes.class, e.id).fByte1);
		e = db.insert(new EntityWithTypes(x -> x.fByte2 = 34));
		assertEquals(34, db.getById(EntityWithTypes.class, e.id).fByte2.byteValue());
	}

	@Test
	public void testShort() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fShort1 = 23));
		assertEquals(23, db.getById(EntityWithTypes.class, e.id).fShort1);
		e = db.insert(new EntityWithTypes(x -> x.fShort2 = 34));
		assertEquals(34, db.getById(EntityWithTypes.class, e.id).fShort2.shortValue());
	}

	@Test
	public void testInt() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fInt1 = 23));
		assertEquals(23, db.getById(EntityWithTypes.class, e.id).fInt1);
		e = db.insert(new EntityWithTypes(x -> x.fInt2 = 34));
		assertEquals(34, db.getById(EntityWithTypes.class, e.id).fInt2.intValue());
	}

	@Test
	public void testLong() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fLong1 = 23));
		assertEquals(23L, db.getById(EntityWithTypes.class, e.id).fLong1);
		e = db.insert(new EntityWithTypes(x -> x.fLong2 = 34L));
		assertEquals(34L, db.getById(EntityWithTypes.class, e.id).fLong2.longValue());
	}

	@Test
	public void testFloat() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fFloat1 = 23.78f));
		assertEquals(23.78f, db.getById(EntityWithTypes.class, e.id).fFloat1, .0001f);
		e = db.insert(new EntityWithTypes(x -> x.fFloat2 = 34.11f));
		assertEquals(34.11f, db.getById(EntityWithTypes.class, e.id).fFloat2.floatValue(), .0001f);
	}

	@Test
	public void testDouble() throws Exception {
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fDouble1 = 23.28d));
		assertEquals(23.28d, db.getById(EntityWithTypes.class, e.id).fDouble1, .0001d);
		e = db.insert(new EntityWithTypes(x -> x.fDouble2 = 34.11d));
		assertEquals(34.11d, db.getById(EntityWithTypes.class, e.id).fDouble2.doubleValue(), .0001d);
	}

	@Test
	public void testBigDecimal() throws Exception {
		BigDecimal b = new BigDecimal("1234567890123456789012345678901234567890.55");
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fBigDecimal = b));
		assertEquals(b, db.getById(EntityWithTypes.class, e.id).fBigDecimal);
	}

	@Test
	public void testByteArray() throws Exception {
		byte[] ba = {4, 7, 9, -5};
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fByteArray = ba));
		assertArrayEquals(ba, db.getById(EntityWithTypes.class, e.id).fByteArray);
	}

	@Test
	public void testTimestampWithoutTimezone() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		Timestamp ts = Timestamp.from(i);
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fTimestamp = ts));
		assertEquals(ts, db.getById(EntityWithTypes.class, e.id).fTimestamp);
	}

	@Test
	public void testInstant() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fInstant = i));
		assertEquals(i, db.getById(EntityWithTypes.class, e.id).fInstant);
	}

	@Test
	public void testZonedDateTime() throws Exception {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("Asia/Tokyo"));
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fZonedDateTime = zdt));
		assertEquals(zdt.toInstant(), db.getById(EntityWithTypes.class, e.id).fZonedDateTime.toInstant());
	}

	@Test
	public void testLocalDate() throws Exception {
		LocalDate ld = LocalDate.parse("2012-12-23");
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fLocalDate = ld));
		assertEquals(ld, db.getById(EntityWithTypes.class, e.id).fLocalDate);
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDateTime ldt = LocalDateTime.parse("2007-12-03T10:15:30.20");
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fLocalDateTime = ldt));
		assertEquals(ldt, db.getById(EntityWithTypes.class, e.id).fLocalDateTime);
	}

	@Test
	public void testJSon() throws Exception {
		JSonStruct s = new JSonStruct();
		s.attr1 = "abc";
		s.attr2 = 4.76;
		s.attr3 = true;
		EntityWithTypes e = db.insert(new EntityWithTypes(x -> x.fJson2 = new String[] {"abc", "def"}, x -> x.fJson1 = s));
		e = db.getById(EntityWithTypes.class, e.id);
		assertEquals(4.76, e.fJson1.attr2, .0001);
		assertEquals("def", e.fJson2[1]);
	}
}
