package eu.miltema.slimorm;

import static java.sql.Types.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;

public class JdbcBinders {

	static final JdbcBinders instance = new JdbcBinders();

	HashMap<Class<?>, SaveBinder> saveBinders = new HashMap<>();

	public JdbcBinders() {
		saveBinders.put(Byte.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Byte)param).intValue()&255);});
		saveBinders.put(byte.class, (stmt, i, param) -> stmt.setInt(i, ((Byte)param).intValue()&255));
		saveBinders.put(Short.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Short)param).intValue());});
		saveBinders.put(short.class, (stmt, i, param) -> stmt.setInt(i, ((Short)param).intValue()));
		saveBinders.put(Integer.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, (Integer)param);});
		saveBinders.put(int.class, (stmt, i, param) -> stmt.setInt(i, (Integer)param));
		saveBinders.put(Long.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setLong(i, (Long)param);});
		saveBinders.put(long.class, (stmt, i, param) -> stmt.setLong(i, (Long)param));
		saveBinders.put(String.class, (stmt, i, param) -> stmt.setString(i, (String)param));
		saveBinders.put(Timestamp.class, (stmt, i, param) -> stmt.setTimestamp(i, (Timestamp)param));
		saveBinders.put(Instant.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.from((Instant)param))));
		saveBinders.put(java.util.Date.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : new Timestamp(((java.util.Date)param).getTime()))));
		saveBinders.put(LocalDateTime.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.valueOf((LocalDateTime)param))));
		saveBinders.put(LocalDate.class, (stmt, i, param) -> stmt.setDate(i, (param == null ? null : java.sql.Date.valueOf((LocalDate)param))));
		saveBinders.put(byte[].class, (stmt, i, param) -> stmt.setBytes(i, (byte[])param));
		saveBinders.put(String[].class, (stmt, i, param) -> stmt.setArray(i, stmt.getConnection().createArrayOf("text", (String[]) param)));
		saveBinders.put(Float.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, FLOAT); else stmt.setFloat(i, (Float)param);});
		saveBinders.put(float.class, (stmt, i, param) -> stmt.setFloat(i, (Float)param));
		saveBinders.put(Double.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, DOUBLE); else stmt.setDouble(i, (Double)param);});
		saveBinders.put(double.class, (stmt, i, param) -> stmt.setDouble(i, (Double)param));
		saveBinders.put(Boolean.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BOOLEAN); else stmt.setBoolean(i, (Boolean)param);});
		saveBinders.put(boolean.class, (stmt, i, param) -> stmt.setBoolean(i, (Boolean)param));
		saveBinders.put(BigDecimal.class, (stmt, i, param) -> stmt.setBigDecimal(i, (BigDecimal)param));
	}
}
