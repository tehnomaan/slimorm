package eu.miltema.slimorm;

import java.sql.ResultSet;

/**
 * Binds a column from ResultSet into an entity field
 *
 * @author Margus
 *
 */
@FunctionalInterface
public interface LoadBinder {
	Object convert(ResultSet rs, int i) throws Exception;
}
