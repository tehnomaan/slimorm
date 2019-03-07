package eu.miltema.slimorm;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter binder for INSERT, UPDATE and DELETE prepared statements
 * @author Margus
 *
 */
@FunctionalInterface
interface SaveBinder {
	void bind(PreparedStatement stmt, int i, Object param) throws SQLException;
}
