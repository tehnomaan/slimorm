package eu.miltema.slimorm;

import java.lang.reflect.*;
import java.util.*;
import static java.util.stream.Collectors.*;

import javax.persistence.*;

public class EntityProperties {

	private String tableName;
	Collection<FieldProperties> fields = new ArrayList<>();//transient fields excluded
	Collection<FieldProperties> mutableFields = new ArrayList<>();//transient fields excluded
	FieldProperties idField;
	String sqlInsert, sqlUpdate, sqlDelete, sqlWhere;

	public EntityProperties(Class<?> clazz) {
		initFields(clazz);
		initTableName(clazz);
		initSqlStatements(clazz);
	}

	private void initFields(Class<?> clazz) {
		while(clazz != Object.class) {
			for(Field field : clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.TRANSIENT) != 0)
					continue;
				if (field.getAnnotation(Transient.class) != null)
					continue;
				field.setAccessible(true);

				FieldProperties props = new FieldProperties();
				props.field = field;
				Column column = field.getAnnotation(Column.class);
				props.columnName = (column != null && !column.name().isEmpty() ? column.name() : toSnakeCase(field.getName()));
				if (field.getAnnotation(Id.class) != null) {
					idField = props;
					props.isMutable = false;
				}
				props.saveBinder = JdbcBinders.instance.saveBinders.get(field.getType());
				if (props.saveBinder == null)
					throw new RuntimeException("Unsupported field type for field " + field.getName());
				fields.add(props);
				if (props.isMutable)
					mutableFields.add(props);
			}
			clazz = clazz.getSuperclass();
		}
	}

	private String toSnakeCase(String s) {
		return s.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
		
	}

	private void initTableName(Class<?> clazz) {
		Table table = clazz.getAnnotation(Table.class);
		tableName = (table != null && !table.name().isEmpty() ? table.name() : toSnakeCase(clazz.getSimpleName()));
	}

	public void initSqlStatements(Class<?> clazz) {
		Collection<String> mutableColumns = mutableFields.stream().map(field -> field.columnName).collect(toList());
		sqlInsert = "INSERT INTO " + tableName + "(" +
				mutableColumns.stream().collect(joining(",")) +
				") VALUES (" +
				mutableColumns.stream().map(column -> "?").collect(joining(",")) +")";
		sqlUpdate = "UPDATE " + tableName + " SET " +
				mutableColumns.stream().map(column -> column + "=?").collect(joining(","));
		sqlDelete = "DELETE FROM " + tableName;
		if (idField != null)
			sqlWhere = idField.columnName + "=?";
	}
}
