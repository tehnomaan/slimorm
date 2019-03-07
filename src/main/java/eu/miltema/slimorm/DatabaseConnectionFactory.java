package eu.miltema.slimorm;

import java.sql.Connection;

/**
 * Factory class for database connections
 * @author Margus
 *
 */
@FunctionalInterface
public interface DatabaseConnectionFactory {

	Connection getConnection() throws Exception;
}
