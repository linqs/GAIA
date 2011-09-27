package linqs.gaia.identifiable;

/**
 * Base interface for all classes which contain a unique String ID.<br>
 * Note: The ID can only be set during construction.
 * 
 * @author namatag
 *
 */
public interface Identifiable<C extends ID> {
	/**
	 * Get the unique identifier
	 * @return The unique ID specifying this particular object.
	 */
	C getID();
}
