package eu.miltema.slimorm;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter binder for INSERT, UPDATE and DELETE prepared statements
 * @author Margus
 *
 */
@FunctionalInterface
public interface SaveBinder {

	/**
	 * Bind a parameter to the prepared statement
	 * @param stmt prepared statement
	 * @param index parameter index (1-based)
	 * @param value parameter value
	 * @throws SQLException when anything goes wrong
	 */
	void bind(PreparedStatement stmt, int index, Object value) throws SQLException, IllegalAccessException;
}
