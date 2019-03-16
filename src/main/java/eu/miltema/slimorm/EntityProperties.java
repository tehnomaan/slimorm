package eu.miltema.slimorm;

import java.lang.reflect.*;
import java.util.*;
import static java.util.stream.Collectors.*;

import javax.persistence.*;

import eu.miltema.slimorm.annot.JSon;
import eu.miltema.slimorm.dialect.Dialect;

/**
 * Represents field-related properties, when accessing database.
 * This class is essentially a cache for these properties, since the initialization of the properties can be time-consuming.
 *
 * @author Margus
 *
 */
public class EntityProperties {

	public String tableName;
	public Collection<FieldProperties> fields = new ArrayList<>();//transient & synthetic fields excluded
	Collection<FieldProperties> mutableFields = new ArrayList<>();//transient & synthetic fields excluded
	public Map<String, FieldProperties> mapColumnToField = new HashMap<>(); 
	public FieldProperties idField;
	String sqlInsert, sqlUpdate, sqlDelete, sqlSelect, sqlWhere, sqlInsertValues;

	public EntityProperties(Class<?> clazz, Dialect dialect) {
		initFields(clazz, dialect);
		tableName = dialect.getTableName(clazz);
		initSqlStatements(clazz, dialect);
	}

	private void initFields(Class<?> clazz, Dialect dialect) {
		while(clazz != Object.class) {
			for(Field field : clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.TRANSIENT) != 0)
					continue;
				if (field.getAnnotation(Transient.class) != null)
					continue;
				if (field.isSynthetic())
					continue;
				field.setAccessible(true);

				FieldProperties props = new FieldProperties();
				props.field = field;
				props.fieldType = field.getType();
				props.columnName = dialect.getColumnName(field);
				if (field.getAnnotation(Id.class) != null) {
					idField = props;
					props.isMutable = false;
				}
				if (field.isAnnotationPresent(JSon.class))
					props.saveBinder = dialect.getJSonSaveBinder(props.fieldType);
				else props.saveBinder = dialect.getSaveBinder(field.getType());
				if (props.saveBinder == null)
					throw new RuntimeException("Unsupported field type for field " + field.getName());
				if (field.isAnnotationPresent(JSon.class))
					props.loadBinder = dialect.getJSonLoadBinder(props.fieldType);
				else props.loadBinder = dialect.getLoadBinder(field.getType());
				fields.add(props);
				if (props.isMutable)
					mutableFields.add(props);
				mapColumnToField.put(props.columnName, props);
			}
			clazz = clazz.getSuperclass();
		}
	}

	public void initSqlStatements(Class<?> clazz, Dialect dialect) {
		Collection<String> mutableColumns = mutableFields.stream().map(field -> field.columnName).collect(toList());
		Collection<String> columns = fields.stream().map(field -> field.columnName).collect(toList());
		sqlInsert = dialect.getSqlForInsert(tableName, mutableColumns);
		sqlInsertValues = dialect.getSqlForValuesClause(tableName, mutableColumns);
		sqlUpdate = dialect.getSqlForUpdate(tableName, mutableColumns);
		sqlDelete = dialect.getSqlForDelete(tableName);
		sqlSelect = dialect.getSqlForSelect(tableName, columns);
		if (idField != null)
			sqlWhere = dialect.getSqlForWhere(tableName, idField.columnName);
	}
}
