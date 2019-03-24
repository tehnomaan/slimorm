package eu.miltema.slimorm;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.*;
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
	private boolean initReferences = false;//=true when referenced columns must be initialized
	private String referencedColumns;
	private Map<Class<?>, String> mapReferencedColumns;

	/**
	 * Create a query object with custom SQL and custom logger
	 * @param database database link
	 * @param sql SQL statement
	 * @param logger optonal custom logger
	 */
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
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> Stream<? extends T> stream(Class<? extends T> entityClass) throws SQLException, BindException {
		return list(entityClass).stream();
	}

	/**
	 * Return the results as a list
	 * @param <T> entity type
	 * @param entityClass target entity class
	 * @return entities list
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> List<T> list(Class<? extends T> entityClass) throws SQLException, BindException {
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
					if (!list.isEmpty())
						attachReferences(fields, () -> list.stream());
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
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> T fetch(Class<? extends T> entityClass) throws SQLException, BindException {
		return database.runStatements((db, conn) -> {
			String sql = getSqlStatement(entityClass);
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				database.bindWhereParameters(stmt, 0, parameters);
				try(ResultSet rs = stmt.executeQuery()) {
					if (!rs.next())
						return null;
					FieldProperties[] fields = getFieldMappers(rs, database.dialect.getProperties(entityClass));
					T entity = buildEntity(entityClass, rs, getFieldMappers(rs, database.dialect.getProperties(entityClass)));
					if (entity != null)
						attachReferences(fields, () -> Stream.of(entity));
					return entity;
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
			sql = props.getSqlSelect();
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

	private <T> T buildEntity(Class<? extends T> entityClass, ResultSet rs, FieldProperties[] fields) throws BindException {
		T entity = null;
		try {
			entity = (T) entityClass.newInstance();
		}
		catch(InstantiationException | IllegalAccessException e) {
			throw new BindException("Unable to invoke " + entityClass.getSimpleName() + "()", e);
		}
		for(int i = 0; i < fields.length; i++)
			if (fields[i] != null)
				try {
					fields[i].field.set(entity, fields[i].loadBinder.convert(rs, i + 1));
				} catch (Exception e) {
					throw new BindException("Unable to bind result from " + fields[i].columnName + " to entity field " + fields[i].field.getName(), e);
				}
		return entity;
	}

	private void attachReferences(FieldProperties[] fields, Supplier<Stream<?>> streamSupplier) throws SQLException, BindException {
		if (!initReferences)
			return;
		for(FieldProperties fprop : fields) {
			if (fprop.foreignField == null)
				continue;
			Class<?> tgtClass = fprop.fieldType;// referenced class
			EntityProperties targetProps = database.dialect.getProperties(tgtClass);
			FieldProperties foreignIdFld = targetProps.idField;// id field of referenced class
			Set<Object> refkeys = streamSupplier.get().
					map(rec -> fprop.getFieldValue(rec)).
					filter(fentity -> fentity != null).
					map(fentity -> foreignIdFld.getFieldValue(fentity)).
					filter(key -> key != null).
					collect(toSet());//list of foreign keys
			String selColumns = (mapReferencedColumns == null ? null : mapReferencedColumns.get(tgtClass));
			SqlQuery q = new SqlQuery(database, sql, logger);
			q.sql = "SELECT " + targetProps.idField.columnName + "," + (selColumns == null ? referencedColumns : selColumns) + " FROM " + targetProps.tableName;
			q.whereExpression = foreignIdFld.columnName + " IN (" + refkeys.stream().map(refkey -> "?").collect(joining(",")) + ")";
			q.parameters = refkeys.toArray(new Object[refkeys.size()]);
			Map<Object, Object> refmap = q.stream(tgtClass).collect(toMap(e -> foreignIdFld.getFieldValue(e), e -> e));//map of foreign entities by key
			streamSupplier.get().
				filter(e -> fprop.getFieldValue(e) != null).
				forEach(e -> fprop.setFieldValue(e, refmap.get(foreignIdFld.getFieldValue(fprop.getFieldValue(e)))));//replace foreign entity in each reference
		}
	}

	/**
	 * Set the columns to fetch for all referenced entties
	 *
	 * @param columns comma-separated column list; * indicates all columns
	 * @return the same query object
	 */
	public SqlQuery referencedColumns(String columns) {
		referencedColumns = columns;
		initReferences = true;
		return this;
	}

	/**
	 * Set the columns to fetch for a specific referenced entity.
	 * This method can be used when an entity is referencing multiple other entities and a different set of columns must be fetched for each entity
	 *
	 * @param entityClass referenced entity class
	 * @param columns comma-separated column list; * indicates all columns
	 * @return the same query object
	 */
	public SqlQuery referencedColumns(Class<?> entityClass, String columns) {
		if (mapReferencedColumns == null)
			mapReferencedColumns = new HashMap<>();
		mapReferencedColumns.put(entityClass, columns);
		initReferences = true;
		return this;
	}
}
