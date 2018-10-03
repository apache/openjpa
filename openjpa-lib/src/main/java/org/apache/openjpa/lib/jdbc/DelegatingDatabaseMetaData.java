/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.lib.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * Wrapper around a DatabaseMetaData instance.
 *
 * @author Marc Prud'hommeaux
 */
public class DelegatingDatabaseMetaData implements DatabaseMetaData {

    private final DatabaseMetaData _metaData;
    private final Connection _conn;

    public DelegatingDatabaseMetaData(DatabaseMetaData metaData,
        Connection conn) {
        _conn = conn;
        _metaData = metaData;
    }

    /**
     * Return the base underlying database metadata.
     */
    public DatabaseMetaData getInnermostDelegate() {
        return _metaData instanceof DelegatingDatabaseMetaData ?
            ((DelegatingDatabaseMetaData) _metaData).getInnermostDelegate()
            : _metaData;
    }

    @Override
    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingDatabaseMetaData)
            other = ((DelegatingDatabaseMetaData) other)
                .getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("metadata ").append(hashCode());
        buf.append("[").append(_metaData.toString()).append("]");
        return buf.toString();
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return _metaData.allProceduresAreCallable();
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return _metaData.allTablesAreSelectable();
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return _metaData.dataDefinitionCausesTransactionCommit();
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return _metaData.dataDefinitionIgnoredInTransactions();
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        return _metaData.deletesAreDetected(type);
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return _metaData.doesMaxRowSizeIncludeBlobs();
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog,
        String schema, String table, int scope, boolean nullable)
        throws SQLException {
        return _metaData.getBestRowIdentifier(catalog, schema,
            table, scope, nullable);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        return _metaData.getCatalogs();
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        return _metaData.getCatalogSeparator();
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        return _metaData.getCatalogTerm();
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema,
        String table, String columnNamePattern) throws SQLException {
        return _metaData.getColumnPrivileges(catalog, schema,
            table, columnNamePattern);
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern,
        String tableNamePattern, String columnNamePattern) throws SQLException {
        return _metaData.getColumns(catalog, schemaPattern,
            tableNamePattern, columnNamePattern);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return _conn;
    }

    @Override
    public ResultSet getCrossReference(String primaryCatalog,
        String primarySchema, String primaryTable, String foreignCatalog,
        String foreignSchema, String foreignTable) throws SQLException {
        return _metaData.getCrossReference(primaryCatalog, primarySchema,
            primaryTable, foreignCatalog, foreignSchema, foreignTable);
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return _metaData.getDatabaseProductName();
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return _metaData.getDatabaseProductVersion();
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        return _metaData.getDefaultTransactionIsolation();
    }

    @Override
    public int getDriverMajorVersion() {
        return _metaData.getDriverMajorVersion();
    }

    @Override
    public int getDriverMinorVersion() {
        return _metaData.getDriverMinorVersion();
    }

    @Override
    public String getDriverName() throws SQLException {
        return _metaData.getDriverName();
    }

    @Override
    public String getDriverVersion() throws SQLException {
        return _metaData.getDriverVersion();
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema,
        String table) throws SQLException {
        return _metaData.getExportedKeys(catalog, schema, table);
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        return _metaData.getExtraNameCharacters();
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        return _metaData.getIdentifierQuoteString();
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema,
        String table) throws SQLException {
        return _metaData.getImportedKeys(catalog, schema, table);
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema,
        String table, boolean unique, boolean approximate) throws SQLException {
        return _metaData.getIndexInfo(catalog, schema, table, unique,
            approximate);
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        return _metaData.getMaxBinaryLiteralLength();
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        return _metaData.getMaxCatalogNameLength();
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        return _metaData.getMaxCharLiteralLength();
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        return _metaData.getMaxColumnNameLength();
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        return _metaData.getMaxColumnsInGroupBy();
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        return _metaData.getMaxColumnsInIndex();
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        return _metaData.getMaxColumnsInOrderBy();
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        return _metaData.getMaxColumnsInSelect();
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        return _metaData.getMaxColumnsInTable();
    }

    @Override
    public int getMaxConnections() throws SQLException {
        return _metaData.getMaxConnections();
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        return _metaData.getMaxCursorNameLength();
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        return _metaData.getMaxIndexLength();
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        return _metaData.getMaxProcedureNameLength();
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        return _metaData.getMaxRowSize();
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        return _metaData.getMaxSchemaNameLength();
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        return _metaData.getMaxStatementLength();
    }

    @Override
    public int getMaxStatements() throws SQLException {
        return _metaData.getMaxStatements();
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        return _metaData.getMaxTableNameLength();
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        return _metaData.getMaxTablesInSelect();
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        return _metaData.getMaxUserNameLength();
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        return _metaData.getNumericFunctions();
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
        throws SQLException {
        return _metaData.getPrimaryKeys(catalog, schema, table);
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
        String procedureNamePattern, String columnNamePattern)
        throws SQLException {
        return _metaData.getProcedureColumns(catalog, schemaPattern,
            procedureNamePattern, columnNamePattern);
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern,
        String procedureNamePattern) throws SQLException {
        return _metaData.getProcedures(catalog, schemaPattern,
            procedureNamePattern);
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        return _metaData.getProcedureTerm();
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return _metaData.getSchemas();
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        return _metaData.getSchemaTerm();
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        return _metaData.getSearchStringEscape();
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        return _metaData.getSQLKeywords();
    }

    @Override
    public String getStringFunctions() throws SQLException {
        return _metaData.getStringFunctions();
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        return _metaData.getSystemFunctions();
    }

    @Override
    public ResultSet getTablePrivileges(String catalog,
        String schemaPattern, String tableNamePattern) throws SQLException {
        return _metaData.getTablePrivileges(catalog, schemaPattern,
            tableNamePattern);
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern,
        String tableNamePattern, String[] types) throws SQLException {
        return _metaData.getTables(catalog, schemaPattern,
            tableNamePattern, types);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        return _metaData.getTableTypes();
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        return _metaData.getTimeDateFunctions();
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return _metaData.getTypeInfo();
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern,
        String typeNamePattern, int[] types) throws SQLException {
        return _metaData.getUDTs(catalog, schemaPattern,
            typeNamePattern, types);
    }

    @Override
    public String getURL() throws SQLException {
        return _metaData.getURL();
    }

    @Override
    public String getUserName() throws SQLException {
        return _metaData.getUserName();
    }

    @Override
    public ResultSet getVersionColumns(String catalog,
        String schema, String table) throws SQLException {
        return _metaData.getVersionColumns(catalog, schema, table);
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        return _metaData.insertsAreDetected(type);
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        return _metaData.isCatalogAtStart();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return _metaData.isReadOnly();
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return _metaData.nullPlusNonNullIsNull();
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return _metaData.nullsAreSortedAtEnd();
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        return _metaData.nullsAreSortedAtStart();
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        return _metaData.nullsAreSortedHigh();
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        return _metaData.nullsAreSortedLow();
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return _metaData.othersDeletesAreVisible(type);
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return _metaData.othersInsertsAreVisible(type);
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return _metaData.othersUpdatesAreVisible(type);
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return _metaData.ownDeletesAreVisible(type);
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return _metaData.ownInsertsAreVisible(type);
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return _metaData.ownUpdatesAreVisible(type);
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return _metaData.storesLowerCaseIdentifiers();
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesLowerCaseQuotedIdentifiers();
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return _metaData.storesMixedCaseIdentifiers();
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesMixedCaseQuotedIdentifiers();
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return _metaData.storesUpperCaseIdentifiers();
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesUpperCaseQuotedIdentifiers();
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return _metaData.supportsAlterTableWithAddColumn();
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return _metaData.supportsAlterTableWithDropColumn();
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return _metaData.supportsANSI92EntryLevelSQL();
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        return _metaData.supportsANSI92FullSQL();
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return _metaData.supportsANSI92IntermediateSQL();
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        return _metaData.supportsBatchUpdates();
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return _metaData.supportsCatalogsInDataManipulation();
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return _metaData.supportsCatalogsInIndexDefinitions();
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions()
        throws SQLException {
        return _metaData.supportsCatalogsInPrivilegeDefinitions();
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return _metaData.supportsCatalogsInProcedureCalls();
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return _metaData.supportsCatalogsInTableDefinitions();
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        return _metaData.supportsColumnAliasing();
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        return _metaData.supportsConvert();
    }

    @Override
    public boolean supportsConvert(int fromType, int toType)
        throws SQLException {
        return _metaData.supportsConvert(fromType, toType);
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return _metaData.supportsCoreSQLGrammar();
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return _metaData.supportsCorrelatedSubqueries();
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException {
        return _metaData
            .supportsDataDefinitionAndDataManipulationTransactions();
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly()
        throws SQLException {
        return _metaData.supportsDataManipulationTransactionsOnly();
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames()
        throws SQLException {
        return _metaData.supportsDifferentTableCorrelationNames();
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return _metaData.supportsExpressionsInOrderBy();
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return _metaData.supportsExtendedSQLGrammar();
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        return _metaData.supportsFullOuterJoins();
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        return _metaData.supportsGroupBy();
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return _metaData.supportsGroupByBeyondSelect();
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        return _metaData.supportsGroupByUnrelated();
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return _metaData.supportsIntegrityEnhancementFacility();
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        return _metaData.supportsLikeEscapeClause();
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return _metaData.supportsLimitedOuterJoins();
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return _metaData.supportsMinimumSQLGrammar();
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return _metaData.supportsMixedCaseIdentifiers();
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return _metaData.supportsMixedCaseQuotedIdentifiers();
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        return _metaData.supportsMultipleResultSets();
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        return _metaData.supportsMultipleTransactions();
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        return _metaData.supportsNonNullableColumns();
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return _metaData.supportsOpenCursorsAcrossCommit();
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return _metaData.supportsOpenCursorsAcrossRollback();
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return _metaData.supportsOpenStatementsAcrossCommit();
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return _metaData.supportsOpenStatementsAcrossRollback();
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        return _metaData.supportsOrderByUnrelated();
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        return _metaData.supportsOuterJoins();
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        return _metaData.supportsPositionedDelete();
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        return _metaData.supportsPositionedUpdate();
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency)
        throws SQLException {
        return _metaData.supportsResultSetConcurrency(type, concurrency);
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        return _metaData.supportsResultSetType(type);
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return _metaData.supportsSchemasInDataManipulation();
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return _metaData.supportsSchemasInIndexDefinitions();
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return _metaData.supportsSchemasInPrivilegeDefinitions();
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return _metaData.supportsSchemasInProcedureCalls();
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return _metaData.supportsSchemasInTableDefinitions();
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        return _metaData.supportsSelectForUpdate();
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        return _metaData.supportsStoredProcedures();
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return _metaData.supportsSubqueriesInComparisons();
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        return _metaData.supportsSubqueriesInExists();
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        return _metaData.supportsSubqueriesInIns();
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return _metaData.supportsSubqueriesInQuantifieds();
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        return _metaData.supportsTableCorrelationNames();
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level)
        throws SQLException {
        return _metaData.supportsTransactionIsolationLevel(level);
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        return _metaData.supportsTransactions();
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        return _metaData.supportsUnion();
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        return _metaData.supportsUnionAll();
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        return _metaData.updatesAreDetected(type);
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        return _metaData.usesLocalFilePerTable();
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        return _metaData.usesLocalFiles();
    }

    // JDBC 3.0 methods follow.

    @Override
    public boolean supportsSavepoints() throws SQLException {
        return _metaData.supportsSavepoints();
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        return _metaData.supportsNamedParameters();
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return _metaData.supportsMultipleOpenResults();
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return _metaData.supportsGetGeneratedKeys();
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern,
        String typeNamePattern) throws SQLException {
        return _metaData.getSuperTypes(catalog, schemaPattern, typeNamePattern);
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern,
        String tableNamePattern) throws SQLException {
        return _metaData.getSuperTables(catalog, schemaPattern,
            tableNamePattern);
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern,
        String typeNamePattern, String attributeNamePattern)
        throws SQLException {
        return _metaData.getAttributes(catalog, schemaPattern, typeNamePattern,
            attributeNamePattern);
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability)
        throws SQLException {
        return _metaData.supportsResultSetHoldability(holdability);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return _metaData.getResultSetHoldability();
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return _metaData.getDatabaseMajorVersion();
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return _metaData.getDatabaseMinorVersion();
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return _metaData.getJDBCMajorVersion();
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return _metaData.getJDBCMinorVersion();
    }

    @Override
    public int getSQLStateType() throws SQLException {
        return _metaData.getSQLStateType();
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        return _metaData.locatorsUpdateCopy();
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return _metaData.supportsStatementPooling();
    }

    /**
     * Return the wrapped database metadata.
     */
    public DatabaseMetaData getDelegate() {
        return _metaData;
    }

    //  JDBC 4.0 methods follow.

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(getDelegate().getClass());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return (T) getDelegate();
        else
            return null;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return _metaData.autoCommitFailureClosesAllResultSets();
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        return _metaData.getClientInfoProperties();
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern,
            String functionNamePattern, String columnNamePattern)
            throws SQLException {
        return _metaData.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern,
            String functionNamePattern) throws SQLException {
        return _metaData.getFunctions(catalog, schemaPattern, functionNamePattern);
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return _metaData.getRowIdLifetime();
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return _metaData.getSchemas(catalog, schemaPattern);
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return _metaData.supportsStoredFunctionsUsingCallSyntax();
    }

    // Java 7 methods follow

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
    	String tableNamepattern, String columnNamePattern) throws SQLException {
    	throw new UnsupportedOperationException();
    }
}
