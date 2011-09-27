package linqs.gaia.feature.schema;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;

/**
 * Type of {@link Schema} which corresponds to the type
 * of {@link Decorable} object this {@link Schema}
 * is defined for in the {@link Graph}.
 * 
 * @see Schema
 * 
 * @author namatag
 *
 */
public enum SchemaType {
	GRAPH,
	NODE,
	DIRECTED,
	UNDIRECTED
}
