package linqs.gaia.exception;

/**
 * Exception to throw when attempting to perform an operation
 * that is invalid.  For example, trying to remove an edge
 * which doesn't exist in the graph should throw this exception.
 * 
 * @see UnsupportedTypeException
 * @see InvalidAssignmentException
 * 
 * @author namatag
 *
 */
public class InvalidOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidOperationException() {
		super();
	}

	public InvalidOperationException(String msg) {
		super(msg);
	}
}
