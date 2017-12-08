/*
 * Central Repository
 *
 * Copyright 2015-2017 Basis Technology Corp.
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
package org.sleuthkit.autopsy.centralrepository.datamodel;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import org.apache.commons.dbcp2.BasicDataSource;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.TskData;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coordinationservice.CoordinationService;

/**
 * Sqlite implementation of the Central Repository database.
 * All methods in AbstractSqlEamDb that read or write to the database should
 * be overriden here and use appropriate locking.
 */
public class SqliteEamDb extends AbstractSqlEamDb {

    private final static Logger LOGGER = Logger.getLogger(SqliteEamDb.class.getName());

    private static SqliteEamDb instance;

    private BasicDataSource connectionPool = null;

    private final SqliteEamDbSettings dbSettings;
    
    // While the Sqlite database should only be used for single users, it is still
    // possible for multiple threads to attempt to write to the database simultaneously. 
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    /**
     * Get the singleton instance of SqliteEamDb
     * 
     * @return the singleton instance of SqliteEamDb
     * 
     * @throws EamDbException if one or more default correlation type(s) have an invalid db table name.
     */
    public synchronized static SqliteEamDb getInstance() throws EamDbException {
        if (instance == null) {
            instance = new SqliteEamDb();
        }

        return instance;
    }

    /**
     * 
     * @throws EamDbException if the AbstractSqlEamDb class has one or more default
     *      correlation type(s) having an invalid db table name.
     */
    private SqliteEamDb() throws EamDbException {
        dbSettings = new SqliteEamDbSettings();
        bulkArtifactsThreshold = dbSettings.getBulkThreshold();
    }

    @Override
    public void shutdownConnections() throws EamDbException {
        try {
            synchronized(this) {
                if (null != connectionPool) {
                    connectionPool.close();
                    connectionPool = null; // force it to be re-created on next connect()
                }
            }
        } catch (SQLException ex) {
            throw new EamDbException("Failed to close existing database connections.", ex); // NON-NLS
        }
    }
    
    @Override
    public void updateSettings() {
        synchronized (this) {
            dbSettings.loadSettings();
            bulkArtifactsThreshold = dbSettings.getBulkThreshold();
        }
    }

    @Override
    public void saveSettings() {
        synchronized (this) {
            dbSettings.saveSettings();
        }
    }

    @Override
    public void reset() throws EamDbException {
        try{
            acquireExclusiveLock();
        
            Connection conn = connect();

            try {

                Statement dropContent = conn.createStatement();
                dropContent.executeUpdate("DELETE FROM organizations");
                dropContent.executeUpdate("DELETE FROM cases");
                dropContent.executeUpdate("DELETE FROM data_sources");
                dropContent.executeUpdate("DELETE FROM reference_sets");
                dropContent.executeUpdate("DELETE FROM artifact_types");
                dropContent.executeUpdate("DELETE FROM db_info");

                String instancesTemplate = "DELETE FROM %s_instances";
                String referencesTemplate = "DELETE FROM global_files";
                for (CorrelationAttribute.Type type : DEFAULT_CORRELATION_TYPES) {
                    dropContent.executeUpdate(String.format(instancesTemplate, type.getDbTableName()));
                    // FUTURE: support other reference types
                    if (type.getId() == CorrelationAttribute.FILES_TYPE_ID) {
                        dropContent.executeUpdate(String.format(referencesTemplate, type.getDbTableName()));
                    }
                }

                dropContent.executeUpdate("VACUUM");
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to reset database.", ex);
            } finally {
                EamDbUtil.closeConnection(conn);
            }

            dbSettings.insertDefaultDatabaseContent();
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * Setup a connection pool for db connections.
     *
     */
    private void setupConnectionPool() throws EamDbException {
        
        if (dbSettings.dbFileExists() == false) {
            throw new EamDbException("Central repository database missing");
        }
        
        connectionPool = new BasicDataSource();
        connectionPool.setDriverClassName(dbSettings.getDriver());
        connectionPool.setUrl(dbSettings.getConnectionURL());

        // tweak pool configuration
        connectionPool.setInitialSize(50);
        connectionPool.setMaxTotal(-1);
        connectionPool.setMaxIdle(-1);
        connectionPool.setMaxWaitMillis(1000);
        connectionPool.setValidationQuery(dbSettings.getValidationQuery());
    }

    /**
     * Lazily setup Singleton connection on first request.
     *
     * @return A connection from the connection pool.
     *
     * @throws EamDbException
     */
    @Override
    protected Connection connect() throws EamDbException {
        synchronized (this) {
            if (!EamDb.isEnabled()) {
                throw new EamDbException("Central Repository module is not enabled"); // NON-NLS
            }

            if (connectionPool == null) {
                setupConnectionPool();
            }

            try {
                return connectionPool.getConnection();
            } catch (SQLException ex) {
                throw new EamDbException("Error getting connection from connection pool.", ex); // NON-NLS
            }
        }
    }

    @Override
    protected String getConflictClause() {
        // For sqlite, our conflict clause is part of the table schema
        return "";
    }

   
    /**
     * Add a new name/value pair in the db_info table.
     *
     * @param name  Key to set
     * @param value Value to set
     *
     * @throws EamDbException
     */
    @Override
    public void newDbInfo(String name, String value) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.newDbInfo(name, value);
        } finally {
            releaseExclusiveLock();
        }
    }
    
    /**
     * Get the value for the given name from the name/value db_info table.
     *
     * @param name Name to search for
     *
     * @return value associated with name.
     *
     * @throws EamDbException
     */
    @Override
    public String getDbInfo(String name) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getDbInfo(name);
        } finally {
            releaseSharedLock();
        }    
    }
    
    /**
     * Update the value for a name in the name/value db_info table.
     *
     * @param name  Name to find
     * @param value Value to assign to name.
     *
     * @throws EamDbException
     */
    @Override
    public void updateDbInfo(String name, String value) throws EamDbException {
         try{
            acquireExclusiveLock();
            super.updateDbInfo(name, value);
        } finally {
            releaseExclusiveLock();
        }       
    }
    
     /**
     * Creates new Case in the database from the given case
     * 
     * @param autopsyCase The case to add
     */
    @Override
    public CorrelationCase newCase(Case autopsyCase) throws EamDbException {
         try{
            acquireExclusiveLock();
            return super.newCase(autopsyCase);
        } finally {
            releaseExclusiveLock();
        }          
    }    
    
    /**
     * Creates new Case in the database
     *
     * Expects the Organization for this case to already exist in the database.
     *
     * @param eamCase The case to add
     */
    @Override
    public CorrelationCase newCase(CorrelationCase eamCase) throws EamDbException {
         try{
            acquireExclusiveLock();
            return super.newCase(eamCase);
        } finally {
            releaseExclusiveLock();
        }          
    }
    
    /**
     * Updates an existing Case in the database
     *
     * @param eamCase The case to update
     */
    @Override
    public void updateCase(CorrelationCase eamCase) throws EamDbException {
         try{
            acquireExclusiveLock();
            super.updateCase(eamCase);
        } finally {
            releaseExclusiveLock();
        }           
    }    
    
    /**
     * Retrieves Case details based on Case UUID
     *
     * @param caseUUID unique identifier for a case
     *
     * @return The retrieved case
     */
    @Override
    public CorrelationCase getCaseByUUID(String caseUUID) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCaseByUUID(caseUUID);
        } finally {
            releaseSharedLock();
        }            
    }   
    
    /**
     * Retrieves cases that are in DB.
     *
     * @return List of cases
     */
    @Override
    public List<CorrelationCase> getCases() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCases();
        } finally {
            releaseSharedLock();
        }            
    } 
    
    /**
     * Creates new Data Source in the database
     *
     * @param eamDataSource the data source to add
     */
    @Override
    public void newDataSource(CorrelationDataSource eamDataSource) throws EamDbException {
         try{
            acquireExclusiveLock();
            super.newDataSource(eamDataSource);
        } finally {
            releaseExclusiveLock();
        }            
    }    
     
    /**
     * Retrieves Data Source details based on data source device ID
     *
     * @param correlationCase the current CorrelationCase used for ensuring uniqueness of DataSource
     * @param dataSourceDeviceId the data source device ID number
     *
     * @return The data source
     */
    @Override
    public CorrelationDataSource getDataSource(CorrelationCase correlationCase, String dataSourceDeviceId) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getDataSource(correlationCase, dataSourceDeviceId);
        } finally {
            releaseSharedLock();
        }               
    }    
    
    /**
     * Return a list of data sources in the DB
     *
     * @return list of data sources in the DB
     */
    @Override
    public List<CorrelationDataSource> getDataSources() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getDataSources();
        } finally {
            releaseSharedLock();
        }              
    }  
    
    /**
     * Inserts new Artifact(s) into the database. Should add associated Case and
     * Data Source first.
     *
     * @param eamArtifact The artifact to add
     */
    @Override
    public void addArtifact(CorrelationAttribute eamArtifact) throws EamDbException {
         try{
            acquireExclusiveLock();
            super.addArtifact(eamArtifact);
        } finally {
            releaseExclusiveLock();
        }                
    }    
    
    /**
     * Retrieves eamArtifact instances from the database that are associated
     * with the eamArtifactType and eamArtifactValue of the given eamArtifact.
     *
     * @param aType  The type of the artifact
     * @param value  The correlation value
     *
     * @return List of artifact instances for a given type/value
     */
    @Override
    public List<CorrelationAttributeInstance> getArtifactInstancesByTypeValue(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getArtifactInstancesByTypeValue(aType, value);
        } finally {
            releaseSharedLock();
        }            
    }    
    
    /**
     * Retrieves eamArtifact instances from the database that are associated
     * with the aType and filePath
     *
     * @param aType    EamArtifact.Type to search for
     * @param filePath File path to search for
     *
     * @return List of 0 or more EamArtifactInstances
     *
     * @throws EamDbException
     */
    @Override
    public List<CorrelationAttributeInstance> getArtifactInstancesByPath(CorrelationAttribute.Type aType, String filePath) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getArtifactInstancesByPath(aType, filePath);
        } finally {
            releaseSharedLock();
        }              
    }   
    
    /**
     * Retrieves number of artifact instances in the database that are
     * associated with the ArtifactType and artifactValue of the given artifact.
     *
     * @param aType The correlation type
     * @param value The value to search for
     *
     * @return Number of artifact instances having ArtifactType and
     *         ArtifactValue.
     * @throws EamDbException 
     */
    @Override
    public Long getCountArtifactInstancesByTypeValue(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCountArtifactInstancesByTypeValue(aType, value);
        } finally {
            releaseSharedLock();
        }    
    }
    
    @Override
    public int getFrequencyPercentage(CorrelationAttribute corAttr) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getFrequencyPercentage(corAttr);
        } finally {
            releaseSharedLock();
        }            
    }    
    
    /**
     * Retrieves number of unique caseDisplayName / dataSource tuples in the
     * database that are associated with the artifactType and artifactValue of
     * the given artifact.
     *
     * @param aType The correlation type
     * @param value The value to search for
     *
     * @return Number of unique tuples
     * @throws EamDbException
     */
    @Override
    public Long getCountUniqueCaseDataSourceTuplesHavingTypeValue(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCountUniqueCaseDataSourceTuplesHavingTypeValue(aType, value);
        } finally {
            releaseSharedLock();
        }  
    }    
    

    @Override
    public Long getCountUniqueDataSources() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCountUniqueDataSources();
        } finally {
            releaseSharedLock();
        }  
    }    
    
    /**
     * Retrieves number of eamArtifact instances in the database that are
     * associated with the caseDisplayName and dataSource of the given
     * eamArtifact instance.
     *
     * @param caseUUID     Case ID to search for
     * @param dataSourceID Data source ID to search for
     *
     * @return Number of artifact instances having caseDisplayName and
     *         dataSource
     */
    @Override
    public Long getCountArtifactInstancesByCaseDataSource(String caseUUID, String dataSourceID) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCountArtifactInstancesByCaseDataSource(caseUUID, dataSourceID);
        } finally {
            releaseSharedLock();
        }          
    }    
    
    /**
     * Executes a bulk insert of the eamArtifacts added from the
     * prepareBulkArtifact() method
     */
    @Override
    public void bulkInsertArtifacts() throws EamDbException {
        try{
            acquireExclusiveLock();
            super.bulkInsertArtifacts();
        } finally {
            releaseExclusiveLock();
        }            
    }    
    
    /**
     * Executes a bulk insert of the cases
     */
    @Override
    public void bulkInsertCases(List<CorrelationCase> cases) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.bulkInsertCases(cases);
        } finally {
            releaseExclusiveLock();
        }         
    }    
    
    /**
     * Sets an eamArtifact instance as the given knownStatus. If eamArtifact
     * exists, it is updated. If eamArtifact does not exist nothing happens
     *
     * @param eamArtifact Artifact containing exactly one (1) ArtifactInstance.
     * @param knownStatus The known status of the artifact
     */
    @Override
    public void setArtifactInstanceKnownStatus(CorrelationAttribute eamArtifact, TskData.FileKnown knownStatus) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.setArtifactInstanceKnownStatus(eamArtifact, knownStatus);
        } finally {
            releaseExclusiveLock();
        }     
    }   
    
    /**
     * Gets list of matching eamArtifact instances that have knownStatus =
     * "Bad".
     *
     * @param aType EamArtifact.Type to search for
     * @param value Value to search for
     * 
     * @return List with 0 or more matching eamArtifact instances.
     */
    @Override
    public List<CorrelationAttributeInstance> getArtifactInstancesKnownBad(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getArtifactInstancesKnownBad(aType, value);
        } finally {
            releaseSharedLock();
        }       
    }    
    
    /**
     * Count matching eamArtifacts instances that have knownStatus = "Bad".
     *
     * @param aType EamArtifact.Type to search for
     * @param value Value to search for
     *
     * @return Number of matching eamArtifacts
     */
    @Override
    public Long getCountArtifactInstancesKnownBad(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCountArtifactInstancesKnownBad(aType, value);
        } finally {
            releaseSharedLock();
        }             
    }    
    
    /**
     * Gets list of distinct case display names, where each case has 1+ Artifact
     * Instance matching eamArtifact with knownStatus = "Bad".
     *
     * @param aType EamArtifact.Type to search for
     * @param value Value to search for
     *
     * @return List of cases containing this artifact with instances marked as
     *         bad
     *
     * @throws EamDbException
     */
    @Override
    public List<String> getListCasesHavingArtifactInstancesKnownBad(CorrelationAttribute.Type aType, String value) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getListCasesHavingArtifactInstancesKnownBad(aType, value);
        } finally {
            releaseSharedLock();
        }       
    }    
    
    /**
     * Remove a reference set and all values contained in it.
     * @param referenceSetID
     * @throws EamDbException 
     */
    @Override
    public void deleteReferenceSet(int referenceSetID) throws EamDbException{
        try{
            acquireExclusiveLock();
            super.deleteReferenceSet(referenceSetID);
        } finally {
            releaseExclusiveLock();
        }   
    }    
    
    /**
     * Check if the given hash is in a specific reference set
     * @param hash
     * @param referenceSetID
     * @return true if the hash is found in the reference set
     */
    @Override
    public boolean isValueInReferenceSet(String value, int referenceSetID, int correlationTypeID) throws EamDbException {
        try{
            acquireSharedLock();
            return super.isValueInReferenceSet(value, referenceSetID, correlationTypeID);
        } finally {
            releaseSharedLock();
        }          
    }
    
    /**
     * Check whether a reference set with the given name/version is in the central repo.
     * Used to check for name collisions when creating reference sets.
     * @param referenceSetName
     * @param version
     * @return true if a matching set is found
     * @throws EamDbException 
     */
    @Override
    public boolean referenceSetExists(String referenceSetName, String version) throws EamDbException {
        try{
            acquireSharedLock();
            return super.referenceSetExists(referenceSetName, version);
        } finally {
            releaseSharedLock();
        }  
    }
    
    /**
     * Is the artifact known as bad according to the reference entries?
     *
     * @param aType EamArtifact.Type to search for
     * @param value Value to search for
     *
     * @return Global known status of the artifact
     */
    @Override
    public boolean isArtifactKnownBadByReference(CorrelationAttribute.Type aType, String value) throws EamDbException {   
        try{
            acquireSharedLock();
            return super.isArtifactKnownBadByReference(aType, value);
        } finally {
            releaseSharedLock();
        }      
    }
    
    /**
     * Add a new organization
     *
     * @return the Organization ID of the newly created organization.
     * 
     * @param eamOrg The organization to add
     *
     * @throws EamDbException
     */
    @Override
    public long newOrganization(EamOrganization eamOrg) throws EamDbException {
        try{
            acquireExclusiveLock();
            return super.newOrganization(eamOrg);
        } finally {
            releaseExclusiveLock();
        }         
    }    
    
    /**
     * Get all organizations
     *
     * @return A list of all organizations
     *
     * @throws EamDbException
     */
    @Override
    public List<EamOrganization> getOrganizations() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getOrganizations();
        } finally {
            releaseSharedLock();
        }    
    }    
    
    /**
     * Get an organization having the given ID
     *
     * @param orgID The id to look up
     *
     * @return The organization with the given ID
     *
     * @throws EamDbException
     */
    @Override
    public EamOrganization getOrganizationByID(int orgID) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getOrganizationByID(orgID);
        } finally {
            releaseSharedLock();
        }    
    } 
    
    @Override
    public void updateOrganization(EamOrganization updatedOrganization) throws EamDbException {
          try{
            acquireExclusiveLock();
            super.updateOrganization(updatedOrganization);
        } finally {
            releaseExclusiveLock();
        }      
    }
    
    @Override
    public void deleteOrganization(EamOrganization organizationToDelete) throws EamDbException {
          try{
            acquireExclusiveLock();
            super.deleteOrganization(organizationToDelete);
        } finally {
            releaseExclusiveLock();
        }      
    }
    /**
     * Add a new Global Set
     *
     * @param eamGlobalSet The global set to add
     *
     * @return The ID of the new global set
     *
     * @throws EamDbException
     */
    @Override
    public int newReferenceSet(EamGlobalSet eamGlobalSet) throws EamDbException {
        try{
            acquireExclusiveLock();
            return super.newReferenceSet(eamGlobalSet);
        } finally {
            releaseExclusiveLock();
        }     
    }    
    
    /**
     * Get a reference set by ID
     *
     * @param referenceSetID The ID to look up
     *
     * @return The global set associated with the ID
     *
     * @throws EamDbException
     */
    @Override
    public EamGlobalSet getReferenceSetByID(int referenceSetID) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getReferenceSetByID(referenceSetID);
        } finally {
            releaseSharedLock();
        }            
    }    
    
    /**
     * Get all reference sets
     *
     * @return List of all reference sets in the central repository
     *
     * @throws EamDbException
     */
    @Override
    public List<EamGlobalSet> getAllReferenceSets() throws EamDbException{
        try{
            acquireSharedLock();
            return super.getAllReferenceSets();
        } finally {
            releaseSharedLock();
        } 
    }
    
    /**
     * Add a new reference instance
     *
     * @param eamGlobalFileInstance The reference instance to add
     * @param correlationType       Correlation Type that this Reference
     *                              Instance is
     *
     * @throws EamDbException
     */
    @Override
    public void addReferenceInstance(EamGlobalFileInstance eamGlobalFileInstance, CorrelationAttribute.Type correlationType) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.addReferenceInstance(eamGlobalFileInstance, correlationType);
        } finally {
            releaseExclusiveLock();
        }  
    }  
    
    /**
     * Insert the bulk collection of Reference Type Instances
     *
     * @throws EamDbException
     */
    @Override
    public void bulkInsertReferenceTypeEntries(Set<EamGlobalFileInstance> globalInstances, CorrelationAttribute.Type contentType) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.bulkInsertReferenceTypeEntries(globalInstances, contentType);
        } finally {
            releaseExclusiveLock();
        }  
    }    
    
    /**
     * Get all reference entries having a given correlation type and value
     *
     * @param aType  Type to use for matching
     * @param aValue Value to use for matching
     *
     * @return List of all global file instances with a type and value
     *
     * @throws EamDbException
     */
    @Override
    public List<EamGlobalFileInstance> getReferenceInstancesByTypeValue(CorrelationAttribute.Type aType, String aValue) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getReferenceInstancesByTypeValue(aType, aValue);
        } finally {
            releaseSharedLock();
        }    
    }    
    
    /**
     * Add a new EamArtifact.Type to the db.
     *
     * @param newType New type to add.
     *
     * @return ID of this new Correlation Type
     *
     * @throws EamDbException
     */
    @Override
    public int newCorrelationType(CorrelationAttribute.Type newType) throws EamDbException {
        try{
            acquireExclusiveLock();
            return super.newCorrelationType(newType);
        } finally {
            releaseExclusiveLock();
        }  
    }    
    
    /**
     * Get the list of EamArtifact.Type's that will be used to correlate
     * artifacts.
     *
     * @return List of EamArtifact.Type's. If none are defined in the database,
     *         the default list will be returned.
     *
     * @throws EamDbException
     */
    @Override
    public List<CorrelationAttribute.Type> getDefinedCorrelationTypes() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getDefinedCorrelationTypes();
        } finally {
            releaseSharedLock();
        }    
    }    
    
    /**
     * Get the list of enabled EamArtifact.Type's that will be used to correlate
     * artifacts.
     *
     * @return List of enabled EamArtifact.Type's. If none are defined in the
     *         database, the default list will be returned.
     *
     * @throws EamDbException
     */
    @Override
    public List<CorrelationAttribute.Type> getEnabledCorrelationTypes() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getEnabledCorrelationTypes();
        } finally {
            releaseSharedLock();
        }   
    }    
    
    /**
     * Get the list of supported EamArtifact.Type's that can be used to
     * correlate artifacts.
     *
     * @return List of supported EamArtifact.Type's. If none are defined in the
     *         database, the default list will be returned.
     *
     * @throws EamDbException
     */
    @Override
    public List<CorrelationAttribute.Type> getSupportedCorrelationTypes() throws EamDbException {
        try{
            acquireSharedLock();
            return super.getSupportedCorrelationTypes();
        } finally {
            releaseSharedLock();
        }  
    }    
    
    /**
     * Update a EamArtifact.Type.
     *
     * @param aType EamArtifact.Type to update.
     *
     * @throws EamDbException
     */
    @Override
    public void updateCorrelationType(CorrelationAttribute.Type aType) throws EamDbException {
        try{
            acquireExclusiveLock();
            super.updateCorrelationType(aType);
        } finally {
            releaseExclusiveLock();
        } 
    } 
    
    /**
     * Get the EamArtifact.Type that has the given Type.Id.
     *
     * @param typeId Type.Id of Correlation Type to get
     *
     * @return EamArtifact.Type or null if it doesn't exist.
     *
     * @throws EamDbException
     */
    @Override
    public CorrelationAttribute.Type getCorrelationTypeById(int typeId) throws EamDbException {
        try{
            acquireSharedLock();
            return super.getCorrelationTypeById(typeId);
        } finally {
            releaseSharedLock();
        }  
    }   
    
    /**
     * Upgrade the schema of the database (if needed)
     * @throws EamDbException 
     */
    @Override
    public void upgradeSchema() throws EamDbException, SQLException {
        try{
            acquireExclusiveLock();
            super.upgradeSchema();
        } finally {
            releaseExclusiveLock();
        } 
    }
    
    /**
     * Gets an exclusive lock (if applicable).
     * Will return the lock if successful, null if unsuccessful because locking
     * isn't supported, and throw an exception if we should have been able to get the
     * lock but failed (meaning the database is in use).
     * @return the lock, or null if locking is not supported
     * @throws EamDbException if the coordination service is running but we fail to get the lock
     */
    @Override
    public CoordinationService.Lock getExclusiveMultiUserDbLock() throws EamDbException{
        // Multiple users are not supported for SQLite
        return null;
    }
    
    /**
     * Acquire the lock that provides exclusive access to the case database. 
     * Call this method in a try block with a call to
     * the lock release method in an associated finally block.
     */
    private void acquireExclusiveLock() {
        rwLock.writeLock().lock();
    }

    /**
     * Release the lock that provides exclusive access to the database.
     * This method should always be called in the finally
     * block of a try block in which the lock was acquired.
     */
    private void releaseExclusiveLock() {
        rwLock.writeLock().unlock();
    }

    /**
     * Acquire the lock that provides shared access to the case database.
     * Call this method in a try block with a call to the
     * lock release method in an associated finally block.
     */
    private void acquireSharedLock() {
        rwLock.readLock().lock();
    }

    /**
     * Release the lock that provides shared access to the database.
     * This method should always be called in the finally block
     * of a try block in which the lock was acquired.
     */
    private void releaseSharedLock() {
        rwLock.readLock().unlock();
    }

}
