package eu.miltema.slimorm.dialect;

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
	 * @param javaName entity class name
	 * @return database table name for this Java class
	 */
	String getTableName(String javaName);

	/**
	 * @param javaName java feld name
	 * @return column name for this field
	 */
	String getColumnName(String javaName);

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

	LoadBinder getJSonLoadBinder(Class<?> fieldType);
	SaveBinder getJSonSaveBinder(Class<?> fieldType);
	SaveBinder getEnumSaveBinder(Class<?> fieldType);
	LoadBinder getEnumLoadBinder(Class<?> fieldType);
}
