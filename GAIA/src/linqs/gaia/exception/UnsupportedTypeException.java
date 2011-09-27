package linqs.gaia.exception;

/**
 * Exception to throw when a received object is of an unsupported type.
 * For example, passing in a numeric feature to a model which
 * doesn't handle numeric features should throw this exception.
 * 
 * @author namatag
 *
 */
public class UnsupportedTypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnsupportedTypeException() {
		super();
	}

	public UnsupportedTypeException(String msg) {
		super(msg);
	}
	
	public UnsupportedTypeException(Object o) {
		super("Unsupported Class Type: "+o+" of type "+o.getClass().getCanonicalName());
	}
}
