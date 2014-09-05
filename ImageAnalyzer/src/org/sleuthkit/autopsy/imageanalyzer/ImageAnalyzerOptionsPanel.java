/*
 * Autopsy Forensic Browser
 *
 * Copyright 2013 Basis Technology Corp.
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
package org.sleuthkit.autopsy.imageanalyzer;

import java.awt.event.ActionEvent;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.ingest.IngestManager;

/** The Image/Video Analyzer panel in the NetBeans provided Options Dialogs
 * accessed via Tool -> Options
 *
 * Uses {@link ImageAnalyzerPreferences} and {@link PerCaseProperties} to
 * persist settings
 */
final class ImageAnalyzerOptionsPanel extends javax.swing.JPanel {

    ImageAnalyzerOptionsPanel(ImageAnalyzerOptionsPanelController controller) {
        initComponents();

        //listen for interactions
        IngestManager.getInstance().addIngestJobEventListener(evt -> {
            //disable during ingest
            enabledForCaseBox.setEnabled(Case.isCaseOpen() && IngestManager.getInstance().isIngestRunning() == false);
        });

        enabledByDefaultBox.addActionListener((ActionEvent e) -> {
            controller.changed();
        });

        enabledForCaseBox.addActionListener((ActionEvent e) -> {
            controller.changed();
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        enabledByDefaultBox = new javax.swing.JCheckBox();
        enabledForCaseBox = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(enabledByDefaultBox, org.openide.util.NbBundle.getMessage(ImageAnalyzerOptionsPanel.class, "ImageAnalyzerOptionsPanel.enabledByDefaultBox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(enabledForCaseBox, org.openide.util.NbBundle.getMessage(ImageAnalyzerOptionsPanel.class, "ImageAnalyzerOptionsPanel.enabledForCaseBox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(enabledForCaseBox)
                    .addComponent(enabledByDefaultBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(enabledByDefaultBox)
                .addGap(18, 18, 18)
                .addComponent(enabledForCaseBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /** {@inheritDoc} */
    void load() {
        enabledByDefaultBox.setSelected(ImageAnalyzerPreferences.isEnabledByDefault());
        if (Case.isCaseOpen() && IngestManager.getInstance().isIngestRunning() == false) {
            enabledForCaseBox.setEnabled(true);
            enabledForCaseBox.setSelected(ImageAnalyzerModule.isEnabledforCase(Case.getCurrentCase()));
        } else {
            enabledForCaseBox.setEnabled(false);
            enabledForCaseBox.setSelected(enabledByDefaultBox.isSelected());
        }
    }

    /** {@inheritDoc } */
    void store() {
        ImageAnalyzerPreferences.setEnabledByDefault(enabledByDefaultBox.isSelected());
        ImageAnalyzerController.getDefault().setListeningEnabled(enabledForCaseBox.isSelected());
        if (Case.isCaseOpen()) {
            new PerCaseProperties(Case.getCurrentCase()).setConfigSetting(ImageAnalyzerModule.MODULE_NAME, PerCaseProperties.ENABLED, Boolean.toString(enabledForCaseBox.isSelected()));
        }
    }

    /** {@inheritDoc }
     *
     * @return true, since there is no way for this form to be invalid */
    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox enabledByDefaultBox;
    private javax.swing.JCheckBox enabledForCaseBox;
    // End of variables declaration//GEN-END:variables
}
