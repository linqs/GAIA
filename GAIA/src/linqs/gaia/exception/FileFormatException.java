package linqs.gaia.exception;

/**
 * Exception to throw when an input format, aside from those in configuration files,
 * is not valid.
 * 
 * @see ConfigurationException
 * 
 * @author namatag
 *
 */
public class FileFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public FileFormatException() {
		super();
	}

	public FileFormatException(String msg) {
		super(msg);
	}
}
