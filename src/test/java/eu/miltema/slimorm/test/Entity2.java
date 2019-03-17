package eu.miltema.slimorm.test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.function.Consumer;

import javax.persistence.*;

import eu.miltema.slimorm.annot.JSon;

@Table(name = "slim_test_types")
public class Entity2 {

	@Id
	@GeneratedValue
	public int id;

	public String fString;
	public byte fByte1;
	public Byte fByte2;
	public short fShort1;
	public Short fShort2;
	public int fInt1;
	public Integer fInt2;
	public long fLong1;
	public Long fLong2;
	public float fFloat1;
	public Float fFloat2;
	public double fDouble1;
	public Double fDouble2;
	public BigDecimal fBigDecimal;
	public byte[] fByteArray;
	public Timestamp fTimestamp;
	public Instant fInstant;
	public ZonedDateTime fZonedDateTime;
	public LocalDate fLocalDate;
	public LocalDateTime fLocalDateTime;
	@JSon public TestStruct fJson1;
	@JSon public String[] fJson2;

	public Entity2() {
	}

	@SafeVarargs
	public Entity2(Consumer<Entity2> ... initializers) {
		for(Consumer<Entity2> initializer : initializers)
			initializer.accept(this);
	}
}
