package eu.miltema.slimorm;

import java.lang.reflect.*;
import java.util.*;
import static java.util.stream.Collectors.*;

import javax.persistence.*;

import eu.miltema.slimorm.dialect.Dialect;

/**
 * Represents field-related properties, when accessing database.
 * This class is essentially a cache for these properties, since the initialization of the properties can be time-consuming.
 *
 * @author Margus
 *
 */
public class EntityProperties {

	private Class<?> clazz;
	private Dialect dialect;
	public String tableName;
	public Collection<FieldProperties> fields = new ArrayList<>();//transient & synthetic fields excluded
	Collection<FieldProperties> insertableFields = new ArrayList<>();//cached fields to make INSERT binding faster
	Collection<FieldProperties> updatableFields = new ArrayList<>();//cached fields to make UPDATE binding faster
	public Map<String, FieldProperties> mapColumnToField = new HashMap<>(); 
	public FieldProperties idField;
	private String sqlInsert, sqlUpdate, sqlDelete, sqlSelect, sqlWhere, sqlInsertValues;

	public EntityProperties(Class<?> clazz, Dialect dialect) {
		this.clazz = clazz;
		this.dialect = dialect;
		initFields();
		tableName = dialect.getTableName(clazz);
	}

	private void initFields() {
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
				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					props.insertable = column.insertable();
					props.updatable = column.updatable();
				}
				if (field.isAnnotationPresent(GeneratedValue.class))
					props.insertable = props.updatable = false;
				
				if (field.getAnnotation(Id.class) != null) {
					idField = props;
					idField.updatable = false;//by definition, primary keys are immutable. So, override the value
				}

				fields.add(props);
				mapColumnToField.put(props.columnName, props);
			}
			clazz = clazz.getSuperclass();
		}
		if (idField == null && mapColumnToField.containsKey("id")) {//if no @Id-field was present, make the field with name "id" as the primary key field and assume, its value is auto-generated
			idField = mapColumnToField.get("id");
			idField.insertable = idField.updatable = false;
		}
		for(FieldProperties fprop : fields) {
			if (fprop.insertable)
				insertableFields.add(fprop);
			if (fprop.updatable)
				updatableFields.add(fprop);
		}
		if (idField != null && fields.size() == 1)
			throw new IllegalArgumentException("No other persistable fields found except @Id field");
		else if (idField == null && fields.size() == 0)
			throw new IllegalArgumentException("No persistable fields found");
	}

	/**
	 * Method initSqlStatements cannot be invoked from constructor - otherwise inter-entity circular references would cause infinite initSql-loops
	 */
	private EntityProperties initSqlStatements() {
		for(FieldProperties props : fields) {
			Field field = props.field;
			if (field.isAnnotationPresent(JSon.class))
				props.saveBinder = dialect.getJSonSaveBinder(props.fieldType);
	//		else if (field.isAnnotationPresent(ManyToOne.class))
	//			props.saveBinder = getFkeySaveBinder(props, dialect);
			else props.saveBinder = dialect.getSaveBinder(field.getType());
			if (props.saveBinder == null)
				throw new IllegalArgumentException("Unsupported field type for field " + field.getName());
			if (field.isAnnotationPresent(JSon.class))
				props.loadBinder = dialect.getJSonLoadBinder(props.fieldType);
			else props.loadBinder = dialect.getLoadBinder(field.getType());
		}

		Collection<String> insertColumns = insertableFields.stream().map(field -> field.columnName).collect(toList());
		Collection<String> updateColumns = updatableFields.stream().map(field -> field.columnName).collect(toList());
		Collection<String> columns = fields.stream().map(field -> field.columnName).collect(toList());
		sqlInsert = dialect.getSqlForInsert(tableName, insertColumns);
		sqlInsertValues = dialect.getSqlForValuesClause(tableName, insertColumns);
		sqlUpdate = dialect.getSqlForUpdate(tableName, updateColumns);
		sqlDelete = dialect.getSqlForDelete(tableName);
		sqlSelect = dialect.getSqlForSelect(tableName, columns);
		if (idField != null)
			sqlWhere = dialect.getSqlForWhere(tableName, idField.columnName);

		return this;
	}

	public String getSqlInsert() {
		return (sqlInsert == null ? initSqlStatements().sqlInsert : sqlInsert);
	}
	public String getSqlUpdate() {
		return (sqlUpdate == null ? initSqlStatements().sqlUpdate : sqlUpdate);
	}
	public String getSqlDelete() {
		return (sqlDelete == null ? initSqlStatements().sqlDelete : sqlDelete);
	}
	public String getSqlSelect() {
		return (sqlSelect == null ? initSqlStatements().sqlSelect : sqlSelect);
	}
	public String getSqlWhere() {
		return (sqlWhere == null ? initSqlStatements().sqlWhere : sqlWhere);
	}
	public String getSqlInsertValues() {
		return (sqlInsertValues == null ? initSqlStatements().sqlInsertValues : sqlInsertValues);
	}
}
