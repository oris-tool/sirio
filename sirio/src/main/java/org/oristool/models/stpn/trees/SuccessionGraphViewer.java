/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2018 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.models.stpn.trees;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;


@SuppressWarnings("serial")
public class SuccessionGraphViewer extends JPanel {

    private final JLabel statusBar;
    private final mxGraphComponent graphComponent;
    private final JTextArea infoArea;
    private final Map<Object, State> vertexState = new HashMap<Object, State>();
    private final Map<State, Object> stateVertex = new HashMap<State, Object>();

    /**
     * Creates a graph viewer.
     *
     * @param g succession graph to be visualized
     */
    public SuccessionGraphViewer(SuccessionGraph g) {

        // create graph component
        final mxGraph graph = buildGraph(g);
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getGraphHandler().setLivePreview(true);
        graphComponent.getGraphHandler().getMovePreview()
                .setClonePreview(false);
        new mxRubberband(graphComponent);
        installListeners(graphComponent);

        // create toolbar
        infoArea = new JTextArea();
        JPanel toolBar = buildToolbar(graphComponent, infoArea);

        // add them to a split panel
        final JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, graphComponent, toolBar);
        splitPane.setDividerLocation(1000);

        // create statusbar
        statusBar = new JLabel("ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        // adds everything
        this.setLayout(new BorderLayout());
        this.add(splitPane, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);

    }

    private JPanel buildToolbar(mxGraphComponent graphComponent,
            JTextArea infoArea) {

        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BorderLayout());

        // toolbar Buttons
        JPanel buttonBar = new JPanel();
        buttonBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.Y_AXIS));

        // graphOutline
        mxGraphOutline graphOutline = new mxGraphOutline(graphComponent);
        graphOutline.setPreferredSize(new Dimension(100, 100));
        buttonBar.add(graphOutline);
        buttonBar.add(Box.createVerticalStrut(10));

        // zoom to fit
        JButton btZoomToFit = new JButton("Zoom to fit");
        btZoomToFit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                mxGraphComponent graphComponent = SuccessionGraphViewer.this.graphComponent;
                double newScale = 1;

                Dimension graphSize = graphComponent.getGraphControl()
                        .getSize();
                Dimension viewPortSize = graphComponent.getViewport().getSize();

                int gw = (int) graphSize.getWidth();
                int gh = (int) graphSize.getHeight();

                if (gw > 0 && gh > 0) {
                    int w = (int) viewPortSize.getWidth();
                    int h = (int) viewPortSize.getHeight();

                    newScale = Math.min((double) w / gw, (double) h / gh);
                }

                SuccessionGraphViewer.this.graphComponent.zoom(newScale);
            }
        });
        buttonBar.add(btZoomToFit);
        buttonBar.add(Box.createVerticalStrut(10));

        // center graph
        JButton btCenter = new JButton("Center");
        btCenter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                mxGraphComponent graphComponent = SuccessionGraphViewer.this.graphComponent;

                Dimension graphSize = graphComponent.getGraphControl()
                        .getSize();
                Dimension viewPortSize = graphComponent.getViewport().getSize();

                int x = graphSize.width / 2 - viewPortSize.width / 2;
                int y = graphSize.height / 2 - viewPortSize.height / 2;
                int w = viewPortSize.width;
                int h = viewPortSize.height;

                graphComponent.getGraphControl().scrollRectToVisible(
                        new Rectangle(x, y, w, h));

            }
        });
        buttonBar.add(btCenter);
        buttonBar.add(Box.createVerticalStrut(10));

        // horizontal layout
        JButton horLayout = new JButton("Horizontal layout");
        horLayout.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final mxGraphComponent graphComponent = SuccessionGraphViewer.this.graphComponent;
                final mxGraph graph = graphComponent.getGraph();

                mxHierarchicalLayout layout = new mxHierarchicalLayout(graph,
                        SwingConstants.WEST);
                layout.setDisableEdgeStyle(true);
                graph.getModel().beginUpdate();

                try {
                    layout.execute(graph.getDefaultParent());
                } finally {
                    mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2,
                            20);

                    morph.addListener(mxEvent.DONE, new mxIEventListener() {
                        @Override
                        public void invoke(Object arg0, mxEventObject arg1) {
                            graph.getModel().endUpdate();
                        }
                    });

                    morph.startAnimation();
                }
            }
        });
        buttonBar.add(horLayout);
        buttonBar.add(Box.createVerticalStrut(10));

        // vertical layout
        JButton vertLayout = new JButton("Vertical layout");
        vertLayout.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final mxGraphComponent graphComponent = SuccessionGraphViewer.this.graphComponent;
                final mxGraph graph = graphComponent.getGraph();

                mxHierarchicalLayout layout = new mxHierarchicalLayout(graph,
                        SwingConstants.NORTH);
                layout.setDisableEdgeStyle(true);
                graph.getModel().beginUpdate();

                try {
                    layout.execute(graph.getDefaultParent());
                } finally {
                    mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2,
                            20);

                    morph.addListener(mxEvent.DONE, new mxIEventListener() {
                        @Override
                        public void invoke(Object arg0, mxEventObject arg1) {
                            graph.getModel().endUpdate();
                        }
                    });

                    morph.startAnimation();
                }
            }
        });
        buttonBar.add(vertLayout);

        toolBar.add(buttonBar, BorderLayout.NORTH);

        // info bar
        JPanel lowerBar = new JPanel(new BorderLayout());
        lowerBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoBar = new JPanel();
        infoBar.setBorder(new TitledBorder("State information"));
        infoBar.setLayout(new BorderLayout());
        infoArea.setEditable(false);
        infoArea.setBorder(BorderFactory.createEmptyBorder());
        infoArea.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setPreferredSize(new Dimension(200, 100));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoBar.add(scrollPane, BorderLayout.CENTER);
        lowerBar.add(infoBar, BorderLayout.CENTER);
        toolBar.add(lowerBar, BorderLayout.CENTER);

        return toolBar;
    }

    private mxGraph buildGraph(SuccessionGraph g) {
        mxGraph graph = new mxGraph();
        graph.setDisconnectOnMove(false);

        Hashtable<String, Object> regStyle = new Hashtable<String, Object>();
        regStyle.put(mxConstants.STYLE_FILLCOLOR, "ffb56c");

        Hashtable<String, Object> rootStyle = new Hashtable<String, Object>();
        rootStyle.put(mxConstants.STYLE_FILLCOLOR, "ffb56c");
        rootStyle.put(mxConstants.STYLE_STROKECOLOR, "b13f18");
        rootStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);

        mxStylesheet stylesheet = graph.getStylesheet();
        stylesheet.putCellStyle("regenerative", regStyle);
        stylesheet.putCellStyle("root", rootStyle);

        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();

        addVertex(g, g.getState(g.getRoot()), graph, parent);
        try {
            for (Succession s : g.getSuccessions()) {
                Object v = addVertex(g, s.getChild(), graph, parent);

                if (s.getParent() != null) {
                    Object u = addVertex(g, s.getParent(), graph, parent);
                    graph.insertEdge(parent, null, s.getEvent(), u, v);
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }

        return graph;
    }

    private Object addVertex(SuccessionGraph g, State s, mxGraph graph,
            Object parent) {

        if (stateVertex.containsKey(s)) {
            return stateVertex.get(s);
        } else {
            Marking marking = s.getFeature(PetriStateFeature.class)
                    .getMarking();
            int id = g.getNode(s).id();

            String style = "";
            if (s == g.getState(g.getRoot()))
                style = "root";
            else if (s.hasFeature(Regeneration.class))
                style = "regenerative";

            Object v = graph.insertVertex(parent, null, id + ": " + marking,
                    100, 100, 80, 30, style);
            vertexState.put(v, s);
            stateVertex.put(s, v);
            return v;
        }
    }

    private void installListeners(final mxGraphComponent graphComponent) {
        // Installs mouse wheel listener for zooming
        graphComponent.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getSource() instanceof mxGraphOutline
                        || e.isControlDown()) {
                    if (e.getWheelRotation() < 0) {
                        graphComponent.zoomIn();
                    } else {
                        graphComponent.zoomOut();
                    }

                    statusBar.setText("scale: "
                            + (int) (100 * graphComponent.getGraph().getView()
                                    .getScale()) + "%");
                }
            }
        });

        // Handles mouse wheel events in the outline and graph component
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mxCell cell = (mxCell) graphComponent.getCellAt(e.getX(),
                        e.getY());
                if (cell != null && cell.isVertex()) {
                    infoArea.setText(vertexState.get(cell).toString());
                } else {
                    infoArea.setText("");
                }
            }
        });
    }

    /**
     * Displays the graph in a JFrame.
     *
     * @param graph input graph
     */
    public static void show(SuccessionGraph graph) {

        JPanel viewer = new SuccessionGraphViewer(graph);
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame frame = new JFrame("State graph");
                frame.add(viewer);
                frame.setDefaultCloseOperation(3);
                frame.setExtendedState(6);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
