package linqs.gaia.exception;

/**
 * Exception to throw when a string value is not in the expected format.
 * 
 * @author namatag
 *
 */
public class StringFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public StringFormatException() {
		super();
	}

	public StringFormatException(String msg) {
		super(msg);
	}
}
