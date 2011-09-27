package linqs.gaia.exception;

/**
 * Exception to throw when an assignment in the code,
 * in the constructor or when assigning feature values,
 * is invalid for the assignment.  For example, assigning
 * a string word to a numeric feature should throw this
 * exception.
 * 
 * @see InvalidOperationException
 * 
 * @author namatag
 *
 */
public class InvalidAssignmentException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidAssignmentException() {
		super();
	}

	public InvalidAssignmentException(String msg) {
		super(msg);
	}
}
