package eu.miltema.slimorm.dialect;

import static java.sql.Types.*;
import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

import com.google.gson.Gson;

import eu.miltema.slimorm.*;

public class DefaultDialect implements Dialect {

	Map<Class<?>, EntityProperties> entityProps = new HashMap<>();
	HashMap<Class<?>, SaveBinder> saveBinders = new HashMap<>();
	HashMap<Class<?>, LoadBinder> loadBinders = new HashMap<>();

	public DefaultDialect() {
		saveBinders.put(Byte.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Byte)param).intValue() & 255);});
		saveBinders.put(byte.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Byte)param).intValue() & 255);});
		saveBinders.put(Short.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Short)param).intValue());});
		saveBinders.put(short.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, ((Short)param).intValue());});
		saveBinders.put(Integer.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, (Integer)param);});
		saveBinders.put(int.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setInt(i, (Integer)param);});
		saveBinders.put(Long.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BIGINT); else stmt.setLong(i, (Long)param);});
		saveBinders.put(long.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BIGINT); else stmt.setLong(i, (Long)param);});
		saveBinders.put(Float.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, FLOAT); else stmt.setFloat(i, (Float)param);});
		saveBinders.put(float.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, FLOAT); else stmt.setFloat(i, (Float)param);});
		saveBinders.put(Double.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, DOUBLE); else stmt.setDouble(i, (Double)param);});
		saveBinders.put(double.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, INTEGER); else stmt.setDouble(i, (Double)param);});
		saveBinders.put(Boolean.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BOOLEAN); else stmt.setBoolean(i, (Boolean)param);});
		saveBinders.put(boolean.class, (stmt, i, param) -> {if (param == null) stmt.setNull(i, BOOLEAN); else stmt.setBoolean(i, (Boolean)param);});
		saveBinders.put(String.class, (stmt, i, param) -> stmt.setString(i, (String)param));
		saveBinders.put(Timestamp.class, (stmt, i, param) -> stmt.setTimestamp(i, (Timestamp)param));
		saveBinders.put(Instant.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.from((Instant)param))));
		saveBinders.put(java.util.Date.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : new Timestamp(((java.util.Date)param).getTime()))));
		saveBinders.put(LocalDateTime.class, (stmt, i, param) -> stmt.setTimestamp(i, (param == null ? null : Timestamp.valueOf((LocalDateTime)param))));
		saveBinders.put(LocalDate.class, (stmt, i, param) -> stmt.setDate(i, (param == null ? null : java.sql.Date.valueOf((LocalDate)param))));
		saveBinders.put(ZonedDateTime.class, sbZonedDateTime);
		saveBinders.put(byte[].class, (stmt, i, param) -> stmt.setBytes(i, (byte[])param));
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
		loadBinders.put(LocalDateTime.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : ts.toLocalDateTime());});
		loadBinders.put(LocalDate.class, (rs, i) -> {Timestamp ts = rs.getTimestamp(i); return (ts == null ? null : ts.toLocalDateTime().toLocalDate());});
		loadBinders.put(ZonedDateTime.class, (rs, i) -> {OffsetDateTime odt = rs.getObject(i, OffsetDateTime.class); return (odt == null ? null : odt.toZonedDateTime());});
		loadBinders.put(byte[].class, (rs, i) -> rs.getBytes(i));
		loadBinders.put(BigDecimal.class, (rs, i) -> rs.getBigDecimal(i));
	}

	private SaveBinder sbZonedDateTime = (stmt, i, param) -> {
		if (param != null) {
			ZonedDateTime zdt = (ZonedDateTime) param;
			TimeZone tz = TimeZone.getTimeZone(zdt.getZone());
			stmt.setTimestamp(i, Timestamp.from(zdt.toInstant()), Calendar.getInstance(tz));
		}
		else stmt.setTimestamp(i, null);
	};

	protected <T> T nvl(T value, ResultSet rs) throws SQLException {
		return (rs.wasNull() ? null : value);
	}

	@Override
	public LoadBinder getLoadBinder(Class<?> fieldType) {
		return loadBinders.get(fieldType);
	}

	@Override
	public SaveBinder getSaveBinder(Class<?> fieldType) {
		return saveBinders.get(fieldType);
	}

	protected String toSnakeCase(String s) {
		return s.replaceAll("([a-z]|[0-9])([A-Z]+)", "$1_$2").toLowerCase();
		
	}

	@Override
	public String getTableName(String javaName) {
		return toSnakeCase(javaName);
	}

	@Override
	public String getColumnName(String javaName) {
		return toSnakeCase(javaName);
	}

	@Override
	public String getSqlForInsert(String tableName, Collection<String> mutableColumns) {
		return "INSERT INTO " + tableName + "(" +
				mutableColumns.stream().collect(joining(",")) +
				") VALUES ";
	}

	@Override
	public String getSqlForValuesClause(String tableName, Collection<String> mutableColumns) {
		return "(" + mutableColumns.stream().map(column -> "?").collect(joining(",")) +")";
	}

	@Override
	public String getSqlForUpdate(String tableName, Collection<String> mutableColumns) {
		return "UPDATE " + tableName + " SET " +
				mutableColumns.stream().map(column -> column + "=?").collect(joining(","));
	}

	@Override
	public String getSqlForDelete(String tableName) {
		return "DELETE FROM " + tableName;
	}

	@Override
	public String getSqlForSelect(String tableName, Collection<String> columns) {
		return "SELECT * FROM " + tableName;
	}

	@Override
	public String getSqlForWhere(String tableName, String idColumn) {
		return idColumn + "=?";
	}

	@Override
	public EntityProperties getProperties(Class<?> entityClass) {
		EntityProperties props = entityProps.get(entityClass);
		if (props == null) {
			entityProps.put(entityClass, props = new EntityProperties(entityClass, this));
			props.finishInitialization();
		}
		return props;
	}

	@Override
	public LoadBinder getJSonLoadBinder(Class<?> fieldClass) {
		return (rs, i) -> {
			String json = rs.getString(i);
			return (json == null ? null : new Gson().fromJson(json, fieldClass));
		};
	}

	@Override
	public SaveBinder getJSonSaveBinder(Class<?> fieldClass) {
		return (stmt, i, param) -> {
			if (param != null) {
				Object jobj;
				try {
//					PGobject jobj = new PGobject();
//					jobj.setType("json");
//					jobj.setValue(new Gson().toJson(param));
					// Implement the above logic without the need of postgre dependencies
					Class<?> clazz = Class.forName("org.postgresql.util.PGobject");
					jobj = clazz.newInstance();
					clazz.getMethod("setType", String.class).invoke(jobj, "json");
					clazz.getMethod("setValue", String.class).invoke(jobj, new Gson().toJson(param));
				}
				catch(Exception x) {
					throw new SQLException(x);
				}
				stmt.setObject(i, jobj);
			}
			else stmt.setObject(i, null);
		};
	}
}
