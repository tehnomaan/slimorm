package eu.miltema.slimorm;

import java.sql.ResultSet;

/**
 * Extracts a value from ResultSet into and converts it to suitable Java type
 *
 * @author Margus
 *
 */
@FunctionalInterface
public interface LoadBinder {
	/**
	 * Extracts a value from ResultSet into and converts it to suitable Java type
	 * @param rs resultset
	 * @param index column index (1-based)
	 * @return resulting value
	 * @throws Exception when anything goes wrong
	 */
	Object convert(ResultSet rs, int index) throws Exception;
}
