package linqs.gaia.feature.derived.structural;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.adjmatrix.AdjacencyMatrix;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.SimpleTimer;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Get the Katz score of a given pair of nodes where Katz score
 * is defined as the weighted sum of all paths of length 1 to infinity.
 * It is computed by solving the matrix Katz = (I - (Beta*M))^(-1)-I
 * from Liben-Nowell, D. & Kleinberg, J.,
 * The link prediction problem for social networks, 
 * International Conference on Information and Knowledge Management, 2003.
 * <p>
 * Note: All Katz score are computed during the first call to this feature.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> adjmatrixclass-Adjacency Matrix exporter to use to create the the adjaceny matrix.
 * By default, use an Adjacency Matrix exporter with the default settings.
 * <LI> beta-Beta parameter to use in Katz score.  If null,
 * beta is set to 1.5 times the largest eigenvalue of the adjacency matrix.
 * Note that 1/beta must be larger than the largest eigenvalue of the adjacency matrix.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeKatz extends DerivedNum implements GraphDependent {
	private Graph g = null;
	private boolean initialize = true;
	private Map<ID,Integer> nodeindex = new HashMap<ID,Integer>();
	private double[][] katzmatrix = null;
	
	private void initialize() {
		initialize = false;
		
		AdjacencyMatrix exporter = null;
		String adjmatrixclass = AdjacencyMatrix.class.getCanonicalName();
		if(this.hasParameter("adjmatrixclass")) {
			adjmatrixclass = this.getStringParameter("adjmatrixclass");
		}
		
		Double beta = null;
		if(this.hasParameter("beta")) {
			beta = this.getDoubleParameter("beta");
		}
		
		exporter = (AdjacencyMatrix) Dynamic.forConfigurableName(AdjacencyMatrix.class, adjmatrixclass, this);
		
		SimpleTimer timer = new SimpleTimer();
		SimplePair<Map<ID,Integer>, double[][]> pair =  exporter.exportGraphWithNodeMap(g);
		this.nodeindex = pair.getFirst();
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Time to convert graph to adjacency matrix for Katz Score computation: "+timer.timeLapse());
		}
		
		timer.start();
		double[][] adjacencymatrix = pair.getSecond();
		katzmatrix = EdgeNodeKatz.computeKatzMatrix(adjacencymatrix, beta);
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Time to compute Katz Matrix: "+timer.timeLapse());
		}
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only valid for edges: "+
					di.getClass().getCanonicalName());
		}
		
		Edge e = (Edge) di;
		if(e.numNodes()!=2) {
			throw new UnsupportedTypeException("Only binary edges supported: "+
					e.numNodes());
		}
		
		// Get nodes
		Iterator<Node> nitr = e.getAllNodes();
		Node n1 = nitr.next();
		Node n2 = nitr.next();
		
		return new NumValue(this.getKatzScore(n1, n2));
	}
	
	private double getKatzScore(Node n1, Node n2) {
		int index1 = nodeindex.get(n1.getID());
		int index2 = nodeindex.get(n2.getID());
		
		if(index1==-1) {
			throw new InvalidStateException("Node not in nodes list: "+n1);
		}
		
		if(index2==-1) {
			throw new InvalidStateException("Node not in nodes list: "+n2);
		}
		
		return this.katzmatrix[index1][index2];
	}
	
	/**
	 * Get the Katz score of a given pair of nodes where Katz score
	 * is defined as the weighted sum of all paths of length 1 to infinity.
	 * It is computed by solving the matrix Katz = (I - (Beta*M))^(-1)-I
	 * from Liben-Nowell, D. & Kleinberg, J.,
	 * The link prediction problem for social networks, 
	 * International Conference on Information and Knowledge Management, 2003.
	 * 
	 * @param adjacencymatrix Adjacency matrix of graph
	 * @param beta Beta parameter to use in Katz score.  If null,
	 * beta is set to 1.5 times the largest eigenvalue of the adjacency matrix.
	 * Note that 1/beta must be larger than the largest eigenvalue of the adjacency matrix.
	 * @return Matrix of Katz scores
	 */
	public static double[][] computeKatzMatrix(double[][] adjacencymatrix, Double beta) {
		Matrix m = new Matrix(adjacencymatrix);
		
		// This is not valid for beta where beta is less than
		// the largest eigenvalue of the adjacency matrix
		EigenvalueDecomposition evd = new EigenvalueDecomposition(m);
		
		// Currently don't support eigenvalues with imaginary parts
		double imag[] = evd.getImagEigenvalues();
		for(double i:imag) {
			if(i!=0.0) {
				throw new UnsupportedTypeException("Only real valued eigenvalues supported: "
						+ArrayUtils.array2String(imag, ","));
			}
		}
		
		// Get the real valued eigenvalues
		double real[] = evd.getRealEigenvalues();
		double maxev = real[ArrayUtils.maxValueIndex(real)];
		if(beta==null) {
			beta = 1/(maxev*(1.5));
			Log.DEBUG("Beta set to: "+beta);
		}
		
		evd = null;
		
		// Verify that the beta is valid
		if((1.0 / beta)<maxev) {
			Log.WARN("1.0/Beta must be below largest eigenvalue of the adjacency matrix: "
					+(1.0 / beta)+" < "+maxev);
		}
		
		// Compute Katz score matrix
		// Katz = (I - (Beta*M))^(-1)-I
		// from Liben-Nowell, D. & Kleinberg, J.
		// The link prediction problem for social networks
		// International Conference on Information and Knowledge Management, 2003
		int size = adjacencymatrix.length;
		Matrix i = Matrix.identity(size, size);
		Matrix katzmatrix = i.minus(m.times(beta)).inverse().minus(i);
		
		return katzmatrix.getArray();
	}

	public void setGraph(Graph g) {
		this.g = g;
	}
	
	/**
	 * If the matrix is symmetric, return true.  Otherwise return false.
	 * 
	 * @param matrix Matrix to test symmetry of
	 * @return True if the matrix is symmetric and false otherwise
	 */
	public static boolean isSymmetric(double[][] matrix) {
		int length = matrix.length;
		
		for(int i=0; i<length; i++) {
			for(int j=i+1; j<length; j++) {
				if(matrix[i][j]!=matrix[j][i]) {
					Log.DEBUG("Matrix is assymmetric: "+matrix[i][j]+"!="+matrix[j][i]);
					return false;
				}
			}
		}
		
		return true;
	}
}