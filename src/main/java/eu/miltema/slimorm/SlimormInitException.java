package eu.miltema.slimorm;

import java.lang.reflect.Field;

public class SlimormInitException extends RuntimeException {

	private Class<?> clazz;
	private Field field;

	public SlimormInitException(Class<?> clazz, String message, Throwable cause) {
		super(message, cause);
		this.clazz = clazz;
	}

	public SlimormInitException(Field field, String message, Throwable cause) {
		super(message, cause);
		this.field = field;
		this.clazz = field.getDeclaringClass();
	}

	public String getMessage() {
		return super.getMessage() + " [" +
				(clazz == null ? "" : clazz.getSimpleName() + "/") +
				(field == null ? "" : field.getName()) + "]";
	}

	void setCausingField(Field field) {
		this.field = field;
	}

	void setCausingClass(Class<?> clazz) {
		this.clazz = clazz;
	}
}
