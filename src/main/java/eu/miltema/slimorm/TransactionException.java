package eu.miltema.slimorm;

public class TransactionException extends Exception {
	
	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(String message, Exception x) {
		super(message, x);
	}

}
