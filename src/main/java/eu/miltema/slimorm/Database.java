package eu.miltema.slimorm;

import java.sql.*;
import java.util.*;

import javax.naming.*;
import javax.sql.DataSource;

/**
 * Database link for subsequent CRUD operations
 *
 * @author Margus
 *
 */
public class Database {

	private DatabaseConnectionFactory connFactory;
	private Connection txConnection;//connection for current transaction; when not in transaction context, this field is null
	private Map<Class<?>, EntityProperties> entityProps = new HashMap<>();

	/**
	 * Create database object via datasource
	 * @param dataSource
	 */
	public Database(DataSource dataSource) {
		connFactory = () -> dataSource.getConnection();
	}

	/**
	 * Create a database via custom connection factory
	 * @param connectionFactory
	 */
	public Database(DatabaseConnectionFactory connectionFactory) {
		this.connFactory = connectionFactory;
	}

	/**
	 * Create database object via JNDI handle
	 * @param jndiName JNDI name, for example "jdbc/demoDB"
	 * @throws NamingException
	 */
	public Database(String jndiName) throws NamingException {
		Context ctx = new InitialContext();
		DataSource dataSource = (DataSource)ctx.lookup(jndiName);
		connFactory = () -> dataSource.getConnection();
	}

	/**
	 * Create database with simple non-pooled connections
	 * @param driverName driver class name, for example "org.postgresql.Driver"
	 * @param jdbcUrl database URL, for example "jdbc:postgresql://localhost:5432/demoDB"
	 * @param username SQL username
	 * @param password SQL password
	 * @throws Exception
	 */
	public Database(String driverName, String jdbcUrl, String username, String password) throws Exception {
		Class.forName(driverName).newInstance();
		connFactory = () -> DriverManager.getConnection(jdbcUrl, username, password);
	}

	/**
	 * Insert a single entity into database
	 * @param entity entity to insert
	 * @return the same entity, with @Id field (if any) being initialized
	 * @throws Exception
	 */
	public <T> T insert(T entity) throws Exception {
		EntityProperties props = getProperties(entity.getClass());
		return runStatements((db, conn) -> {
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
				bindMutableParameters(entity, props, stmt);
				stmt.executeUpdate();
				if (props.idField != null && !props.idField.isMutable) {//if auto-generated key is declared
					ResultSet rs = stmt.getGeneratedKeys();
					rs.next();
					props.idField.field.set(entity, rs.getObject(1));
				}
			}
			return entity;
		});
	}

	/**
	 * Insert a collection of entities into database as a batch. Batch insertion is faster than inserting one by one.
	 * @param entities collection of entities to insert
	 * @return the same entities, with @Id field (if any) being initialized
	 * @throws Exception
	 */
	public <T> Collection<T> insertBatch(Collection<T> entities) throws Exception {
		if (entities.isEmpty())
			return entities;
		Object[] array = entities.stream().toArray();
		EntityProperties props = getProperties(array[0].getClass());
		return runStatements((db, conn) -> {
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
				for(int i = 0; i < array.length; i++) {
					bindMutableParameters(array[i], props, stmt);
					stmt.addBatch();
				}
				stmt.executeBatch();
				if (props.idField != null && !props.idField.isMutable) {//if auto-generated key is declared
					ResultSet rs = stmt.getGeneratedKeys();
					int ordinal = 0;
					while(rs.next())
						props.idField.field.set(array[ordinal++], rs.getObject(1));
				}
			}
			return entities;
		});
	}

	/**
	 * Update an existing entity in database. Only entities with @Id field can be updated. When @Id field is missing, use method update(entity, whereExpression, whereParameters)
	 * @param entity entity with new attribute values
	 * @throws Exception
	 */
	public void update(Object entity) throws Exception {
		EntityProperties props = getProperties(entity.getClass());
		if (props.idField == null)
			throw new Exception("Missing @Id field in " + entity.getClass().getSimpleName());
		update(entity, props.sqlWhere, props.idField.field.get(entity));
	}

	/**
	 * Update an existing entity in database. If WHERE expression selects multiple records, then all these records will be updated with new attribute values (except the @Id field).
	 * @param entity entity with new attribute values
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameters for WHERE expression
	 * @throws Exception
	 */
	public void update(Object entity, String whereExpression, Object ... whereParameters) throws Exception {
		runStatements((db, conn) -> {
			EntityProperties props = getProperties(entity.getClass());
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlUpdate + " WHERE " + whereExpression)) {
				int ordinal = bindMutableParameters(entity, props, stmt);
				bindWhereParameters(stmt, ordinal, whereParameters);
				stmt.executeUpdate();
			}
			return entity;
		});
	}

	/**
	 * Delete an existing record
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param id entity id
	 * @return true, if the record existed before deletion; false, if the record did not exist
	 * @throws Exception
	 */
	public boolean delete(Class<?> entityClass, Object id) throws Exception {
		EntityProperties props = getProperties(entityClass);
		if (props.idField == null)
			throw new Exception("Missing @Id field in " + entityClass.getSimpleName());
		return delete(entityClass, props.sqlWhere, id) > 0;
	}

	/**
	 * Delete multiple records
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameters for WHERE expression
	 * @return number of records deleted
	 * @throws Exception
	 */
	public int delete(Class<?> entityClass, String whereExpression, Object ... whereParameters) throws Exception {
		return runStatements((db, conn) -> {
			EntityProperties props = getProperties(entityClass);
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlDelete + " WHERE " + whereExpression)) {
				bindWhereParameters(stmt, 0, whereParameters);
				return stmt.executeUpdate();
			}
		});
	}

	/**
	 * Prepare a SELECT query
	 * @param sqlSelect SQL SELECT statement
	 * @param whereParameters parameter values for WHERE expression
	 * @return query object for fetching the results
	 * @throws Exception
	 */
	public SqlQuery sql(String sqlSelect, Object ... whereParameters) throws Exception {
		SqlQuery q = new SqlQuery(this, sqlSelect);
		q.parameters = whereParameters;
		return q;
	}

	/**
	 * Prepare a SELECT query with WHERE filter only. Subsequent fetch operation (get or list) determines database table for this query
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameter values for WHERE expression
	 * @return query object for fetching the results
	 * @throws Exception
	 */
	public SqlQuery where(String whereExpression, Object ... whereParameters) throws Exception {
		SqlQuery q = new SqlQuery(this, null);
		q.whereExpression = whereExpression;
		q.parameters = whereParameters;
		return q;
	}

	/**
	 * Fetch all records/entities from a database table into a list
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @return list of entities
	 * @throws Exception
	 */
	public <T> List<T> listAll(Class<? extends T> entityClass) throws Exception {
		return new SqlQuery(this, getProperties(entityClass).sqlSelect).list(entityClass);
	}

	/**
	 * Fetch a single record/entity from a database table
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param id entity id
	 * @return entity; returns null, if id refers to non-existing record
	 * @throws Exception
	 */
	public <T> T getById(Class<? extends T> entityClass, Object id) throws Exception {
		return where(getProperties(entityClass).sqlWhere, id).fetch(entityClass);
	}

	/**
	 * Runs a bunch of statements in a single transaction
	 * @param statements statements to run
	 * @return the return value from statements
	 * @throws Exception
	 */
	synchronized public <T> T transaction(TransactionStatements<T> statements) throws Exception {
		if (txConnection != null)
			throw new RuntimeException("Nested transactions are not supported");
		txConnection = connFactory.getConnection();
		try {
			txConnection.setAutoCommit(false);
			T returnValue = runStatements(statements);
			txConnection.commit();
			return returnValue;
		}
		catch(Exception x) {
			txConnection.rollback();
			throw x;
		}
		finally {
			txConnection = null;
		}
	}

	private <T> int bindMutableParameters(T entity, EntityProperties props, PreparedStatement stmt) throws SQLException, IllegalAccessException {
		int ordinal = 0;
		for(FieldProperties fprops : props.mutableFields)
			fprops.saveBinder.bind(stmt, ++ordinal, fprops.field.get(entity));
		return ordinal;
	}

	void bindWhereParameters(PreparedStatement stmt, int ordinal, Object... whereParameters) throws SQLException {
		if (whereParameters != null)
			for(Object whereParam : whereParameters)
				if (whereParam == null)
					stmt.setNull(++ordinal, Types.VARCHAR);
				else JdbcBinders.instance.saveBinders.get(whereParam.getClass()).bind(stmt, ++ordinal, whereParam);
	}

	EntityProperties getProperties(Class<?> entityClass) {
		EntityProperties props = entityProps.get(entityClass);
		if (props == null)
			entityProps.put(entityClass, props = new EntityProperties(entityClass));
		return props;
	}

	synchronized <T> T runStatements(TransactionStatements<T> statements) throws Exception {
		if (txConnection != null)
			return statements.statements(this, txConnection);//in transaction context, connection management takes place in method transaction

		// When not in transaction context, connection management takes place in this method
		try(Connection connection = connFactory.getConnection()) {
			return statements.statements(this, connection);
		}
	}
}
