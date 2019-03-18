package eu.miltema.slimorm;

/**
 * Indicates that parameter binding or result binding has failed
 * @author Margus
 *
 */
public class BindException extends Exception {

	public BindException(String message) {
		super(message);
	}

	public BindException(String message, Exception cause) {
		super(message, cause);
	}
}
