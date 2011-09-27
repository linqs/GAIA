package linqs.gaia.exception;

/**
 * Exception to throw whenever a function, which was not defined for a
 * variety of reasons (i.e., Java class is incomplete), is called.
 * These should be avoided as much as possible and should not be included
 * in any official releases of the code.  However, if a case does
 * arise where the code needs to be released with a function undefined,
 * make sure to include this to make sure that the condition
 * is known.
 * 
 * @author namatag
 *
 */
public class UndefinedFunctionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UndefinedFunctionException() {
		super();
	}

	public UndefinedFunctionException(String msg) {
		super(msg);
	}
}
