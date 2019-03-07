package eu.miltema.slimorm;

import java.sql.*;
import java.util.*;

import javax.naming.*;
import javax.sql.DataSource;

/**
 * Database connector
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
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public Database(String driverName, String jdbcUrl, String username, String password) throws Exception {
		Class.forName(driverName).newInstance();
		connFactory = () -> DriverManager.getConnection(jdbcUrl, username, password);
	}

	public <T> T insert(T entity) throws Exception {
		runStatements((db, conn) -> {
			EntityProperties props = getProperties(entity.getClass());
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
		return entity;
	}

	public <T> T update(T entity) throws Exception {
		EntityProperties props = getProperties(entity.getClass());
		if (props.idField == null)
			throw new Exception("Missing @Id field in " + entity.getClass().getSimpleName());
		return update(entity, props.sqlWhere, props.idField.field.get(entity));
	}

	public <T> T update(T entity, String whereExpression, Object ... whereParameters) throws Exception {
		runStatements((db, conn) -> {
			EntityProperties props = getProperties(entity.getClass());
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlUpdate + " WHERE " + whereExpression)) {
				int ordinal = bindMutableParameters(entity, props, stmt);
				if (whereParameters != null)
					for(Object whereParam : whereParameters)
						if (whereParam == null)
							stmt.setNull(++ordinal, Types.VARCHAR);
						else JdbcBinders.instance.saveBinders.get(whereParam.getClass()).bind(stmt, ++ordinal, whereParam);
				stmt.executeUpdate();
			}
			return entity;
		});
		return entity;
	}
	
	public boolean delete(Class<?> entityClass, Object id) throws Exception {
		EntityProperties props = getProperties(entityClass);
		if (props.idField == null)
			throw new Exception("Missing @Id field in " + entityClass.getSimpleName());
		return delete(entityClass, props.sqlWhere, id);
	}
	
	public boolean delete(Class<?> entityClass, String whereExpression, Object ... whereParameters) throws Exception {
		return runStatements((db, conn) -> {
			EntityProperties props = getProperties(entityClass);
			try(PreparedStatement stmt = conn.prepareStatement(props.sqlDelete + " WHERE " + whereExpression)) {
				int ordinal = 0;
				if (whereParameters != null)
					for(Object whereParam : whereParameters)
						if (whereParam == null)
							stmt.setNull(++ordinal, Types.VARCHAR);
						else JdbcBinders.instance.saveBinders.get(whereParam.getClass()).bind(stmt, ++ordinal, whereParam);
				return stmt.executeUpdate();
			}
		}) > 0;
	}

	private <T> int bindMutableParameters(T entity, EntityProperties props, PreparedStatement stmt) throws SQLException, IllegalAccessException {
		int ordinal = 0;
		for(FieldProperties fprops : props.mutableFields)
			fprops.saveBinder.bind(stmt, ++ordinal, fprops.field.get(entity));
		return ordinal;
	}

	private EntityProperties getProperties(Class<?> entityClass) {
		EntityProperties props = entityProps.get(entityClass);
		if (props == null)
			entityProps.put(entityClass, props = new EntityProperties(entityClass));
		return props;
	}

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

	private <T> T runStatements(TransactionStatements<T> statements) throws Exception {
		if (txConnection != null)
			return statements.statements(this, txConnection);//in transaction context, connection management takes place in method transaction

		// When not in transaction context, connection management takes place in this method
		try(Connection connection = connFactory.getConnection()) {
			return statements.statements(this, connection);
		}
	}
}
