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

	public String toString() {
		return field.getName() + "/" + columnName;
	}
}
