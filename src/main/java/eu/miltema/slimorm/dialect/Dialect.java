package eu.miltema.slimorm.dialect;

import java.lang.reflect.Field;
import java.util.Collection;

import eu.miltema.slimorm.*;

public interface Dialect {

	/**
	 * @param fieldType field type
	 * @return load binder for a Java type
	 */
	LoadBinder getLoadBinder(Class<?> fieldType);

	/**
	 * @param fieldType field type
	 * @return save binder for a Java type
	 */
	SaveBinder getSaveBinder(Class<?> fieldType);

	/**
	 * @param clazz entity class
	 * @return database table name for this Java class
	 */
	String getTableName(Class<?> clazz);

	/**
	 * @param field entity field
	 * @return column name for this field
	 */
	String getColumnName(Field field);

	String getSqlForInsert(String tableName, Collection<String> mutableColumns);
	String getSqlForUpdate(String tableName, Collection<String> mutableColumns);
	String getSqlForDelete(String tableName);
	String getSqlForSelect(String tableName, Collection<String> columns);
	String getSqlForWhere(String tableName, String idColumn);

	EntityProperties getProperties(Class<?> entityClass);
}
