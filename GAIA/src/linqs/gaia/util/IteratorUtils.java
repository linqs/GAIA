package linqs.gaia.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;

/**
 * Utilities to simplify handling iterators
 * 
 * @author namatag
 *
 */
public class IteratorUtils<C> {
	/**
	 * Return the number of items returned by iterator
	 * 
	 * @param itr Iterator
	 * @return Number of items returned by iterator
	 */
	public static int numIterable(Iterator<?> itr) {
		int size = 0;
		while(itr.hasNext()){
			itr.next();
			size++;
		}
		
		return size;
	}
	
	/**
	 * Return the number of items in the iterable object
	 * 
	 * @param itrbl Iterable object
	 * @return Number of items in iterable object
	 */
	public static int numIterable(Iterable<?> itrbl) {
		if(itrbl instanceof Collection) {
			// If iterable object is a collection,
			// just use the size of the collection
			return ((Collection<?>) itrbl).size();
		} else if(itrbl instanceof UnmodifiableList) {
			return ((UnmodifiableList<?>) itrbl).size();
		} else if(itrbl instanceof UnmodifiableSet) {
			return ((UnmodifiableSet<?>) itrbl).size();
		} else {
			// If needed, just iterate over list
			return numIterable(itrbl.iterator()) ;
		}
	}
	
	/**
	 * Return a String representation of the items
	 * the Iterator is over.
	 * 
	 * @param itr Iterator
	 * @param delimiter String delimiter
	 * @return String representation
	 */
	public static String iterator2string(Iterator<?> itr, String delimiter){
		String output = null;
		while(itr.hasNext()){
			if(output==null){
				output="";
			} else {
				output+=delimiter;
			}
			
			output+=itr.next();
		}
			
		return output;
	}
	
	/**
	 * Create a list over all iterable objects
	 * <p>
	 * Note: Only use this when you know the size of the list will be small.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List iterator2list(Iterator<?> itr) {
		List list = new LinkedList();
		while(itr.hasNext()) {
			list.add(itr.next());
		}
		
		return list;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static List<String> iterator2stringlist(Iterator<String> itr) {
		List<String> list = new LinkedList<String>();
		while(itr.hasNext()) {
			list.add(itr.next());
		}
		
		return list;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * when turning an iterator over nodes into a list of nodes.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static List<Node> iterator2nodelist(Iterator<?> itr) {
		List<Node> list = new LinkedList<Node>();
		while(itr.hasNext()) {
			list.add((Node) itr.next());
		}
		
		return list;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * when turning an iterator over edges into a list of graph items.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static List<GraphItem> iterator2graphitemlist(Iterator<?> itr) {
		List<GraphItem> list = new LinkedList<GraphItem>();
		while(itr.hasNext()) {
			list.add((GraphItem) itr.next());
		}
		
		return list;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * when turning an iterator over edges into a list of decorable items.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static List<Decorable> iterator2decorablelist(Iterator<?> itr) {
		List<Decorable> list = new LinkedList<Decorable>();
		while(itr.hasNext()) {
			list.add((Decorable) itr.next());
		}
		
		return list;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * when turning an iterator over nodes into a set of nodes.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static Set<Node> iterator2nodeset(Iterator<?> itr) {
		Set<Node> set = new HashSet<Node>();
		while(itr.hasNext()) {
			set.add((Node) itr.next());
		}
		
		return set;
	}
	
	/**
	 * Wrapper over iterator2list to avoid type warnings
	 * when turning an iterator over edges into a list of edges.
	 * 
	 * @param itr Iterator
	 * @return List
	 */
	public static List<Edge> iterator2edgelist(Iterator<?> itr) {
		List<Edge> list = new LinkedList<Edge>();
		while(itr.hasNext()) {
			list.add((Edge) itr.next());
		}
		
		return list;
	}
}
