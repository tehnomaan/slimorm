package eu.miltema.slimorm;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A convenience class for building SQL query
 *
 * @author Margus
 *
 */
public class SqlQuery {

	private Database database;
	String sql;
	String whereExpression;
	String orderBy;
	String groupBy;
	Object[] parameters;
	private Consumer<String> logger = message -> {};

	SqlQuery(Database database, String sql, Consumer<String> logger) {
		this.database = database;
		this.sql = sql;
		this.logger = logger;
	}

	/**
	 * Return the results as a stream
	 * @param <T> entity type
	 * @param entityClass target entity class
	 * @return entities stream
	 * @throws Exception when anything goes wrong
	 */
	public <T> Stream<? extends T> stream(Class<? extends T> entityClass) throws Exception {
		return list(entityClass).stream();
	}

	/**
	 * Return the results as a list
	 * @param <T> entity type
	 * @param entityClass target entity class
	 * @return entities list
	 * @throws Exception when anything goes wrong
	 */
	public <T> List<T> list(Class<? extends T> entityClass) throws Exception {
		return database.runStatements((db, conn) -> {
			String sql = getSqlStatement(entityClass);
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				database.bindWhereParameters(stmt, 0, parameters);
				try(ResultSet rs = stmt.executeQuery()) {
					ArrayList<T> list = new ArrayList<>();
					FieldProperties[] fields = getFieldMappers(rs, database.dialect.getProperties(entityClass));
					while(rs.next())
						list.add(buildEntity(entityClass, rs, fields));
					return list;
				}
			}
		});
	}

	/**
	 * Return a single record/entity from the result
	 * @param <T> entity type
	 * @param entityClass target entity class
	 * @return entity; if no records where found, null is returned
	 * @throws Exception when anything goes wrong
	 */
	public <T> T fetch(Class<? extends T> entityClass) throws Exception {
		return database.runStatements((db, conn) -> {
			String sql = getSqlStatement(entityClass);
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				database.bindWhereParameters(stmt, 0, parameters);
				try(ResultSet rs = stmt.executeQuery()) {
					if (!rs.next())
						return null;
					return buildEntity(entityClass, rs, getFieldMappers(rs, database.dialect.getProperties(entityClass)));
				}
			}
		});
	}

	/**
	 * Add an SQL ORDER BY clause to select query
	 * @param columns columns list for ORDER BY, for example "age DESC, name"
	 * @return SqlQuery object
	 */
	public SqlQuery orderBy(String columns) {
		this.orderBy = columns;
		return this;
	}

	/**
	 * Add an SQL GROUP BY clause to select query
	 * @param columns columns list for GROUP BY, for example "name, age"
	 * @return SQLQuery object
	 */
	public SqlQuery groupBy(String columns) {
		this.groupBy = columns;
		return this;
	}

	private String getSqlStatement(Class<?> entityClass) {
		EntityProperties props = database.dialect.getProperties(entityClass);
		if (sql == null)
			sql = props.sqlSelect;
		if (whereExpression != null)
			sql += " WHERE " + whereExpression;
		if (orderBy != null)
			sql += " ORDER BY " + orderBy;
		if (groupBy != null)
			sql += " GROUP BY " + groupBy;
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
