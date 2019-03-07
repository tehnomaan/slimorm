package eu.miltema.slimorm;

import static java.sql.Types.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;

public class JdbcBinders {

	static final JdbcBinders instance = new JdbcBinders();

	HashMap<Class<?>, SaveBinder> saveBinders = new HashMap<>();
	HashMap<Class<?>, LoadBinder> loadBinders = new HashMap<>();

	public JdbcBinders() {
		saveBinders.put(Byte.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Byte)param).intValue() & 255);});
		saveBinders.put(byte.class, (stmt, i, param) -> stmt.setInt(i, ((Byte)param).intValue() & 255));
		saveBinders.put(Short.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Short)param).intValue());});
		saveBinders.put(short.class, (stmt, i, param) -> stmt.setInt(i, ((Short)param).intValue()));
		saveBinders.put(Integer.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, (Integer)param);});
		saveBinders.put(int.class, (stmt, i, param) -> stmt.setInt(i, (Integer)param));
		saveBinders.put(Long.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setLong(i, (Long)param);});
		saveBinders.put(long.class, (stmt, i, param) -> stmt.setLong(i, (Long)param));
		saveBinders.put(Float.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, FLOAT); else stmt.setFloat(i, (Float)param);});
		saveBinders.put(float.class, (stmt, i, param) -> stmt.setFloat(i, (Float)param));
		saveBinders.put(Double.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, DOUBLE); else stmt.setDouble(i, (Double)param);});
		saveBinders.put(double.class, (stmt, i, param) -> stmt.setDouble(i, (Double)param));
		saveBinders.put(Boolean.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BOOLEAN); else stmt.setBoolean(i, (Boolean)param);});
		saveBinders.put(boolean.class, (stmt, i, param) -> stmt.setBoolean(i, (Boolean)param));
		saveBinders.put(String.class, (stmt, i, param) -> stmt.setString(i, (String)param));
		saveBinders.put(Timestamp.class, (stmt, i, param) -> stmt.setTimestamp(i, (Timestamp)param));
		saveBinders.put(Instant.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.from((Instant)param))));
		saveBinders.put(java.util.Date.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : new Timestamp(((java.util.Date)param).getTime()))));
		saveBinders.put(LocalDateTime.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.valueOf((LocalDateTime)param))));
		saveBinders.put(LocalDate.class, (stmt, i, param) -> stmt.setDate(i, (param == null ? null : java.sql.Date.valueOf((LocalDate)param))));
		saveBinders.put(byte[].class, (stmt, i, param) -> stmt.setBytes(i, (byte[])param));
//		saveBinders.put(String[].class, (stmt, i, param) -> stmt.setArray(i, stmt.getConnection().createArrayOf("text", (String[]) param)));
		saveBinders.put(BigDecimal.class, (stmt, i, param) -> stmt.setBigDecimal(i, (BigDecimal)param));

		loadBinders.put(Byte.class, (rs, i) -> nvl(new Byte((byte) rs.getInt(i)), rs));
		loadBinders.put(byte.class, (rs, i) -> nvl(new Byte((byte) rs.getInt(i)), rs));
		loadBinders.put(Short.class, (rs, i) -> nvl(new Short((short) rs.getInt(i)), rs));
		loadBinders.put(short.class, (rs, i) -> nvl(new Short((short) rs.getInt(i)), rs));
		loadBinders.put(Integer.class, (rs, i) -> nvl(new Integer(rs.getInt(i)), rs));
		loadBinders.put(int.class, (rs, i) -> nvl(new Integer(rs.getInt(i)), rs));
		loadBinders.put(Long.class, (rs, i) -> nvl(new Long(rs.getLong(i)), rs));
		loadBinders.put(long.class, (rs, i) -> nvl(new Long(rs.getLong(i)), rs));
		loadBinders.put(Float.class, (rs, i) -> nvl(new Float(rs.getFloat(i)), rs));
		loadBinders.put(float.class, (rs, i) -> nvl(new Float(rs.getFloat(i)), rs));
		loadBinders.put(Double.class, (rs, i) -> nvl(new Double(rs.getDouble(i)), rs));
		loadBinders.put(double.class, (rs, i) -> nvl(new Double(rs.getDouble(i)), rs));
		loadBinders.put(Boolean.class, (rs, i) -> nvl(new Boolean(rs.getBoolean(i)), rs));
		loadBinders.put(boolean.class, (rs, i) -> nvl(new Boolean(rs.getBoolean(i)), rs));
		loadBinders.put(String.class, (rs, i) -> rs.getString(i));
		loadBinders.put(Timestamp.class, (rs, i) -> rs.getTimestamp(i));
		loadBinders.put(Instant.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : ts.toInstant());});
		loadBinders.put(java.util.Date.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : new java.util.Date(ts.getTime()));});
		loadBinders.put(LocalDateTime.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : ts.toLocalDateTime());});
		loadBinders.put(LocalDate.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : ts.toLocalDateTime().toLocalDate());});
		loadBinders.put(byte[].class, (rs, i) -> rs.getBytes(i));
		loadBinders.put(BigDecimal.class, (rs, i) -> rs.getBigDecimal(i));
	}

	private <T> T nvl(T value, ResultSet rs) throws SQLException {
		return (rs.wasNull() ? null : value);
	}
}
