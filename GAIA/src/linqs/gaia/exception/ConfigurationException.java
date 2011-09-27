package linqs.gaia.exception;

/**
 * Exception to throw when there is an invalid or missing configuration
 * parameter in a Configurable object.
 * 
 * @see linqs.gaia.configurable.Configurable
 * 
 * @author namatag
 *
 */
public class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ConfigurationException() {
		super();
	}

	public ConfigurationException(String msg) {
		super(msg);
	}
}
