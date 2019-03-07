package eu.miltema.slimorm;

import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

public class SqlQuery {

	private Database database;
	String sql;
	String whereExpression;
	Object[] parameters;

	public SqlQuery(Database database, String sql) {
		this.database = database;
		this.sql = sql;
	}

	public <T> Stream<? extends T> stream(Class<? extends T> entityClass) throws Exception {
		return list(entityClass).stream();
	}

	public <T> Collection<T> list(Class<? extends T> entityClass) throws Exception {
		return database.runStatements((db, conn) -> {
			try(PreparedStatement stmt = conn.prepareStatement(getSqlStatement(entityClass))) {
				database.bindWhereParameters(stmt, 0, parameters);
				try(ResultSet rs = stmt.executeQuery()) {
					ArrayList<T> list = new ArrayList<>();
					FieldProperties[] fields = getFieldMappers(rs, database.getProperties(entityClass));
					while(rs.next())
						list.add(buildEntity(entityClass, rs, fields));
					return list;
				}
			}
		});
	}

	public <T> T first(Class<? extends T> entityClass) throws Exception {
		return database.runStatements((db, conn) -> {
			try(PreparedStatement stmt = conn.prepareStatement(getSqlStatement(entityClass))) {
				database.bindWhereParameters(stmt, 0, parameters);
				try(ResultSet rs = stmt.executeQuery()) {
					if (!rs.next())
						return null;
					return buildEntity(entityClass, rs, getFieldMappers(rs, database.getProperties(entityClass)));
				}
			}
		});
	}

	private String getSqlStatement(Class<?> entityClass) {
		EntityProperties props = database.getProperties(entityClass);
		if (sql == null)
			sql = props.sqlSelect;
		if (whereExpression != null)
			sql += " WHERE " + whereExpression;
		return sql;
	}

	private FieldProperties[] getFieldMappers(ResultSet rs, EntityProperties props) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		FieldProperties[] fields = new FieldProperties[count];
		for(int i = 0; i < count; i++)
			fields[i] = props.mapColumnToField.get(rsmd.getColumnName(i + 1));
		return fields;
	}

	private <T> T buildEntity(Class<? extends T> entityClass, ResultSet rs, FieldProperties[] fields) throws Exception {
		T entity = (T) entityClass.newInstance();
		for(int i = 0; i < fields.length; i++)
			if (fields[i] != null)
				fields[i].field.set(entity, fields[i].loadBinder.convert(rs, i + 1));
		return entity;
	}
}
