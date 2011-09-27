package linqs.gaia.model.oc.active;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;

/**
 * Performs multi task active learning by applying an active learning
 * algorithm for each task and aggregating the results.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>alclass-Active learning class to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>aggregationtype-Method to aggregate the queries
 * <UL>
 * <LI>even-Apply active learning for each task and returns k/t queries for each where
 * k is the requested number of queries and t is the specified number of tasks.
 * <LI>fullmerge-Apply active learning for each task and merge the results, sorting by
 * score.  The top scores, overall, among all the queries will be returned.
 * </UL>
 * </UL>
 * 
 * @author namatag
 *
 */
public class NaiveMultiTask extends BaseConfigurable implements MultiTaskActiveLearning {
	private List<String> schemaids = null;
	private List<String> featureids = null;
	private List<ActiveLearning> als = null;
	private String type = "even";
	
	public void initialize(List<String> targetschemaids,
			List<String> targetfeatureids) {
		if(targetschemaids.size()!=targetfeatureids.size()) {
			throw new InvalidStateException("List size must match: "+
					targetschemaids.size()+"!="+targetfeatureids.size());
		}
		
		int size = targetschemaids.size();
		this.schemaids = targetschemaids;
		this.featureids = targetfeatureids;
		this.type = this.getCaseParameter("aggregationtype", new String[]{"even","fullmerge"});
		
		als = new ArrayList<ActiveLearning>(this.schemaids.size());
		String alclass = this.getStringParameter("alclass");
		for(int i=0; i<size; i++) {
			ActiveLearning al = (ActiveLearning) Dynamic.forConfigurableName(ActiveLearning.class,
				alclass, this);
			al.initialize(schemaids.get(i), featureids.get(i));
			als.add(al);
		}
		
	}

	public List<Query> getQueries(Graph g,
			int numqueries) {
		List<Iterable<? extends Decorable>> testitems =
			new ArrayList<Iterable<? extends Decorable>>();
		for(String sid:schemaids) {
			testitems.add(IteratorUtils.iterator2graphitemlist(g.getGraphItems(sid)));
		}
		
		return this.getQueries(testitems, numqueries);
	}

	public List<Query> getQueries(
			List<Iterable<? extends Decorable>> testitems, int numqueries) {
		if(featureids.size()!=testitems.size()) {
			throw new InvalidStateException("List size must match: "+
					featureids.size()+"!="+testitems.size());
		}
		
		if(numqueries==0) {
			return new ArrayList<Query>();
		}
		
		List<Query> queries = null;
		int numsets = featureids.size();
		
		if(this.type.equals("even")) {
			// Take an even sampling of each
			int numeach = numqueries / testitems.size();
			int numextra = numqueries % testitems.size();
			
			queries = new ArrayList<Query>(numqueries);
			for(int i=0; i<numsets; i++) {
				ActiveLearning al = als.get(i);
				int currnumeach = numeach;
				if(numextra>0) {
					currnumeach++;
					numextra--;
				}
				
				if(currnumeach==0) {
					continue;
				}
				
				List<Query> dlist = al.getQueries(testitems.get(i), currnumeach);
				queries.addAll(dlist);
			}
			
			// Sort descending
			Collections.sort(queries,Collections.reverseOrder());
		} else {
			// Query all things and filter as one big list
			queries = new ArrayList<Query>(numqueries);
			for(int i=0; i<numsets; i++) {
				ActiveLearning al = als.get(i);
				List<Query> dlist = al.getQueries(testitems.get(i), numqueries);
				queries.addAll(dlist);
			}
			
			// Sort descending
			Collections.sort(queries,Collections.reverseOrder());
			
			// If there are less queries than the desired,
			// just return all the queries.  Otherwise, we need to filter.
			if(queries.size()>numqueries) {
				queries = queries.subList(0, numqueries);
			}
		}
		
		return queries;
	}
}
