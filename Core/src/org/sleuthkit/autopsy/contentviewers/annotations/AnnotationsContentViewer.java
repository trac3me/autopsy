/*
 * Autopsy Forensic Browser
 *
 * Copyright 2018-2021 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.contentviewers.annotations;

import com.google.common.collect.ImmutableSet;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.SwingWorker;

import static org.openide.util.NbBundle.Messages;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataContentViewer;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.jsoup.nodes.Document;
import org.openide.util.WeakListeners;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.contentviewers.layout.ContentViewerHtmlStyles;
import org.sleuthkit.autopsy.contentviewers.utils.ViewerPriority;
import org.sleuthkit.autopsy.guiutils.RefreshThrottler;
import org.sleuthkit.autopsy.guiutils.RefreshThrottler.Refresher;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.autopsy.ingest.ModuleDataEvent;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 * Annotations view of file contents.
 */
@SuppressWarnings("PMD.SingularField") // UI widgets cause lots of false positives
@ServiceProvider(service = DataContentViewer.class, position = 9)
@Messages({
    "AnnotationsContentViewer.title=Annotations",
    "AnnotationsContentViewer.toolTip=Displays tags and comments associated with the selected content.",
    "AnnotationsContentViewer.onEmpty=No annotations were found for this particular item."
})
public class AnnotationsContentViewer extends javax.swing.JPanel implements DataContentViewer {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AnnotationsContentViewer.class.getName());

    private static final Set<Case.Events> CASE_EVENTS_OF_INTEREST = EnumSet.of(
            Case.Events.BLACKBOARD_ARTIFACT_TAG_ADDED,
            Case.Events.BLACKBOARD_ARTIFACT_TAG_DELETED,
            Case.Events.CONTENT_TAG_ADDED,
            Case.Events.CONTENT_TAG_DELETED,
            Case.Events.CR_COMMENT_CHANGED);

    private static final Set<BlackboardArtifact.Type> ARTIFACT_TYPES_OF_INTEREST = ImmutableSet.of(
            BlackboardArtifact.Type.TSK_HASHSET_HIT,
            BlackboardArtifact.Type.TSK_INTERESTING_FILE_HIT
    );

    /**
     * Refresher used with refresh throttler to listen for artifact events.
     */
    private final Refresher refresher = new Refresher() {

        @Override
        public void refresh() {
            AnnotationsContentViewer.this.refresh();
        }

        @Override
        public boolean isRefreshRequired(PropertyChangeEvent evt) {
            if (IngestManager.IngestModuleEvent.DATA_ADDED.toString().equals(evt.getPropertyName()) && evt.getOldValue() instanceof ModuleDataEvent) {
                ModuleDataEvent moduleDataEvent = (ModuleDataEvent) evt.getOldValue();
                if (ARTIFACT_TYPES_OF_INTEREST.contains(moduleDataEvent.getBlackboardArtifactType())) {
                    return true;
                }
            }
            return false;
        }
    };

    private final RefreshThrottler refreshThrottler = new RefreshThrottler(refresher);

    private final PropertyChangeListener caseEventListener = WeakListeners.propertyChange((pcl) -> refresh(), null);

    private final Object updateLock = new Object();

    private AnnotationWorker worker = null;

    private Node node;

    /**
     * Creates an instance of AnnotationsContentViewer.
     */
    public AnnotationsContentViewer() {
        initComponents();
        ContentViewerHtmlStyles.setupHtmlJTextPane(textPanel);
        registerListeners();
    }

    @Override
    protected void finalize() throws Throwable {
        unregisterListeners();
    }

    /**
     * Registers case event and ingest event listeners.
     */
    private void registerListeners() {
        Case.addEventTypeSubscriber(CASE_EVENTS_OF_INTEREST, caseEventListener);
        refreshThrottler.registerForIngestModuleEvents();;
    }

    /**
     * Unregisters case event and ingest event listeners.
     */
    private void unregisterListeners() {
        Case.removeEventTypeSubscriber(CASE_EVENTS_OF_INTEREST, caseEventListener);
        refreshThrottler.unregisterEventListener();
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
        updateData(this.node, true);
    }

    /**
     * Refreshes the data displayed.
     */
    private void refresh() {
        updateData(this.node, false);
    }

    /**
     * Updates data displayed in the viewer.
     *
     * @param node       The node to use for data.
     * @param forceReset If true, forces a reset cancelling the previous worker
     *                   if one exists and clearing data in the component. If
     *                   false, only submits a worker if no previous worker is
     *                   running.
     */
    private void updateData(Node node, boolean forceReset) {
        if (node == null) {
            return;
        }

        if (forceReset) {
            resetComponent();
        }

        synchronized (updateLock) {
            if (worker != null) {
                if (forceReset) {
                    worker.cancel(true);
                    worker = null;
                } else {
                    return;
                }
            }

            worker = new AnnotationWorker(node, forceReset);
            worker.execute();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane();
        textPanel = new javax.swing.JTextPane();

        setPreferredSize(new java.awt.Dimension(100, 58));

        textPanel.setEditable(false);
        textPanel.setName(""); // NOI18N
        textPanel.setPreferredSize(new java.awt.Dimension(600, 52));
        scrollPane.setViewportView(textPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 907, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextPane textPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getTitle() {
        return Bundle.AnnotationsContentViewer_title();
    }

    @Override
    public String getToolTip() {
        return Bundle.AnnotationsContentViewer_toolTip();
    }

    @Override
    public DataContentViewer createInstance() {
        return new AnnotationsContentViewer();
    }

    @Override
    public boolean isSupported(Node node) {
        return AnnotationUtils.isSupported(node);
    }

    @Override
    public int isPreferred(Node node) {
        return ViewerPriority.viewerPriority.LevelOne.getFlag();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void resetComponent() {
        textPanel.setText("");
    }

    /**
     * A SwingWorker that will fetch the annotation information for the given
     * node.
     */
    private class AnnotationWorker extends SwingWorker<String, Void> {

        private final Node node;
        private final boolean resetCaretPosition;

        /**
         * Main constructor.
         *
         * @param node               The node for which data will be fetched.
         * @param resetCaretPosition Whether or not to reset the caret position
         *                           when finished.
         */
        AnnotationWorker(Node node, boolean resetCaretPosition) {
            this.node = node;
            this.resetCaretPosition = resetCaretPosition;
        }

        @Override
        protected String doInBackground() throws Exception {
            Document doc = AnnotationUtils.buildDocument(node);

            if (isCancelled()) {
                return null;
            }

            if (doc != null) {
                return doc.html();
            } else {
                return "<span class='" + ContentViewerHtmlStyles.getMessageClassName() + "'>" + Bundle.AnnotationsContentViewer_onEmpty() + "</span>";
            }
        }

        @Override
        public void done() {
            if (!isCancelled()) {
                try {
                    String text = get();
                    ContentViewerHtmlStyles.setStyles(textPanel);
                    textPanel.setText(text);

                    if (resetCaretPosition) {
                        textPanel.setCaretPosition(0);
                    }

                } catch (InterruptedException | ExecutionException ex) {
                    logger.log(Level.SEVERE, "Failed to get annotation information for node", ex);
                }
            }

            synchronized (updateLock) {
                if (worker == this) {
                    worker = null;
                }
            }
        }

    }
}
