package linqs.gaia.graph.event;

/**
 * For use to call a custom event with a single string
 * message.
 * 
 * @author namatag
 *
 */
public class CustomEvent implements GraphEvent {
	private String message;
	
	/**
	 * Constructor
	 * 
	 * @param message Message for custom event
	 */
	public CustomEvent(String message) {
		this.message = message;
	}
	
	/**
	 * Return the message
	 * 
	 * @return Message for event
	 */
	public String getMessage() {
		return this.message;
	}
}
