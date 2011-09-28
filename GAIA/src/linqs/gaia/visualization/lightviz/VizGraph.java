/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.visualization.lightviz;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import linqs.gaia.visualization.lightviz.prefuse.custom.CDataColorAction;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.SizeAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.BalloonTreeLayout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.FruchtermanReingoldLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.action.layout.graph.SquarifiedTreeMapLayout;
import prefuse.activity.Activity;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.ToolTipControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.ui.UILib;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

/**
 * GAIA Visual Graph - Light Version
 * <p>
 * Supported layouts include:
 * <UL>
 * <LI>RadialTreeLayout
 * <LI>ForceDirectedLayout
 * <LI>BallonTreeLayout
 * <LI>NodeLinkTreeLayout
 * <LI>FruchtermanReingoldLayout
 * <LI>SquarifiedTreeMapLayout
 * </UL>
 * See Prefuse documentation for details.
 *
 * @version 1.0
 * @author <a href="http://www.cs.umd.edu/~hossam">Hossam Sharara</a>
 */
public class VizGraph extends Display {

	private static final long serialVersionUID = 1L;
	public static final String graph = "graph";
	public static final String graphNodes = "graph.nodes";
	public static final String graphEdges = "graph.edges";
	public static final String aggr = "aggregates";
	public Graph graphdata;
	public GAIAGraphObject graphObj;
	public String aggrattr;
	public String keyattr;
	public GraphDistanceFilter distfilter;
	public String output;

	public VizGraph(linqs.gaia.graph.Graph g, String key, String initLayout){
		super(new Visualization());
		initialize(new GAIAGraphObject(g), key, initLayout);
	}

	private void initialize(GAIAGraphObject g, String key, String initLayout){
		String attr="nodeType";
		graphObj=g;
		graphdata=graphObj.getPrefuseGraph(key);
		m_vis.add(graph, graphdata);

		m_vis.setInteractive(graphEdges, null, true);

		// Show message on tool tip
		ToolTipControl ttc = new ToolTipControl(attr);

		aggrattr=attr;
		keyattr=key;

		// Change property of node on hover
		Control hoverc = new ControlAdapter() {
			// Highlight node or edge the mouse is currently over
			public void itemEntered(VisualItem item, MouseEvent evt) {
				if ( item.isInGroup(graphNodes) ) {
					item.setFillColor(ColorLib.rgb(255,255,0));
					item.setStrokeColor(ColorLib.rgb(255,255,0));
					item.getVisualization().repaint();
				}

				if ( item.isInGroup(graphEdges) ) {
					item.setFillColor(ColorLib.rgb(255,255,0));
					item.setStrokeColor(ColorLib.rgb(255,255,0));
					item.getVisualization().repaint();

					EdgeItem ei = (EdgeItem) item;
					VisualItem ti= ei.getTargetItem();
					ti.setFillColor(ColorLib.rgb(255,255,0));
					ti.setStrokeColor(ColorLib.rgb(255,255,0));
					VisualItem si= ei.getSourceItem();
					si.setFillColor(ColorLib.rgb(255,255,0));
					si.setStrokeColor(ColorLib.rgb(255,255,0));
				}
			}

			// If exiting a node, reset color unless the node was clicked.
			public void itemExited(VisualItem item, MouseEvent evt) {
				if(item==lastClicked){
					return;
				}

				if (item.isInGroup(graphNodes)) {
					item.setFillColor(item.getEndFillColor());
					item.setStrokeColor(item.getEndStrokeColor());
					item.getVisualization().repaint();
				}

				if ( item.isInGroup(graphEdges) ) {
					item.setFillColor(item.getEndFillColor());
					item.setStrokeColor(item.getEndStrokeColor());

					EdgeItem ei = (EdgeItem) item;
					VisualItem ti= ei.getTargetItem();
					ti.setFillColor(ti.getEndFillColor());
					ti.setStrokeColor(ti.getEndStrokeColor());
					VisualItem si= ei.getSourceItem();
					si.setFillColor(si.getEndFillColor());
					si.setStrokeColor(si.getEndStrokeColor());

					item.getVisualization().repaint();
				}
			}

			public VisualItem lastClicked = null;
			public void itemClicked(VisualItem item, MouseEvent evt) {
				// Deselect the last clicked item
				if(lastClicked!=null){
					lastClicked.setFillColor(lastClicked.getEndFillColor());
					lastClicked.setStrokeColor(lastClicked.getEndStrokeColor());
					if(lastClicked.equals(item)){
						lastClicked=null;
						item.getVisualization().repaint();
						firePropertyChange("output", output, "");
						output="";
						return;
					}

					// unselect the node in the other graph
					//unselectOtherGraphItem();

					if(lastClicked.isInGroup(graphEdges)){
						lastClicked.setFillColor(lastClicked.getEndFillColor());
						lastClicked.setStrokeColor(lastClicked.getEndStrokeColor());

						EdgeItem ei = (EdgeItem) lastClicked;
						VisualItem ti= ei.getTargetItem();
						ti.setFillColor(ti.getEndFillColor());
						ti.setStrokeColor(ti.getEndStrokeColor());
						VisualItem si= ei.getSourceItem();
						si.setFillColor(si.getEndFillColor());
						si.setStrokeColor(si.getEndStrokeColor());

						item.getVisualization().repaint();
					}
				}
				lastClicked =item;

				// Highlight selected item green
				// and output properties of selected item
				// in text pane.
				output = "";
				if ( item.isInGroup(graphNodes) ) {
					item.setHighlighted(true);
					item.setFillColor(ColorLib.rgb(0,255,0));
					item.setStrokeColor(ColorLib.rgb(0,255,0));
					item.getVisualization().repaint();

					output = "Node Attributes\n";
					output += getPropertyString(item.getLong("nodeID"),true);
				}

				if ( item.isInGroup(graphEdges) ) {
					item.setFillColor(ColorLib.rgb(0,255,0));
					item.setStrokeColor(ColorLib.rgb(0,255,0));
					item.getVisualization().repaint();

					EdgeItem ei = (EdgeItem) item;
					VisualItem ti= ei.getTargetItem();
					ti.setFillColor(ColorLib.rgb(0,255,0));
					ti.setStrokeColor(ColorLib.rgb(0,255,0));
					VisualItem si= ei.getSourceItem();
					si.setFillColor(ColorLib.rgb(0,255,0));
					si.setStrokeColor(ColorLib.rgb(0,255,0));

					output = "Edge Attributes\n";
					output += getPropertyString(item.getLong("edgeID"),false);
					output += "\nStart Node Attributes\n";
					output += getPropertyString(si.getLong("nodeID"),true);
					output += "\nTarget Node Attributes\n";
					output += getPropertyString(ti.getLong("nodeID"),true);
				}
				firePropertyChange("output", "", output);

			}

		};

		this.addControlListener(ttc);
		this.addControlListener(hoverc);

		// create a new default renderer factory
		// return our name label renderer as the default for all non-EdgeItems
		// includes straight line edges for EdgeItems by default

		if(m_vis.getGroup(graphNodes).getTupleCount()<50){
			LabelRenderer r = new LabelRenderer(attr);
			r.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
			r.setHorizontalAlignment(Constants.CENTER);
			r.setRoundedCorner(8, 8); // round the corners
			m_vis.setRendererFactory(new DefaultRendererFactory(r));
		} else {
			// Option to print a shape instead of a label
			// Note:  Is an option since the edge labels are huge.
			ShapeRenderer r = new ShapeRenderer(10);
			m_vis.setValue(graphNodes, null, VisualItem.SHAPE,
					Integer.valueOf(Constants.SHAPE_ELLIPSE));
			m_vis.setRendererFactory(new DefaultRendererFactory(r));
		}

		// ---- ACTIONS ----
		// initially use grey
		ColorAction nodefill = new ColorAction(graphNodes,
				VisualItem.FILLCOLOR, ColorLib.gray(220,245));
		ColorAction nodestroke = new ColorAction(graphNodes,
				VisualItem.STROKECOLOR, ColorLib.gray(110));
		// use black for node text
		ColorAction nodetext = new ColorAction(graphNodes,
				VisualItem.TEXTCOLOR, ColorLib.gray(0));
		// use light grey for edges
		ColorAction edgestroke = new ColorAction(graphEdges,
				VisualItem.STROKECOLOR, ColorLib.gray(100,50));
		ColorAction edgefill = new ColorAction(graphEdges,
				VisualItem.FILLCOLOR, ColorLib.gray(100,40));


		// create action lists containing all color assignments
		// separate lists for each graph property in order to
		// change them separately. May not need the lists here.
		ActionList nodeColor = new ActionList();
		nodeColor.add(nodefill);
		nodeColor.add(nodestroke);
		nodeColor.add(new RepaintAction());
		ActionList edgeColor = new ActionList();
		edgeColor.add(edgestroke);
		edgeColor.add(edgefill);
		edgeColor.add(new RepaintAction());
		ActionList textColor = new ActionList();
		textColor.add(nodetext);

		ActionList nodeSize = new ActionList();
		nodeSize.add(new SizeAction(graphNodes,1));
		nodeSize.add(new RepaintAction());
		ActionList edgeSize = new ActionList();
		edgeSize.add(new SizeAction(graphEdges,1));
		edgeSize.add(new RepaintAction());

		// create the initial layout
		setLayout(initLayout);

		// add the actions to the visualization
		m_vis.putAction("nodeColor", nodeColor);
		m_vis.putAction("edgeColor", edgeColor);
		m_vis.putAction("textColor", textColor);
		m_vis.putAction("nodeSize", nodeSize);
		m_vis.putAction("edgeSize", edgeSize);

		// add control listeners
		addControlListener(new DragControl());
		addControlListener(new PanControl());
		addControlListener(new ZoomControl());
		addControlListener(new ZoomToFitControl());
		addControlListener(new FocusControl(1));


		// start up the actions
		m_vis.run("layout");
		m_vis.run("nodeColor");
		m_vis.run("edgeColor");
		m_vis.run("textColor");
		m_vis.run("nodeSize");
		m_vis.run("edgeSize");
	}

	public void exportToJPG(String path){
		UILib.setPlatformLookAndFeel();
		//MainFrameLight f = new MainFrameLight();
		//f.setVizGraph(this);
		BufferedImage img=new BufferedImage(this.getWidth(),
				this.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = img.createGraphics();
		this.paint(g2);
		g2.dispose();
		try {
			ImageIO.write(img,"JPG",new File(path));
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
	}

	public ArrayList<?> getMapList(long id, boolean isNode){
		return graphObj.getGAIAItemList(id, isNode);
	}

	public void updatePrefuseGraph(String fid, boolean isAddedNodeFeature, boolean isApproximate){
		updatePrefuseGraph(fid, isAddedNodeFeature, isApproximate, "cacheNodeColumn");
	}

	/**
	 *  Fetch Attributes that will only affect the visualization
	 */
	public void updatePrefuseGraph(String fid, boolean isAddedNodeFeature, boolean isApproximate, String column){
		Table backTable;

		if(isAddedNodeFeature)
			backTable=graphdata.getNodeTable();
		else
			backTable=graphdata.getEdgeTable();

		if(isApproximate)
			graphObj.populatePrefuseGraphTable(fid, isAddedNodeFeature, backTable,true);
		else
			graphObj.populatePrefuseGraphTable(fid, isAddedNodeFeature, backTable,column);
	}

	/**
	 * Switches the graph layout based on user combobox selection.
	 *
	 * The BallonTreeLayout doesnt seem to work
	 * @param layoutType Layout type to use.  Options are RadialTreeLayout,
	 * ForceDirectedLayout, BallonTreeLayout, NodeLinkTreeLayout, FruchtermanReingoldLayout,
	 * and SquarifiedTreeMapLayout.
	 */
	public void setLayout(String layoutType){
		// cancel previous layout
		m_vis.cancel("layout");
		ActionList layout = new ActionList();
		if (layoutType.equals("RadialTreeLayout")){
			layout.add(new RadialTreeLayout(graph));
		}else if (layoutType.equals("ForceDirectedLayout")){
			layout = new ActionList(Activity.INFINITY);//Run force directed at most 5 seconds

			ForceSimulator fsim = new ForceSimulator();
			fsim.addForce(new NBodyForce(1f, 10f, NBodyForce.DEFAULT_THETA));
			fsim.addForce(new DragForce());

			layout.add(new ForceDirectedLayout(graph, fsim, true));
		}else if (layoutType.equals("BallonTreeLayout")){
			layout.add(new BalloonTreeLayout(graph));
		}else if (layoutType.equals("NodeLinkTreeLayout")){
			layout.add(new NodeLinkTreeLayout(graph,Constants.ORIENT_TOP_BOTTOM, 50, 0, 8));
		}else if (layoutType.equals("FruchtermanReingoldLayout")){
			layout.add(new FruchtermanReingoldLayout(graph));
		}else if (layoutType.equals("SquarifiedTreeMapLayout")){
			layout.add(new SquarifiedTreeMapLayout(graph));
		}

		layout.add(new RepaintAction());
		m_vis.putAction("layout", layout);
		m_vis.run("layout");
	}


	public void allvisible(){
		Iterator<?> itr = m_vis.items();
		while(itr.hasNext()){
			VisualItem i = (VisualItem) itr.next();
			i.setVisible(true);
		}
	}

	public void allInvisible(){
		Iterator<?> itr = m_vis.items();
		while(itr.hasNext()){
			VisualItem i = (VisualItem) itr.next();
			i.setVisible(false);
		}
	}

	/**
	 * Switch the root of the tree by requesting a new spanning tree
	 * at the desired root
	 */
	public static class TreeRootAction extends GroupAction {
		public TreeRootAction(String graphGroup) {
			super(graphGroup);
		}
		public void run(double frac) {
			TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
			if ( focus==null || focus.getTupleCount() == 0 ) return;

			Graph g = (Graph)m_vis.getGroup(m_group);
			Node f = null;
			Iterator<?> tuples = focus.tuples();
			while (tuples.hasNext() && !g.containsTuple(f=(Node)tuples.next()))
			{
				f = null;
			}
			if ( f == null ) return;
			g.getSpanningTree(f);
		}
	}

	/**
	 * Set node fill colors
	 */
	public static class NodeColorAction extends ColorAction {
		public NodeColorAction(String group) {
			super(group, VisualItem.FILLCOLOR, ColorLib.rgba(255,255,255,0));
			add("_hover", ColorLib.gray(220,230));
			add("ingroup('_search_')", ColorLib.rgb(255,190,190));
			add("ingroup('_focus_')", ColorLib.rgb(198,229,229));
		}

	} // end of inner class NodeColorAction

	/**
	 * Set node text colors
	 */
	public static class TextColorAction extends ColorAction {
		public TextColorAction(String group) {
			super(group, VisualItem.TEXTCOLOR, ColorLib.gray(0));
			add("_hover", ColorLib.rgb(255,0,0));
		}
	} // end of inner class TextColorAction


	public Visualization getVis(){
		return m_vis;
	}

	//  Print property of the selected item
	public String getPropertyString(long id, boolean isNode){
		return graphObj.getPropertyString(id, isNode);
	}

	public static boolean checkValue(String key, String val){
		String parts[]=val.split(",");
		for(int i=0; i<parts.length; i++){
			if(parts[i].equalsIgnoreCase(key)){
				return true;
			}
		}

		return false;
	}

    public void setNodeColors(String attrib){
    	ActionList recolor = new ActionList();
    	int[] palette = new int[] {
        	ColorLib.rgb(141,211,199),
        	ColorLib.rgb(255,255,179),
        	ColorLib.rgb(190,186,218),
        	ColorLib.rgb(251,128,114),
        	ColorLib.rgb(128,177,211),
        	ColorLib.rgb(253,180,98),
        	ColorLib.rgb(179,222,105),
        	ColorLib.rgb(252,205,229),
        	ColorLib.rgb(217,217,217),
        	ColorLib.rgb(188,128,189),
        	ColorLib.rgb(204,235,197),
        	ColorLib.rgb(255,237,111)
        };

    	if(attrib.equals("NONE")){
            m_vis.run("nodeColor");
    	} else if (graphObj.isOrdinalFeature(attrib,true)){
    		CDataColorAction fill = null;
    		fill = new CDataColorAction(graphNodes, "cacheNodeColumn",
                Constants.ORDINAL, VisualItem.FILLCOLOR, ColorLib.rgb(0, 0, 255), ColorLib.rgb(255, 0, 0));

    		if (fill!=null){
            	recolor.add(fill);
            	recolor.add(new RepaintAction());
            	m_vis.putAction("nodeColorCustom", recolor);
            	m_vis.run("nodeColorCustom");
        	}
    	} else {
    		DataColorAction fill = new DataColorAction(graphNodes, "cacheNodeColumn",
                    Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
    		if (fill!=null){
            	recolor.add(fill);
            	recolor.add(new RepaintAction());
            	m_vis.putAction("nodeColorCustom", recolor);
            	m_vis.run("nodeColorCustom");
        	}
    	}

    }

} // end of class NetworkGraph
