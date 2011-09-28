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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class MainFrameLight extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JTextPane jInfoText = null;

	private VizGraph vizgraph = null;

	private javax.swing.JMenuBar menuBar;
	private javax.swing.JMenu fileMenu;
	private javax.swing.JMenuItem quitItem;
	private javax.swing.JMenu optionsMenu;
	private javax.swing.JMenuItem changeLayoutItem;
	private javax.swing.JMenuItem changeColorItem;
	private javax.swing.JMenuItem saveAsJpgItem;
	private javax.swing.JMenu helpMenu;
	private javax.swing.JMenuItem instructionsItem;
	private javax.swing.JMenuItem aboutUsItem;

	/**
	 * This is the default constructor
	 */
	public MainFrameLight() {
		super();
		initialize();
	}

	public void setVizGraph(VizGraph vizgraph) {
		this.vizgraph = vizgraph;
		this.add(vizgraph);
		jInfoText=new JTextPane();
		jInfoText.setEditable(false);
		this.add(new JScrollPane(jInfoText));

		validate();
		getJContentPane().getComponent(0).addPropertyChangeListener("output",new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt){
				jInfoText.setText(evt.getNewValue().toString());
				jInfoText.setCaretPosition(0);
			}
		});
	}

	/**
	 * This method initializes this
	 */
	private void initialize() {
		this.setSize(650, 500);
		this.setContentPane(getJContentPane());
		this.setTitle("LightViz");
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		// Add menu bar
		menuBar = new javax.swing.JMenuBar();
		fileMenu = new javax.swing.JMenu();
		quitItem = new javax.swing.JMenuItem();
		optionsMenu = new javax.swing.JMenu();
		changeLayoutItem = new javax.swing.JMenuItem();
		changeColorItem = new javax.swing.JMenuItem();
		saveAsJpgItem = new javax.swing.JMenuItem();
		helpMenu = new javax.swing.JMenu();
		instructionsItem = new javax.swing.JMenuItem();
		aboutUsItem = new javax.swing.JMenuItem();

		// Add file menu
		fileMenu.setText("File");

		// Add quit
		quitItem.setText("Quit");
		quitItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				quitItemActionPerformed(evt);
			}
		});
		fileMenu.add(quitItem);

		menuBar.add(fileMenu);

		/****************************************/

		// Add options menu
		optionsMenu.setText("Options");
		changeLayoutItem.setText("Change Layout");
		changeLayoutItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				changeLayoutActionPerformed(evt);
			}
		});
		optionsMenu.add(changeLayoutItem);

		changeColorItem.setText("Change Node Color");
		changeColorItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				changeColorActionPerformed(evt);
			}
		});
		optionsMenu.add(changeColorItem);

		saveAsJpgItem.setText("Save As JPG");
		saveAsJpgItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				saveAsJpgActionPerformed(evt);
			}
		});
		optionsMenu.add(saveAsJpgItem);

		menuBar.add(optionsMenu);

		/****************************************/

		// Add help menu
		helpMenu.setText("Help");

		instructionsItem.setText("Instructions");
		instructionsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				instructionsItemActionPerformed(evt);
			}
		});
		helpMenu.add(instructionsItem);

		aboutUsItem.setText("About Us");
		aboutUsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				aboutUsItemActionPerformed(evt);
			}
		});
		helpMenu.add(aboutUsItem);

		menuBar.add(helpMenu);

		// Set menu bar
		setJMenuBar(menuBar);
	}

	private void saveAsJpgActionPerformed(java.awt.event.ActionEvent evt) {
		// Pop up a file choose so you can set the file name and directory
		// to save image to
		JFileChooser chooser = new JFileChooser("Save as JPG");
	    chooser.setApproveButtonText("Save");
	    chooser.showSaveDialog(this);
	    File f = chooser.getSelectedFile();

	    this.vizgraph.exportToJPG(f.getAbsolutePath());
	}

	private void changeLayoutActionPerformed(java.awt.event.ActionEvent evt) {
		// Pop up a selection box to show available layouts to change to
		Object[] possibilities = {"RadialTreeLayout",
				"ForceDirectedLayout",
				"BallonTreeLayout",
				"NodeLinkTreeLayout",
				"FruchtermanReingoldLayout",
				"SquarifiedTreeMapLayout"};

		String s = (String) JOptionPane.showInputDialog(
				this,
				"Layout",
				"Select Layout:",
				JOptionPane.PLAIN_MESSAGE,
				null,
				possibilities,
				"RadialTreeLayout");

		if(s!=null) {
			// Get VizGraph
			this.vizgraph.setLayout(s);
		}
	}

	private void changeColorActionPerformed(java.awt.event.ActionEvent evt) {
		// Pop up a selection box to show available node attributes to choose from
		ArrayList<String> attribs = new ArrayList<String>();
		attribs.add("NONE");
		Iterator<?> itr=this.vizgraph.graphObj.getFeatures(true).iterator();
		while(itr.hasNext())
			attribs.add(itr.next().toString());


		String s = (String) JOptionPane.showInputDialog(
				this,
				"Node Color",
				"Select Attribute:",
				JOptionPane.PLAIN_MESSAGE,
				null,
				attribs.toArray(),
				"NONE");

		if(s!=null) {
			// Get VizGraph
			if(!s.equalsIgnoreCase("NONE"))
				this.vizgraph.updatePrefuseGraph(s, true, false);
			this.vizgraph.setNodeColors(s);
		}
	}

	private void instructionsItemActionPerformed(java.awt.event.ActionEvent evt) {
		// Give very brief description of what the tool has
		JOptionPane.showMessageDialog(this,
				"Basic Controls: "
				+"\n-Left click to select a node and show its properties"
				+"\n-Left click and hold to drag a node"
				+"\n-Right click and move the mouse up or down to zoom in and out"
				+"\n-Double right click will recenter the graph in the display"
				+"\n-Hovering over a node will display the node object id"
				+"\n-To change layouts, select Options>Change Layout."
				+"\n-To save the graph on display, select Options>Save As JPG"
				+"\n-To change the color of a graph based on some feature, select Options>Change Node Color",
				"Instructions",
				JOptionPane.PLAIN_MESSAGE);
	}

	private void aboutUsItemActionPerformed(java.awt.event.ActionEvent evt) {
		// Just add acknowledgments that this tool is part of GAIA by linqs group
		JOptionPane.showMessageDialog(this,
				"LighViz v1.0"
				+"\nLINQS Group, University of Maryland, College Park."
				+"\nhttp://linqs.cs.umd.edu/trac/gaia"
				+"\nhttp://linqs.cs.umd.edu",
				"About Us",
				JOptionPane.PLAIN_MESSAGE);
	}

	private void quitItemActionPerformed(java.awt.event.ActionEvent evt) {
		this.dispose();
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BoxLayout(jContentPane,BoxLayout.X_AXIS));
		}

		return jContentPane;
	}
}
