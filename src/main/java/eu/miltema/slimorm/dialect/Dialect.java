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

	/**
	 * This method provides the leading part of INSERT-statement, up until VALUES-word (inclusive)
	 * @param tableName table name
	 * @param mutableColumns names of mutable columns
	 * @return part of INSERT-statement without value parenthesis, for example "INSERT INTO mytable VALUES" 
	 */
	String getSqlForInsert(String tableName, Collection<String> mutableColumns);

	/**
	 * This method provides value placeholders in parenthesis
	 * @param tableName table name
	 * @param mutableColumns names of mutable columns
	 * @return for example "(?, ?, ?)"
	 */
	String getSqlForValuesClause(String tableName, Collection<String> mutableColumns);

	String getSqlForUpdate(String tableName, Collection<String> mutableColumns);
	String getSqlForDelete(String tableName);
	String getSqlForSelect(String tableName, Collection<String> columns);
	String getSqlForWhere(String tableName, String idColumn);

	EntityProperties getProperties(Class<?> entityClass);

	LoadBinder getJSonLoadBinder(Class<?> fieldClass);
	SaveBinder getJSonSaveBinder(Class<?> fieldClass);
}
