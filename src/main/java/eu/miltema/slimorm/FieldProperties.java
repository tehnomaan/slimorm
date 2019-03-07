package eu.miltema.slimorm;

import java.lang.reflect.Field;

public class FieldProperties {
	public String columnName;
	public Field field;
	public boolean isMutable = true;
	public SaveBinder saveBinder;
	public LoadBinder loadBinder;

	public String toString() {
		return field.getName() + "/" + columnName + "/mutable=" + isMutable;
	}
}
