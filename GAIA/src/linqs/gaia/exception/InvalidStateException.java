package linqs.gaia.exception;

/**
 * Exception to throw when the graph, or any other object,
 * is in a state that should not be valid given the design
 * of the code.  For example, finding an edge not adjacent
 * to any node should throw this exception.
 * 
 * @author namatag
 *
 */
public class InvalidStateException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidStateException() {
		super();
	}

	public InvalidStateException(String msg) {
		super(msg);
	}
}
