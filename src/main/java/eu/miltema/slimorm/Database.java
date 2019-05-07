package eu.miltema.slimorm;

import static java.util.stream.Collectors.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.naming.*;
import javax.sql.DataSource;

import eu.miltema.slimorm.dialect.*;

/**
 * Database link for subsequent CRUD operations
 *
 * @author Margus
 *
 */
public class Database {

	private static Map<Class<? extends Database>, Dialect> mapDialects = new HashMap<>();//dialects are database-specific and are cached in this map, since dialect initialization can be time-consuming

	Dialect dialect;
	private DatabaseConnectionFactory connFactory;
	private Connection txConnection;//connection for current transaction; when not in transaction context, this field is null
	private String schema = "public";
	private Consumer<String> logger = message -> {};
	private int batchSize = 1000;

	/**
	 * Create database object via datasource
	 * @param dataSource data source, which provides database connections
	 */
	public Database(DataSource dataSource) {
		connFactory = () -> dataSource.getConnection();
		initDialect();
	}

	/**
	 * Create a database via custom connection factory
	 * @param connectionFactory custom connection factory, which provides database connections
	 */
	public Database(DatabaseConnectionFactory connectionFactory) {
		this.connFactory = connectionFactory;
		initDialect();
	}

	/**
	 * Create database object via JNDI handle
	 * @param jndiName JNDI name, for example "jdbc/demoDB"
	 * @throws NamingException when JNDI name lookup fails
	 */
	public Database(String jndiName) throws NamingException {
		Context ctx = new InitialContext();
		DataSource dataSource = (DataSource)ctx.lookup(jndiName);
		connFactory = () -> dataSource.getConnection();
		initDialect();
	}

	/**
	 * Create database with simple non-pooled connections
	 * @param driverName driver class name, for example "org.postgresql.Driver"
	 * @param jdbcUrl database URL, for example "jdbc:postgresql://localhost:5432/demoDB"
	 * @param username SQL username
	 * @param password SQL password
	 * @throws Exception when anything goes wrong
	 */
	public Database(String driverName, String jdbcUrl, String username, String password) throws Exception {
		Class.forName(driverName).newInstance();
		connFactory = () -> DriverManager.getConnection(jdbcUrl, username, password);
		initDialect();
	}

	/**
	 * Modify the default batch size in bulk insert
	 * @param size batch size
	 * @return database object
	 */
	public Database setBatchSize(int size) {
		this.batchSize = size;
		return this;
	}

	/**
	 * Insert a single entity into database
	 * @param <T> entity type
	 * @param entity entity to insert
	 * @return the same entity, with @Id field (if any) being initialized
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 * @throws UnauthorizedException when entity saving is unauthorized (in authorize-method)
	 */
	public <T> T insert(T entity) throws BindException, SQLException, UnauthorizedException {
		authorize(entity);
		EntityProperties props = dialect.getProperties(entity.getClass());
		return runStatements((db, conn) -> {
			boolean hasId = (props.idField != null);
			String sql = props.sqlInsert + props.sqlInsertValues;
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql, hasId  ? new String[] {props.idField.columnName} : null)) {
				bindParameters(0, entity, props, stmt, props.insertableFields);
				stmt.executeUpdate();
				if (hasId) {
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
	 * @param <T> entity type
	 * @param entities collection of entities to insert
	 * @return the same entities, with @Id field (if any) being initialized
	 * @throws SQLException when an SQL specific error occurs (see method setBatchSize() in case of large entity lists)
	 * @throws BindException when data binding fails
	 * @throws UnauthorizedException when entity saving is unauthorized (in authorize-method)
	 */
	public <T> List<T> bulkInsert(List<T> entities) throws BindException, SQLException, UnauthorizedException {
		if (entities == null || entities.isEmpty())
			return entities;
		for(T entity : entities)
			authorize(entity);
		EntityProperties props = dialect.getProperties(entities.get(0).getClass());
		return runStatements((db, conn) -> {
			for(List<T> batch : partition(entities, batchSize)) {//split the entity list into smaller batches, to avoid jdbc driver limit
				String sql = props.sqlInsert + batch.stream().map(e -> props.sqlInsertValues).collect(joining(", "));
				logger.accept(sql);
				boolean hasId = (props.idField != null);
				try(PreparedStatement stmt = conn.prepareStatement(sql, hasId  ? new String[] {props.idField.columnName} : null)) {
					int ordinal = 0;
					int batchSize = batch.size();
					for(int i = 0; i < batchSize; i++)
						ordinal = bindParameters(ordinal, batch.get(i), props, stmt, props.insertableFields);
					stmt.execute();
					if (hasId) {
						ResultSet rs = stmt.getGeneratedKeys();
						int rowIndex = 0;
						while(rs.next())
							props.idField.field.set(batch.get(rowIndex++), rs.getObject(1));
					}
				}
			}
			return entities;
		});
	}

	private <T> Collection<List<T>> partition(List<T> list, int size) {
		final AtomicInteger counter = new AtomicInteger(0);
		return list.stream().collect(groupingBy(it -> counter.getAndIncrement() / size)).values();
	}

	/**
	 * Update an existing entity in database. Only entities with @Id field can be updated. When @Id field is missing, use method update(entity, whereExpression, whereParameters)
	 * @param entity entity with new attribute values
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 * @throws RecordNotFoundException when referenced entity was not found in database
	 * @throws UnauthorizedException when entity saving is unauthorized (in authorize-method)
	 */
	public void update(Object entity) throws BindException, SQLException, RecordNotFoundException, UnauthorizedException {
		authorize(entity);
		Class<?> clazz = entity.getClass();
		EntityProperties props = dialect.getProperties(clazz);
		if (props.idField == null)
			throw new BindException("Missing @Id field in " + entity.getClass().getSimpleName());
		int count = runStatements((db, conn) -> {
			String sql = props.sqlUpdate + " WHERE " + injectIntoWhereExpression(clazz, props.sqlWhere);
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				int ordinal = bindParameters(0, entity, props, stmt, props.updatableFields);
				bindWhereParameters(stmt, ordinal, injectWhereParameters(clazz, new Object[] {props.idField.field.get(entity)}));
				return stmt.executeUpdate();
			}
		});
		if (count != 1)
			throw new RecordNotFoundException();
	}

	/**
	 * Delete an existing record
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param id entity id
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 * @throws RecordNotFoundException when referenced entity was not found in database
	 */
	public void delete(Class<?> entityClass, Object id) throws BindException, SQLException, RecordNotFoundException {
		EntityProperties props = dialect.getProperties(entityClass);
		if (props.idField == null)
			throw new BindException("Missing @Id field in " + entityClass.getSimpleName());
		if (deleteWhere(entityClass, injectIntoWhereExpression(entityClass, props.sqlWhere), injectWhereParameters(entityClass, new Object[] {id})) != 1)
			throw new RecordNotFoundException();
	}

	/**
	 * Delete multiple records
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameters for WHERE expression
	 * @return number of records deleted
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public int deleteWhere(Class<?> entityClass, String whereExpression, Object ... whereParameters) throws BindException, SQLException {
		return runStatements((db, conn) -> {
			EntityProperties props = dialect.getProperties(entityClass);
			String sql = props.sqlDelete + " WHERE " + injectIntoWhereExpression(entityClass, whereExpression);
			logger.accept(sql);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				bindWhereParameters(stmt, 0, injectWhereParameters(entityClass, whereParameters));
				return stmt.executeUpdate();
			}
		});
	}

	/**
	 * Prepare a SELECT query
	 * @param sqlSelect SQL SELECT statement
	 * @param whereParameters parameter values for WHERE expression
	 * @return query object for fetching the results
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public SqlQuery sql(String sqlSelect, Object ... whereParameters) throws BindException, SQLException {
		SqlQuery q = new SqlQuery(this, sqlSelect, logger);
		q.parameters = whereParameters;
		return q;
	}

	/**
	 * Prepare a SELECT query with WHERE filter only. Subsequent fetch operation (get or list) determines database table for this query
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameter values for WHERE expression
	 * @return query object for fetching the results
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public SqlQuery where(String whereExpression, Object ... whereParameters) throws BindException, SQLException {
		SqlQuery q = new SqlQuery(this, null, logger);
		q.whereExpression = injectIntoWhereExpression(null, whereExpression);
		q.parameters = injectWhereParameters(null, whereParameters);
		return q;
	}

	/**
	 * Fetch records from the entity-related table and return records in list
	 * @param <T> entity type
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameter values for WHERE expression
	 * @return results in list
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> List<T> listWhere(Class<? extends T> entityClass, String whereExpression, Object ... whereParameters) throws BindException, SQLException {
		SqlQuery q = new SqlQuery(this, dialect.getProperties(entityClass).sqlSelect, logger);
		q.whereExpression = injectIntoWhereExpression(entityClass, whereExpression);
		q.parameters = injectWhereParameters(entityClass, whereParameters);
		return q.list(entityClass);
	}

	/**
	 * Fetch records from the entity-related table and return records in stream
	 * @param <T> entity type
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @param whereParameters parameter values for WHERE expression
	 * @return results in stream
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> Stream<? extends T> streamWhere(Class<? extends T> entityClass, String whereExpression, Object ... whereParameters) throws BindException, SQLException {
		SqlQuery q = new SqlQuery(this, dialect.getProperties(entityClass).sqlSelect, logger);
		q.whereExpression = injectIntoWhereExpression(entityClass, whereExpression);
		q.parameters = injectWhereParameters(entityClass, whereParameters);
		return q.stream(entityClass);
	}

	/**
	 * Fetch all records/entities from a database table into a list
	 * @param <T> entity type
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @return list of entities
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 */
	public <T> List<T> listAll(Class<? extends T> entityClass) throws BindException, SQLException {
		SqlQuery q = new SqlQuery(this, dialect.getProperties(entityClass).sqlSelect, logger);
		q.whereExpression = injectIntoWhereExpression(entityClass, null);
		q.parameters = injectWhereParameters(entityClass, null);
		return q.list(entityClass);
	}

	/**
	 * Fetch a single record/entity from a database table
	 * @param <T> entity type
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param id entity id
	 * @return entity; returns null, if id refers to non-existing record
	 * @throws SQLException when an SQL specific error occurs
	 * @throws BindException when data binding fails
	 * @throws RecordNotFoundException when referenced entity was not found in database
	 */
	public <T> T getById(Class<? extends T> entityClass, Object id) throws BindException, SQLException, RecordNotFoundException {
		String sqlWhere = injectIntoWhereExpression(entityClass, dialect.getProperties(entityClass).sqlWhere);
		Object[] parameters = injectWhereParameters(entityClass, new Object[] {id});
		T entity = where(sqlWhere, parameters).fetch(entityClass);
		if (entity == null)
			throw new RecordNotFoundException();
		else return entity;
	}

	/**
	 * Override this method to manipulate SQL WHERE expression for database SELECT, UPDATE and DELETE statements.
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereExpression SQL WHERE expression, for example "name LIKE ?"
	 * @return where expression including injections
	 */
	protected String injectIntoWhereExpression(Class<?> entityClass, String whereExpression) {
		return whereExpression;
	}

	/**
	 * Override this method to manipulate SQL WHERE parameters for database SELECT, UPDATE and DELETE statements.
	 * @param entityClass entity class, which indirectly refers to a database table
	 * @param whereParameters original WHERE parameters
	 * @return where WHERE parameters with injections
	 */
	protected Object[] injectWhereParameters(Class<?> entityClass, Object[] whereParameters) {
		return whereParameters;
	}

	/**
	 * Override this method to authorize an entity before saving it to database
	 * @param entity entity to authorize
	 * @throws UnauthorizedException when trying to save unauthorized entity
	 */
	protected void authorize(Object entity) throws UnauthorizedException {
	}

	/**
	 * Runs a bunch of statements in a single transaction
	 * @param <T> entity type
	 * @param statements statements to run
	 * @return the return value from statements
	 * @throws SQLException when connection allocation, release, commit or rollback fails
	 * @throws TransactionException when a exception is thrown inside transaction logic 
	 */
	synchronized public <T> T transaction(TransactionStatements<T> statements) throws SQLException, TransactionException {
		if (txConnection != null)
			throw new TransactionException("Nested transactions are not supported");

		try {
			try {
				txConnection = connFactory.getConnection();
			} catch (SQLException se) {
				throw se;
			} catch (Exception e) {
				throw new TransactionException("Transaction failed", e);
			}
	
			try {
				txConnection.setAutoCommit(false);
				T returnValue = runStatementsEx(statements);
				txConnection.commit();
				return returnValue;
			}
			catch(TransactionException x) {
				txConnection.rollback();
				throw x;
			}
			catch(Exception x) {
				txConnection.rollback();
				throw new TransactionException("Transaction failed", x);
			}
		}
		finally {
			txConnection.close();
			txConnection = null;
		}
	}

	private <T> int bindParameters(int ordinal, T entity, EntityProperties props, PreparedStatement stmt, Collection<FieldProperties> fields) throws SQLException, BindException {
		for(FieldProperties fprops : fields)
			try {
				fprops.saveBinder.bind(stmt, ++ordinal, fprops.field.get(entity));
			}
			catch(IllegalAccessException iae) {
				throw new BindException("Unable to access field " + fprops.field.getName());
			}
		return ordinal;
	}

	void bindWhereParameters(PreparedStatement stmt, int ordinal, Object... whereParameters) throws SQLException, BindException {
		if (whereParameters != null)
			for(Object whereParam : whereParameters)
				if (whereParam == null)
					stmt.setNull(++ordinal, Types.VARCHAR);
				else
					try {
						dialect.getSaveBinder(whereParam.getClass()).bind(stmt, ++ordinal, whereParam);
					}
					catch(IllegalAccessException iae) {
						throw new BindException("Unable to access field", iae);
					}
	}

	synchronized <T> T runStatements(TransactionStatements<T> statements) throws SQLException, BindException {
		try {
			return runStatementsEx(statements);
		}
		catch(BindException | SQLException x) {
			throw x;
		}
		catch(Exception x) {
			throw new RuntimeException("Transaction failed", x);
		}
	}

	synchronized private <T> T runStatementsEx(TransactionStatements<T> statements) throws Exception {
		if (txConnection != null)
			return statements.statements(this, txConnection);//in transaction context, connection management takes place in method "transaction"

		// When not in transaction context, connection management takes place in this method
		try(Connection connection = connFactory.getConnection()) {
			return statements.statements(this, connection);
		}
	}

	public Dialect getDialect() {
		return new PgDialect();
	}

	private void initDialect() {
		Class<? extends Database> clazz = getClass();
		dialect = mapDialects.get(clazz);
		if (dialect == null)
			mapDialects.put(clazz, dialect = getDialect());
	}

	public String getSchema() {
		return schema;
	}

	public Database setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public Database setLogger(Consumer<String> logger) {
		this.logger = logger;
		return this;
	}
}
