package eu.miltema.slimorm;

import java.sql.Connection;

/**
 * An interface for declaring the logic, that must be run in a transaction
 * @author Margus
 *
 */
@FunctionalInterface
public interface TransactionStatements<T> {
	public T statements(Database database, Connection connection) throws Exception;
}
