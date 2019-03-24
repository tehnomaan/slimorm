package eu.miltema.slimorm;

import java.lang.reflect.Field;

import javax.persistence.*;

import eu.miltema.slimorm.dialect.Dialect;

/**
 * Represents field-related properties, when accessing database
 *
 * @author Margus
 *
 */
public class FieldProperties {
	public String columnName;
	public Field field;
	public boolean updatable = true;
	public boolean insertable = true;
	public SaveBinder saveBinder;
	public LoadBinder loadBinder;
	public Class<?> fieldType;
	public FieldProperties foreignField;//only present when the field has @ManyToOne annotation

	public FieldProperties(Field field, Dialect dialect) {
		this.field = field;
		this.fieldType = field.getType();

		Column column = field.getAnnotation(Column.class);
		this.columnName = (column != null && !column.name().isEmpty() ? column.name() : dialect.getColumnName(field.getName() + (field.isAnnotationPresent(ManyToOne.class) ? "Id" : "")));

		if (column != null) {
			this.insertable = column.insertable();
			this.updatable = column.updatable();
		}

		if (field.isAnnotationPresent(GeneratedValue.class))
			this.insertable = this.updatable = false;
	}

	public String toString() {
		return field.getName() + "/" + columnName;
	}

	public Object getFieldValue(Object object) {
		try {
			return field.get(object);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public void setFieldValue(Object object, Object value) {
		try {
			field.set(object, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
