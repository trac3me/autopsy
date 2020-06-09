/*
 * Autopsy Forensic Browser
 *
 * Copyright 2020 Basis Technology Corp.
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
package org.sleuthkit.autopsy.contentviewers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.centralrepository.datamodel.CentralRepoAccount;
import org.sleuthkit.autopsy.centralrepository.datamodel.CentralRepoException;
import org.sleuthkit.autopsy.centralrepository.datamodel.Persona;
import org.sleuthkit.autopsy.centralrepository.datamodel.PersonaAccount;
import org.sleuthkit.autopsy.centralrepository.persona.PersonaDetailsDialog;
import org.sleuthkit.autopsy.centralrepository.persona.PersonaDetailsDialogCallback;
import org.sleuthkit.autopsy.centralrepository.persona.PersonaDetailsMode;
import org.sleuthkit.autopsy.centralrepository.persona.PersonaDetailsPanel;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * This class displays the TSK_CONTACT artifact.
 */
public class ContactArtifactViewer extends javax.swing.JPanel implements ArtifactContentViewer {

    private final static Logger logger = Logger.getLogger(ContactArtifactViewer.class.getName());
    private static final long serialVersionUID = 1L;

    private static final int TOP_INSET = 4;
    private static final int LEFT_INSET = 12;

    // contact name, if available.
    private String contactName;

    // A list of unique accounts matching the attributes of the contact artifact.
    private final List<CentralRepoAccount> contactUniqueAccountsList = new ArrayList<>();

    // A list of all unique personas and their account, found by searching on the 
    // account identifier attributes of the Contact artifact.
    private final Map<Persona, ArrayList<CentralRepoAccount>> contactUniquePersonasMap = new HashMap<>();

    /**
     * Creates new form for ContactArtifactViewer
     */
    public ContactArtifactViewer() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        namePanel = new javax.swing.JPanel();
        contactNameLabel = new javax.swing.JLabel();
        phonesLabel = new javax.swing.JLabel();
        phoneNumbersPanel = new javax.swing.JPanel();
        emailsLabel = new javax.swing.JLabel();
        emailsPanel = new javax.swing.JPanel();
        othersLabel = new javax.swing.JLabel();
        otherAttrsPanel = new javax.swing.JPanel();
        interPanelfiller = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        personasLabel = new javax.swing.JLabel();
        personasPanel = new javax.swing.JPanel();
        bottomFiller = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        rightFiller = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        setLayout(new java.awt.GridBagLayout());

        contactNameLabel.setFont(contactNameLabel.getFont().deriveFont((contactNameLabel.getFont().getStyle() | java.awt.Font.ITALIC) | java.awt.Font.BOLD, contactNameLabel.getFont().getSize()+6));
        org.openide.awt.Mnemonics.setLocalizedText(contactNameLabel, org.openide.util.NbBundle.getMessage(ContactArtifactViewer.class, "ContactArtifactViewer.contactNameLabel.text")); // NOI18N

        javax.swing.GroupLayout namePanelLayout = new javax.swing.GroupLayout(namePanel);
        namePanel.setLayout(namePanelLayout);
        namePanelLayout.setHorizontalGroup(
            namePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(namePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(contactNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        namePanelLayout.setVerticalGroup(
            namePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(namePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(contactNameLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(namePanel, gridBagConstraints);

        phonesLabel.setFont(phonesLabel.getFont().deriveFont(phonesLabel.getFont().getStyle() | java.awt.Font.BOLD, phonesLabel.getFont().getSize()+2));
        org.openide.awt.Mnemonics.setLocalizedText(phonesLabel, org.openide.util.NbBundle.getMessage(ContactArtifactViewer.class, "ContactArtifactViewer.phonesLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(phonesLabel, gridBagConstraints);

        phoneNumbersPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(phoneNumbersPanel, gridBagConstraints);

        emailsLabel.setFont(emailsLabel.getFont().deriveFont(emailsLabel.getFont().getStyle() | java.awt.Font.BOLD, emailsLabel.getFont().getSize()+2));
        org.openide.awt.Mnemonics.setLocalizedText(emailsLabel, org.openide.util.NbBundle.getMessage(ContactArtifactViewer.class, "ContactArtifactViewer.emailsLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(emailsLabel, gridBagConstraints);

        emailsPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(emailsPanel, gridBagConstraints);

        othersLabel.setFont(othersLabel.getFont().deriveFont(othersLabel.getFont().getStyle() | java.awt.Font.BOLD, othersLabel.getFont().getSize()+2));
        org.openide.awt.Mnemonics.setLocalizedText(othersLabel, org.openide.util.NbBundle.getMessage(ContactArtifactViewer.class, "ContactArtifactViewer.othersLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(othersLabel, gridBagConstraints);

        otherAttrsPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(otherAttrsPanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 0.1;
        add(interPanelfiller, gridBagConstraints);

        personasLabel.setFont(personasLabel.getFont().deriveFont(personasLabel.getFont().getStyle() | java.awt.Font.BOLD, personasLabel.getFont().getSize()+2));
        org.openide.awt.Mnemonics.setLocalizedText(personasLabel, org.openide.util.NbBundle.getMessage(ContactArtifactViewer.class, "ContactArtifactViewer.personasLabel.text")); // NOI18N
        personasLabel.setMaximumSize(new java.awt.Dimension(60, 19));
        personasLabel.setMinimumSize(new java.awt.Dimension(60, 19));
        personasLabel.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(personasLabel, gridBagConstraints);

        personasPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 19, 0, 0);
        add(personasPanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        add(bottomFiller, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 2;
        gridBagConstraints.weightx = 1.0;
        add(rightFiller, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setArtifact(BlackboardArtifact artifact) {

        // Reset the panel.
        resetComponent();

        List<BlackboardAttribute> phoneNumList = new ArrayList<>();
        List<BlackboardAttribute> emailList = new ArrayList<>();
        List<BlackboardAttribute> nameList = new ArrayList<>();
        List<BlackboardAttribute> otherList = new ArrayList<>();
        List<BlackboardAttribute> accountAttributesList = new ArrayList<>();

        try {
            // Get all the attributes and group them by the section panels they go in
            for (BlackboardAttribute bba : artifact.getAttributes()) {
                if (bba.getAttributeType().getTypeName().startsWith("TSK_PHONE")) {
                    phoneNumList.add(bba);
                    accountAttributesList.add(bba);
                } else if (bba.getAttributeType().getTypeName().startsWith("TSK_EMAIL")) {
                    emailList.add(bba);
                    accountAttributesList.add(bba);
                } else if (bba.getAttributeType().getTypeName().startsWith("TSK_NAME")) {
                    nameList.add(bba);
                } else {
                    otherList.add(bba);
                    if (bba.getAttributeType().getTypeName().equalsIgnoreCase("TSK_ID")) {
                        accountAttributesList.add(bba);
                    }
                }
            }
        } catch (TskCoreException ex) {
            logger.log(Level.SEVERE, String.format("Error getting attributes for artifact (artifact_id=%d, obj_id=%d)", artifact.getArtifactID(), artifact.getObjectID()), ex);
        }

        // update name section
        updateNamePanel(nameList);

        // update contact attributes sections
        updateSection(phoneNumList, this.phonesLabel, this.phoneNumbersPanel);
        updateSection(emailList, this.emailsLabel, this.emailsPanel);
        updateSection(otherList, this.othersLabel, this.otherAttrsPanel);

        try {
            initiatePersonasSearch(accountAttributesList);
        } catch (CentralRepoException ex) {
            logger.log(Level.SEVERE, String.format("Error getting Personas for Contact artifact (artifact_id=%d, obj_id=%d)", artifact.getArtifactID(), artifact.getObjectID()), ex);
        }

        // repaint
        this.revalidate();
        this.repaint();
    }

    @Override
    public Component getComponent() {
        // Slap a vertical scrollbar on the panel.
        return new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Checks if the given artifact is supported by this viewer. This viewer
     * supports TSK_CONTACT artifacts.
     *
     * @param artifact artifact to check.
     * @return True if the artifact is supported, false otherwise.
     */
    @Override
    public boolean isSupported(BlackboardArtifact artifact) {
        return artifact.getArtifactTypeID() == BlackboardArtifact.ARTIFACT_TYPE.TSK_CONTACT.getTypeID();
    }

    /**
     * Clears all artifact specific state.
     */
    private void resetComponent() {
        contactNameLabel.setVisible(false);
        emailsLabel.setVisible(false);
        emailsPanel.removeAll();
        //namePanel.removeAll();    // this is not dynamically populated, do not remove.
        otherAttrsPanel.removeAll();
        othersLabel.setVisible(false);
        personasLabel.setVisible(false);
        personasPanel.removeAll();
        phoneNumbersPanel.removeAll();
        phonesLabel.setVisible(false);

        contactName = null;
        contactUniqueAccountsList.clear();
        contactUniquePersonasMap.clear();
    }

    /**
     * Updates the contact name in the view.
     *
     * @param attributesList
     */
    private void updateNamePanel(List<BlackboardAttribute> attributesList) {
        for (BlackboardAttribute bba : attributesList) {
            if (bba.getAttributeType().getTypeName().startsWith("TSK_NAME")) {
                contactName = bba.getDisplayString();
                contactNameLabel.setText(contactName);
                contactNameLabel.setVisible(true);
                break;
            }
        }

        contactNameLabel.revalidate();
    }

    /**
     * Updates the view by displaying the given list of attributes in the given
     * section panel.
     *
     * @param sectionAttributesList list of attributes to display.
     * @param sectionLabel section name label.
     * @param sectionPanel section panel to display the attributes in.
     */
    private void updateSection(List<BlackboardAttribute> sectionAttributesList, JLabel sectionLabel, JPanel sectionPanel) {

        // If there are no attributes for tis section, hide the section panel and the section label
        if (sectionAttributesList.isEmpty()) {
            sectionLabel.setVisible(false);
            sectionPanel.setVisible(false);
            return;
        }

        // create a gridbag layout to show each attribute on one line
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.gridy = 0;
        constraints.insets = new java.awt.Insets(TOP_INSET, LEFT_INSET, 0, 0);
        for (BlackboardAttribute bba : sectionAttributesList) {
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;

            constraints.gridx = 0;

            // Add a label for attribute type
            javax.swing.JLabel attrTypeLabel = new javax.swing.JLabel();
            String attrLabel = bba.getAttributeType().getDisplayName();
            attrTypeLabel.setText(attrLabel);

            // make type label bold - uncomment if needed.
            //attrTypeLabel.setFont(attrTypeLabel.getFont().deriveFont(Font.BOLD, attrTypeLabel.getFont().getSize() ));
            gridBagLayout.setConstraints(attrTypeLabel, constraints);
            sectionPanel.add(attrTypeLabel);

            // Add the attribute value
            constraints.gridx++;
            javax.swing.JLabel attrValueLabel = new javax.swing.JLabel();
            attrValueLabel.setText(bba.getValueString());
            gridBagLayout.setConstraints(attrValueLabel, constraints);
            sectionPanel.add(attrValueLabel);

            // add a filler to take up rest of the space
            constraints.gridx++;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            sectionPanel.add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0)));

            constraints.gridy++;
        }

        sectionLabel.setVisible(true);
        sectionPanel.setVisible(true);

        sectionPanel.setLayout(gridBagLayout);
        sectionPanel.revalidate();
        sectionPanel.repaint();
    }

    /**
     * Kicks off a search for personas, based in the list of attributes.
     *
     * @param accountAttributesList a list of account identifying attributes.
     *
     * @throws CentralRepoException
     */
    @NbBundle.Messages({
        "ContactArtifactViewer_persona_searching= Searching..."
    })
    private void initiatePersonasSearch(List<BlackboardAttribute> accountAttributesList) throws CentralRepoException {

        personasLabel.setVisible(true);

        // create a gridbag layout to show each participant on one line
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new java.awt.Insets(TOP_INSET, LEFT_INSET, 0, 0);

        // Add a Persona Name label 
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;

        //javax.swing.Box.Filler filler1 = this.createFiller(5, 0);
        //personasPanel.add(filler1, constraints);

        //constraints.gridx++;
        javax.swing.JLabel primaryPersonaNameLabel = new javax.swing.JLabel();
        primaryPersonaNameLabel.setText(Bundle.ContactArtifactViewer_persona_searching());
        gridBagLayout.setConstraints(primaryPersonaNameLabel, constraints);
        personasPanel.add(primaryPersonaNameLabel);

        personasPanel.setLayout(gridBagLayout);
        personasPanel.revalidate();
        personasPanel.repaint();

        // Kick off a background task to serach for personas for the contact
        ContactPersonaSearcherTask personaSearchTask = new ContactPersonaSearcherTask(accountAttributesList);
        personaSearchTask.execute();

    }

    /**
     * Updates the Persona panel with the gathered persona information.
     */
    private void updatePersonasPanel() {
        // Clear out the panel
        personasPanel.removeAll();

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new java.awt.Insets(TOP_INSET, LEFT_INSET, 0, 0);

        if (contactUniquePersonasMap.isEmpty()) {
            showPersona(null, Collections.emptyList(), gridBagLayout, constraints);
        } else {
            for (Map.Entry<Persona, ArrayList<CentralRepoAccount>> entry : contactUniquePersonasMap.entrySet()) {
                List<CentralRepoAccount> missingAccounts = new ArrayList<>();
                ArrayList<CentralRepoAccount> personaAccounts = entry.getValue();

                // create a list of accounts missing from this persona
                for (CentralRepoAccount account : contactUniqueAccountsList) {
                    if (personaAccounts.contains(account) == false) {
                        missingAccounts.add(account);
                    }
                }

                showPersona(entry.getKey(), missingAccounts, gridBagLayout, constraints);
                constraints.gridy += 2;
            }
        }

        personasPanel.setLayout(gridBagLayout);
        personasPanel.setSize(personasPanel.getPreferredSize());
        personasPanel.revalidate();
        personasPanel.repaint();
    }

    @NbBundle.Messages({
        "ContactArtifactViewer_persona_label=Persona ",
        "ContactArtifactViewer_persona_text_none=None found.",
        "ContactArtifactViewer_persona_button_view=View",
        "ContactArtifactViewer_persona_button_new=Create",
        "ContactArtifactViewer_missing_account_label=Missing Account: "
    })

    /**
     * Displays the given persona in the persona panel.
     *
     * @param persona Persona to display.
     * @param missingAccountsList List of accounts this persona may be missing.
     * @param gridBagLayout Layout to use.
     * @param constraints layout constraints.
     *
     * @throws CentralRepoException
     */
    private void showPersona(Persona persona, List<CentralRepoAccount> missingAccountsList, GridBagLayout gridBagLayout, GridBagConstraints constraints) {

        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.gridx = 0;

        //javax.swing.Box.Filler filler1 = createFiller(5, 0);
       // gridBagLayout.setConstraints(filler1, constraints);
        //personasPanel.add(filler1);

        // Add a "Persona: " label
        //constraints.gridx++;
        javax.swing.JLabel personaLabel = new javax.swing.JLabel();
        personaLabel.setText(Bundle.ContactArtifactViewer_persona_label());
        gridBagLayout.setConstraints(personaLabel, constraints);
        personasPanel.add(personaLabel);

        javax.swing.JLabel personaNameLabel = new javax.swing.JLabel();
        javax.swing.JButton personaButton = new javax.swing.JButton();

        String personaName;
        String personaButtonText;
        ActionListener personaButtonListener;

        if (persona != null) {
            personaName = persona.getName();
            personaButtonText = Bundle.ContactArtifactViewer_persona_button_view();
            personaButtonListener = new ViewPersonaButtonListener(persona);
        } else {
            personaName = Bundle.ContactArtifactViewer_persona_text_none();
            personaButtonText = Bundle.ContactArtifactViewer_persona_button_new();
            personaButtonListener = new CreatePersonaButtonListener(new PersonaUIComponents(personaNameLabel, personaButton));
        }

        // Add the label for persona name, 
        constraints.gridx++;
        personaNameLabel.setText(personaName);
        gridBagLayout.setConstraints(personaNameLabel, constraints);
        personasPanel.add(personaNameLabel);

        //constraints.gridx++;
        //personasPanel.add(createFiller(5, 0), constraints);

        // Add a Persona action button
        constraints.gridx++;
        personaButton.setText(personaButtonText);
        personaButton.addActionListener(personaButtonListener);

        // no top inset of the button, in order to center align with the labels.
        constraints.insets = new java.awt.Insets(0, LEFT_INSET, 0, 0);
        gridBagLayout.setConstraints(personaButton, constraints);
        personasPanel.add(personaButton);

        // restore normal inset
        constraints.insets = new java.awt.Insets(TOP_INSET, LEFT_INSET, 0, 0);

        // show missing accounts.
        for (CentralRepoAccount missingAccount : missingAccountsList) {
            constraints.weightx = 0;
            constraints.gridx = 0;
            constraints.gridy++;

            // Add a "Missing Account: " label
            constraints.gridx ++; // Ident 
            javax.swing.JLabel missingAccountLabel = new javax.swing.JLabel();
            missingAccountLabel.setText(Bundle.ContactArtifactViewer_missing_account_label());
            gridBagLayout.setConstraints(missingAccountLabel, constraints);
            personasPanel.add(missingAccountLabel);

            // Add the label for account id, 
            constraints.gridx++;
            javax.swing.JLabel missingAccountIdentifierLabel = new javax.swing.JLabel();
            missingAccountIdentifierLabel.setText(missingAccount.getIdentifier());
            gridBagLayout.setConstraints(missingAccountIdentifierLabel, constraints);
            personasPanel.add(missingAccountIdentifierLabel);
        }
    }

    /**
     * Creates a swing filler.
     *
     * @param width Filler width.
     * @param height Filler height.
     * @return Filler object.
     */
    private javax.swing.Box.Filler createFiller(int width, int height) {
        return new javax.swing.Box.Filler(new Dimension(width, height), new Dimension(width, height), new Dimension(width, height));
    }

    /**
     * Thread to search for a personas for all account identifier attributes for
     * a contact.
     */
    private class ContactPersonaSearcherTask extends SwingWorker<Map<Persona, ArrayList<CentralRepoAccount>>, Void> {

        private final List<BlackboardAttribute> accountAttributesList;
        private final List<CentralRepoAccount> uniqueAccountsList = new ArrayList<>();

        /**
         * Creates a persona searcher task.
         *
         * @param accountAttributesList List of attributes that may map to
         * accounts.
         */
        ContactPersonaSearcherTask(List<BlackboardAttribute> accountAttributesList) {
            this.accountAttributesList = accountAttributesList;
        }

        @Override
        protected Map<Persona, ArrayList<CentralRepoAccount>> doInBackground() throws Exception {

            Map<Persona, ArrayList<CentralRepoAccount>> uniquePersonas = new HashMap<>();

            for (BlackboardAttribute bba : accountAttributesList) {

                // Get account, add to accounts list
                Collection<Persona> personas;

                Collection<CentralRepoAccount> accountCandidates
                        = CentralRepoAccount.getAccountsWithIdentifier(bba.getValueString());

                if (accountCandidates.isEmpty() == false) {
                    CentralRepoAccount account = accountCandidates.iterator().next();
                    if (uniqueAccountsList.contains(account) == false) {
                        uniqueAccountsList.add(account);
                    }

                    // get personas for the account
                    personas = PersonaAccount.getPersonaAccountsForAccount(account.getId())
                            .stream()
                            .map(PersonaAccount::getPersona)
                            .collect(Collectors.toList());

                    // make a list of unique personas, along with all their accounts
                    for (Persona persona : personas) {
                        if (uniquePersonas.containsKey(persona) == false) {
                            Collection<CentralRepoAccount> accounts = persona.getPersonaAccounts()
                                    .stream()
                                    .map(PersonaAccount::getAccount)
                                    .collect(Collectors.toList());

                            ArrayList<CentralRepoAccount> personaAccountsList = new ArrayList<>(accounts);
                            uniquePersonas.put(persona, personaAccountsList);
                        }
                    }
                }

            }

            return uniquePersonas;
        }

        @Override
        protected void done() {

            Map<Persona, ArrayList<CentralRepoAccount>> personasMap;
            try {
                personasMap = super.get();

                if (this.isCancelled()) {
                    return;
                }

                contactUniquePersonasMap.clear();
                contactUniquePersonasMap.putAll(personasMap);
                contactUniqueAccountsList.clear();
                contactUniqueAccountsList.addAll(uniqueAccountsList);

                updatePersonasPanel();

            } catch (CancellationException ex) {
                logger.log(Level.INFO, "Persona searching was canceled."); //NON-NLS
            } catch (InterruptedException ex) {
                logger.log(Level.INFO, "Persona searching was interrupted."); //NON-NLS
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, "Fatal error during Persona search.", ex); //NON-NLS
            }

        }
    }

    /**
     * A wrapper class that bags the UI components that need to be updated when
     * a persona search task or a create dialog returns.
     */
    private class PersonaUIComponents {

        private final JLabel personaNameLabel;
        private final JButton personaActionButton;

        /**
         * Constructor.
         *
         * @param personaNameLabel Persona name label.
         * @param personaActionButton Persona action button.
         */
        PersonaUIComponents(JLabel personaNameLabel, JButton personaActionButton) {
            this.personaNameLabel = personaNameLabel;
            this.personaActionButton = personaActionButton;
        }

        /**
         * Returns persona name label.
         *
         * @return Persona name label.
         */
        public JLabel getPersonaNameLabel() {
            return personaNameLabel;
        }

        /**
         * Returns persona action button.
         *
         * @return Persona action button.
         */
        public JButton getPersonaActionButton() {
            return personaActionButton;
        }
    }

    /**
     * Action listener for Create persona button.
     */
    private class CreatePersonaButtonListener implements ActionListener {

        private final PersonaUIComponents personaUIComponents;

        /**
         * Constructs a listener for Create persona button..
         *
         * @param personaUIComponents UI components.
         */
        CreatePersonaButtonListener(PersonaUIComponents personaUIComponents) {
            this.personaUIComponents = personaUIComponents;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            // Launch the Persona Create dialog - do not display immediately
            PersonaDetailsDialog createPersonaDialog = new PersonaDetailsDialog(ContactArtifactViewer.this,
                    PersonaDetailsMode.CREATE, null, new PersonaCreateCallbackImpl(personaUIComponents), false);

            // Pre populate the persona name and accounts if we have them.
            PersonaDetailsPanel personaPanel = createPersonaDialog.getDetailsPanel();

            if (contactName != null) {
                personaPanel.setPersonaName(contactName);
            }

            // pass the list of accounts to the dialog
            for (CentralRepoAccount account : contactUniqueAccountsList) {
                personaPanel.addAccount(account, "Account found in Contact artifact.", Persona.Confidence.UNKNOWN);
            }

            // display the dialog now
            createPersonaDialog.display();
        }
    }

    /**
     * Action listener for View persona button.
     */
    private class ViewPersonaButtonListener implements ActionListener {

        private final Persona persona;

        /**
         * Creates listener for View persona button.
         *
         * @param persona
         */
        ViewPersonaButtonListener(Persona persona) {
            this.persona = persona;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            new PersonaDetailsDialog(ContactArtifactViewer.this,
                    PersonaDetailsMode.VIEW, persona, new PersonaViewCallbackImpl());
        }
    }

    /**
     * Callback method for the create mode of the PersonaDetailsDialog
     */
    class PersonaCreateCallbackImpl implements PersonaDetailsDialogCallback {

        private final PersonaUIComponents personaUIComponents;

        /**
         * Creates a callback to handle new persona creation.
         *
         * @param personaUIComponents UI Components.
         */
        PersonaCreateCallbackImpl(PersonaUIComponents personaUIComponents) {
            this.personaUIComponents = personaUIComponents;
        }

        @Override
        public void callback(Persona persona) {
            JButton personaButton = personaUIComponents.getPersonaActionButton();
            if (persona != null) {
                // update the persona name label with newly created persona, 
                // and change the button to a "View" button
                personaUIComponents.getPersonaNameLabel().setText(persona.getName());
                personaUIComponents.getPersonaActionButton().setText(Bundle.ContactArtifactViewer_persona_button_view());

                // replace action listener with a View button listener
                for (ActionListener act : personaButton.getActionListeners()) {
                    personaButton.removeActionListener(act);
                }
                personaButton.addActionListener(new ViewPersonaButtonListener(persona));

            }

            personaButton.getParent().revalidate();
            personaButton.getParent().repaint();
        }
    }

    /**
     * Callback method for the view mode of the PersonaDetailsDialog
     */
    class PersonaViewCallbackImpl implements PersonaDetailsDialogCallback {

        @Override
        public void callback(Persona persona) {
            // nothing to do 
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler bottomFiller;
    private javax.swing.JLabel contactNameLabel;
    private javax.swing.JLabel emailsLabel;
    private javax.swing.JPanel emailsPanel;
    private javax.swing.Box.Filler interPanelfiller;
    private javax.swing.JPanel namePanel;
    private javax.swing.JPanel otherAttrsPanel;
    private javax.swing.JLabel othersLabel;
    private javax.swing.JLabel personasLabel;
    private javax.swing.JPanel personasPanel;
    private javax.swing.JPanel phoneNumbersPanel;
    private javax.swing.JLabel phonesLabel;
    private javax.swing.Box.Filler rightFiller;
    // End of variables declaration//GEN-END:variables
}
