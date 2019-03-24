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

	private Dialect dialect;
	public String tableName;
	public Collection<FieldProperties> fields = new ArrayList<>();//transient & synthetic fields excluded
	Collection<FieldProperties> insertableFields = new ArrayList<>();//cached fields to make INSERT binding faster
	Collection<FieldProperties> updatableFields = new ArrayList<>();//cached fields to make UPDATE binding faster
	public Map<String, FieldProperties> mapColumnToField = new HashMap<>(); 
	public FieldProperties idField;
	private String sqlInsert, sqlUpdate, sqlDelete, sqlSelect, sqlWhere, sqlInsertValues;

	public EntityProperties(Class<?> clazz, Dialect dialect) {
		this.dialect = dialect;
		initFields(clazz);

		Table table = clazz.getAnnotation(Table.class);
		tableName = (table != null && !table.name().isEmpty() ? table.name() : dialect.getTableName(clazz.getSimpleName()));
	}

	private void initFields(Class<?> clazz) {
		while(clazz != Object.class) {
			for(Field field : clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.TRANSIENT) != 0)
					continue;
				if (field.getAnnotation(Transient.class) != null)
					continue;
				if (field.isSynthetic())
					continue;
				field.setAccessible(true);

				FieldProperties props = new FieldProperties(field, dialect);
				
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
			if (field.isAnnotationPresent(JSon.class)) {
				props.saveBinder = dialect.getJSonSaveBinder(props.fieldType);
				props.loadBinder = dialect.getJSonLoadBinder(props.fieldType);
			}
			else if (field.isAnnotationPresent(ManyToOne.class)) {
				EntityProperties feProp = dialect.getProperties(props.fieldType);
				Class<?> feClass = feProp.idField.field.getDeclaringClass();
				props.foreignField = feProp.idField;
				SaveBinder sb = dialect.getSaveBinder(props.foreignField.fieldType);
				LoadBinder lb = dialect.getLoadBinder(props.foreignField.fieldType);
				props.saveBinder = (stmt, index, value) -> sb.bind(stmt, index, (value == null ? null : feProp.idField.field.get(value)));
				props.loadBinder = (rs, index) -> {
					Object fkeyValue = lb.convert(rs, index);
					if (fkeyValue == null)
						return null;
					Object foreignObject = feClass.newInstance();
					feProp.idField.field.set(foreignObject, fkeyValue);
					return foreignObject;
				};
			}
			else {
				props.saveBinder = dialect.getSaveBinder(field.getType());
				props.loadBinder = dialect.getLoadBinder(field.getType());
			}
			if (props.saveBinder == null)
				throw new IllegalArgumentException("Unsupported field type for field " + field.getName());
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
