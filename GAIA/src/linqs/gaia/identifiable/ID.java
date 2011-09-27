package linqs.gaia.identifiable;

import java.io.Serializable;
import java.util.regex.Pattern;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.StringFormatException;

/**
 * Base class for all unique identifiers
 * 
 * @author namatag
 *
 */
public abstract class ID implements Comparable<ID>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private static Pattern schemaidpattern = Pattern.compile("^[a-zA-Z_0-9\\-:]+$");
	private static Pattern objidpattern = Pattern.compile("^[^.,\\n\\s\\t]+$");

	protected String schemaid;
	protected String objid;

	/**
	 * Abstract constructor
	 * <p>
	 * Note: Schema ID must match the Java Regex "^[a-zA-Z_0-9\\-:]+$"
	 * and the Object ID must match the Java Regex "^[^.,\\n\\s\\t]+$".
	 * 
	 * @param schemaid Schema ID
	 * @param objid Object ID
	 */
	public ID(String schemaid, String objid){
		if(!schemaidpattern.matcher(schemaid).matches()) {
			throw new InvalidOperationException("Invalid schema id: "+schemaid);
		}

		if(!objidpattern.matcher(objid).matches()) {
			throw new InvalidOperationException("Invalid object id: "+objid);
		}

		this.schemaid = schemaid.intern();
		this.objid = objid;
	}

	/**
	 * Return Schema ID
	 * 
	 * @return Schema ID
	 */
	public String getSchemaID(){
		return this.schemaid;
	}

	/**
	 * Return object ID
	 * 
	 * @return Object ID
	 */
	public String getObjID(){
		return this.objid;
	}

	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof ID)) {
			return false;
		}

		ID newid = (ID) obj;
		return this.schemaid.equals(newid.schemaid) && this.objid.equals(newid.objid);
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.schemaid.hashCode();
		hash = hash * 31 + this.objid.hashCode();

		return hash;
	}

	public int compareTo(ID o) {
		if (o == null) {
			throw new InvalidStateException("Cannot have null object in compareTo");
		}

		return this.toString().compareTo(o.toString());
	}

	/**
	 * Parse an ID from a string in the same
	 * format as that output by toString for
	 * either GraphID or GraphItemID.
	 * This method also parses the output of toString
	 * for Nodes, Edges, and Graphs.
	 * 
	 * @param s String to parse
	 * @return ID
	 */
	public static ID parseID(String s) {
		String[] parts = s.split("\\.");

		if(parts.length==4) {
			return GraphItemID.parseGraphItemID(s);
		} else if(parts.length==5 && 
				(parts[0].equals("UNDIRECTED") 
						|| parts[0].equals("DIRECTED") 
						|| parts[0].equals("NODE"))) {
			return GraphItemID.parseGraphItemID(parts[1]+"."+parts[2]+"."+parts[3]+"."+parts[4]);
		} else if(parts.length==2) {
			return GraphID.parseGraphID(s);
		} else if(parts.length==3 && parts[0].equals("GRAPH")) {
			return GraphID.parseGraphID(parts[1]+"."+parts[2]);
		} else {
			throw new StringFormatException("Unable to parse: "+s);
		}
	}
}
