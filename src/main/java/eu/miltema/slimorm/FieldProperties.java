package eu.miltema.slimorm;

import java.lang.reflect.Field;

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
