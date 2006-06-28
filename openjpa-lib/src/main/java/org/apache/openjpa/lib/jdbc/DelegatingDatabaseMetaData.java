/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.openjpa.lib.jdbc;

import java.io.*;

import java.sql.*;

import java.util.*;


/**
 *  <p>Wrapper around a DatabaseMetadata instance.</p>
 *
 *  @author Marc Prud'hommeaux
 */
public class DelegatingDatabaseMetaData implements DatabaseMetaData {
    private final DatabaseMetaData _metaData;
    private final Connection _conn;

    public DelegatingDatabaseMetaData(DatabaseMetaData metaData, Connection conn) {
        _conn = conn;
        _metaData = metaData;
    }

    /**
     *  Return the base underlying database metadata.
     */
    public DatabaseMetaData getInnermostDelegate() {
        return (_metaData instanceof DelegatingDatabaseMetaData)
        ? ((DelegatingDatabaseMetaData) _metaData).getInnermostDelegate()
        : _metaData;
    }

    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof DelegatingDatabaseMetaData) {
            other = ((DelegatingDatabaseMetaData) other).getInnermostDelegate();
        }

        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("metadata ").append(hashCode());
        buf.append("[").append(_metaData.toString()).append("]");

        return buf.toString();
    }

    public boolean allProceduresAreCallable() throws SQLException {
        return _metaData.allProceduresAreCallable();
    }

    public boolean allTablesAreSelectable() throws SQLException {
        return _metaData.allTablesAreSelectable();
    }

    public boolean dataDefinitionCausesTransactionCommit()
        throws SQLException {
        return _metaData.dataDefinitionCausesTransactionCommit();
    }

    public boolean dataDefinitionIgnoredInTransactions()
        throws SQLException {
        return _metaData.dataDefinitionIgnoredInTransactions();
    }

    public boolean deletesAreDetected(int type) throws SQLException {
        return _metaData.deletesAreDetected(type);
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return _metaData.doesMaxRowSizeIncludeBlobs();
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema,
        String table, int scope, boolean nullable) throws SQLException {
        return _metaData.getBestRowIdentifier(catalog, schema, table, scope,
            nullable);
    }

    public ResultSet getCatalogs() throws SQLException {
        return _metaData.getCatalogs();
    }

    public String getCatalogSeparator() throws SQLException {
        return _metaData.getCatalogSeparator();
    }

    public String getCatalogTerm() throws SQLException {
        return _metaData.getCatalogTerm();
    }

    public ResultSet getColumnPrivileges(String catalog, String schema,
        String table, String columnNamePattern) throws SQLException {
        return _metaData.getColumnPrivileges(catalog, schema, table,
            columnNamePattern);
    }

    public ResultSet getColumns(String catalog, String schemaPattern,
        String tableNamePattern, String columnNamePattern)
        throws SQLException {
        return _metaData.getColumns(catalog, schemaPattern, tableNamePattern,
            columnNamePattern);
    }

    public Connection getConnection() throws SQLException {
        return _conn;
    }

    public ResultSet getCrossReference(String primaryCatalog,
        String primarySchema, String primaryTable, String foreignCatalog,
        String foreignSchema, String foreignTable) throws SQLException {
        return _metaData.getCrossReference(primaryCatalog, primarySchema,
            primaryTable, foreignCatalog, foreignSchema, foreignTable);
    }

    public String getDatabaseProductName() throws SQLException {
        return _metaData.getDatabaseProductName();
    }

    public String getDatabaseProductVersion() throws SQLException {
        return _metaData.getDatabaseProductVersion();
    }

    public int getDefaultTransactionIsolation() throws SQLException {
        return _metaData.getDefaultTransactionIsolation();
    }

    public int getDriverMajorVersion() {
        return _metaData.getDriverMajorVersion();
    }

    public int getDriverMinorVersion() {
        return _metaData.getDriverMinorVersion();
    }

    public String getDriverName() throws SQLException {
        return _metaData.getDriverName();
    }

    public String getDriverVersion() throws SQLException {
        return _metaData.getDriverVersion();
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table)
        throws SQLException {
        return _metaData.getExportedKeys(catalog, schema, table);
    }

    public String getExtraNameCharacters() throws SQLException {
        return _metaData.getExtraNameCharacters();
    }

    public String getIdentifierQuoteString() throws SQLException {
        return _metaData.getIdentifierQuoteString();
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table)
        throws SQLException {
        return _metaData.getImportedKeys(catalog, schema, table);
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table,
        boolean unique, boolean approximate) throws SQLException {
        return _metaData.getIndexInfo(catalog, schema, table, unique,
            approximate);
    }

    public int getMaxBinaryLiteralLength() throws SQLException {
        return _metaData.getMaxBinaryLiteralLength();
    }

    public int getMaxCatalogNameLength() throws SQLException {
        return _metaData.getMaxCatalogNameLength();
    }

    public int getMaxCharLiteralLength() throws SQLException {
        return _metaData.getMaxCharLiteralLength();
    }

    public int getMaxColumnNameLength() throws SQLException {
        return _metaData.getMaxColumnNameLength();
    }

    public int getMaxColumnsInGroupBy() throws SQLException {
        return _metaData.getMaxColumnsInGroupBy();
    }

    public int getMaxColumnsInIndex() throws SQLException {
        return _metaData.getMaxColumnsInIndex();
    }

    public int getMaxColumnsInOrderBy() throws SQLException {
        return _metaData.getMaxColumnsInOrderBy();
    }

    public int getMaxColumnsInSelect() throws SQLException {
        return _metaData.getMaxColumnsInSelect();
    }

    public int getMaxColumnsInTable() throws SQLException {
        return _metaData.getMaxColumnsInTable();
    }

    public int getMaxConnections() throws SQLException {
        return _metaData.getMaxConnections();
    }

    public int getMaxCursorNameLength() throws SQLException {
        return _metaData.getMaxCursorNameLength();
    }

    public int getMaxIndexLength() throws SQLException {
        return _metaData.getMaxIndexLength();
    }

    public int getMaxProcedureNameLength() throws SQLException {
        return _metaData.getMaxProcedureNameLength();
    }

    public int getMaxRowSize() throws SQLException {
        return _metaData.getMaxRowSize();
    }

    public int getMaxSchemaNameLength() throws SQLException {
        return _metaData.getMaxSchemaNameLength();
    }

    public int getMaxStatementLength() throws SQLException {
        return _metaData.getMaxStatementLength();
    }

    public int getMaxStatements() throws SQLException {
        return _metaData.getMaxStatements();
    }

    public int getMaxTableNameLength() throws SQLException {
        return _metaData.getMaxTableNameLength();
    }

    public int getMaxTablesInSelect() throws SQLException {
        return _metaData.getMaxTablesInSelect();
    }

    public int getMaxUserNameLength() throws SQLException {
        return _metaData.getMaxUserNameLength();
    }

    public String getNumericFunctions() throws SQLException {
        return _metaData.getNumericFunctions();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
        throws SQLException {
        return _metaData.getPrimaryKeys(catalog, schema, table);
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
        String procedureNamePattern, String columnNamePattern)
        throws SQLException {
        return _metaData.getProcedureColumns(catalog, schemaPattern,
            procedureNamePattern, columnNamePattern);
    }

    public ResultSet getProcedures(String catalog, String schemaPattern,
        String procedureNamePattern) throws SQLException {
        return _metaData.getProcedures(catalog, schemaPattern,
            procedureNamePattern);
    }

    public String getProcedureTerm() throws SQLException {
        return _metaData.getProcedureTerm();
    }

    public ResultSet getSchemas() throws SQLException {
        return _metaData.getSchemas();
    }

    public String getSchemaTerm() throws SQLException {
        return _metaData.getSchemaTerm();
    }

    public String getSearchStringEscape() throws SQLException {
        return _metaData.getSearchStringEscape();
    }

    public String getSQLKeywords() throws SQLException {
        return _metaData.getSQLKeywords();
    }

    public String getStringFunctions() throws SQLException {
        return _metaData.getStringFunctions();
    }

    public String getSystemFunctions() throws SQLException {
        return _metaData.getSystemFunctions();
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern,
        String tableNamePattern) throws SQLException {
        return _metaData.getTablePrivileges(catalog, schemaPattern,
            tableNamePattern);
    }

    public ResultSet getTables(String catalog, String schemaPattern,
        String tableNamePattern, String[] types) throws SQLException {
        return _metaData.getTables(catalog, schemaPattern, tableNamePattern,
            types);
    }

    public ResultSet getTableTypes() throws SQLException {
        return _metaData.getTableTypes();
    }

    public String getTimeDateFunctions() throws SQLException {
        return _metaData.getTimeDateFunctions();
    }

    public ResultSet getTypeInfo() throws SQLException {
        return _metaData.getTypeInfo();
    }

    public ResultSet getUDTs(String catalog, String schemaPattern,
        String typeNamePattern, int[] types) throws SQLException {
        return _metaData.getUDTs(catalog, schemaPattern, typeNamePattern, types);
    }

    public String getURL() throws SQLException {
        return _metaData.getURL();
    }

    public String getUserName() throws SQLException {
        return _metaData.getUserName();
    }

    public ResultSet getVersionColumns(String catalog, String schema,
        String table) throws SQLException {
        return _metaData.getVersionColumns(catalog, schema, table);
    }

    public boolean insertsAreDetected(int type) throws SQLException {
        return _metaData.insertsAreDetected(type);
    }

    public boolean isCatalogAtStart() throws SQLException {
        return _metaData.isCatalogAtStart();
    }

    public boolean isReadOnly() throws SQLException {
        return _metaData.isReadOnly();
    }

    public boolean nullPlusNonNullIsNull() throws SQLException {
        return _metaData.nullPlusNonNullIsNull();
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        return _metaData.nullsAreSortedAtEnd();
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        return _metaData.nullsAreSortedAtStart();
    }

    public boolean nullsAreSortedHigh() throws SQLException {
        return _metaData.nullsAreSortedHigh();
    }

    public boolean nullsAreSortedLow() throws SQLException {
        return _metaData.nullsAreSortedLow();
    }

    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return _metaData.othersDeletesAreVisible(type);
    }

    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return _metaData.othersInsertsAreVisible(type);
    }

    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return _metaData.othersUpdatesAreVisible(type);
    }

    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return _metaData.ownDeletesAreVisible(type);
    }

    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return _metaData.ownInsertsAreVisible(type);
    }

    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return _metaData.ownUpdatesAreVisible(type);
    }

    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return _metaData.storesLowerCaseIdentifiers();
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesLowerCaseQuotedIdentifiers();
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return _metaData.storesMixedCaseIdentifiers();
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesMixedCaseQuotedIdentifiers();
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return _metaData.storesUpperCaseIdentifiers();
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return _metaData.storesUpperCaseQuotedIdentifiers();
    }

    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return _metaData.supportsAlterTableWithAddColumn();
    }

    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return _metaData.supportsAlterTableWithDropColumn();
    }

    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return _metaData.supportsANSI92EntryLevelSQL();
    }

    public boolean supportsANSI92FullSQL() throws SQLException {
        return _metaData.supportsANSI92FullSQL();
    }

    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return _metaData.supportsANSI92IntermediateSQL();
    }

    public boolean supportsBatchUpdates() throws SQLException {
        return _metaData.supportsBatchUpdates();
    }

    public boolean supportsCatalogsInDataManipulation()
        throws SQLException {
        return _metaData.supportsCatalogsInDataManipulation();
    }

    public boolean supportsCatalogsInIndexDefinitions()
        throws SQLException {
        return _metaData.supportsCatalogsInIndexDefinitions();
    }

    public boolean supportsCatalogsInPrivilegeDefinitions()
        throws SQLException {
        return _metaData.supportsCatalogsInPrivilegeDefinitions();
    }

    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return _metaData.supportsCatalogsInProcedureCalls();
    }

    public boolean supportsCatalogsInTableDefinitions()
        throws SQLException {
        return _metaData.supportsCatalogsInTableDefinitions();
    }

    public boolean supportsColumnAliasing() throws SQLException {
        return _metaData.supportsColumnAliasing();
    }

    public boolean supportsConvert() throws SQLException {
        return _metaData.supportsConvert();
    }

    public boolean supportsConvert(int fromType, int toType)
        throws SQLException {
        return _metaData.supportsConvert(fromType, toType);
    }

    public boolean supportsCoreSQLGrammar() throws SQLException {
        return _metaData.supportsCoreSQLGrammar();
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return _metaData.supportsCorrelatedSubqueries();
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException {
        return _metaData.supportsDataDefinitionAndDataManipulationTransactions();
    }

    public boolean supportsDataManipulationTransactionsOnly()
        throws SQLException {
        return _metaData.supportsDataManipulationTransactionsOnly();
    }

    public boolean supportsDifferentTableCorrelationNames()
        throws SQLException {
        return _metaData.supportsDifferentTableCorrelationNames();
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return _metaData.supportsExpressionsInOrderBy();
    }

    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return _metaData.supportsExtendedSQLGrammar();
    }

    public boolean supportsFullOuterJoins() throws SQLException {
        return _metaData.supportsFullOuterJoins();
    }

    public boolean supportsGroupBy() throws SQLException {
        return _metaData.supportsGroupBy();
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return _metaData.supportsGroupByBeyondSelect();
    }

    public boolean supportsGroupByUnrelated() throws SQLException {
        return _metaData.supportsGroupByUnrelated();
    }

    public boolean supportsIntegrityEnhancementFacility()
        throws SQLException {
        return _metaData.supportsIntegrityEnhancementFacility();
    }

    public boolean supportsLikeEscapeClause() throws SQLException {
        return _metaData.supportsLikeEscapeClause();
    }

    public boolean supportsLimitedOuterJoins() throws SQLException {
        return _metaData.supportsLimitedOuterJoins();
    }

    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return _metaData.supportsMinimumSQLGrammar();
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return _metaData.supportsMixedCaseIdentifiers();
    }

    public boolean supportsMixedCaseQuotedIdentifiers()
        throws SQLException {
        return _metaData.supportsMixedCaseQuotedIdentifiers();
    }

    public boolean supportsMultipleResultSets() throws SQLException {
        return _metaData.supportsMultipleResultSets();
    }

    public boolean supportsMultipleTransactions() throws SQLException {
        return _metaData.supportsMultipleTransactions();
    }

    public boolean supportsNonNullableColumns() throws SQLException {
        return _metaData.supportsNonNullableColumns();
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return _metaData.supportsOpenCursorsAcrossCommit();
    }

    public boolean supportsOpenCursorsAcrossRollback()
        throws SQLException {
        return _metaData.supportsOpenCursorsAcrossRollback();
    }

    public boolean supportsOpenStatementsAcrossCommit()
        throws SQLException {
        return _metaData.supportsOpenStatementsAcrossCommit();
    }

    public boolean supportsOpenStatementsAcrossRollback()
        throws SQLException {
        return _metaData.supportsOpenStatementsAcrossRollback();
    }

    public boolean supportsOrderByUnrelated() throws SQLException {
        return _metaData.supportsOrderByUnrelated();
    }

    public boolean supportsOuterJoins() throws SQLException {
        return _metaData.supportsOuterJoins();
    }

    public boolean supportsPositionedDelete() throws SQLException {
        return _metaData.supportsPositionedDelete();
    }

    public boolean supportsPositionedUpdate() throws SQLException {
        return _metaData.supportsPositionedUpdate();
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency)
        throws SQLException {
        return _metaData.supportsResultSetConcurrency(type, concurrency);
    }

    public boolean supportsResultSetType(int type) throws SQLException {
        return _metaData.supportsResultSetType(type);
    }

    public boolean supportsSchemasInDataManipulation()
        throws SQLException {
        return _metaData.supportsSchemasInDataManipulation();
    }

    public boolean supportsSchemasInIndexDefinitions()
        throws SQLException {
        return _metaData.supportsSchemasInIndexDefinitions();
    }

    public boolean supportsSchemasInPrivilegeDefinitions()
        throws SQLException {
        return _metaData.supportsSchemasInPrivilegeDefinitions();
    }

    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return _metaData.supportsSchemasInProcedureCalls();
    }

    public boolean supportsSchemasInTableDefinitions()
        throws SQLException {
        return _metaData.supportsSchemasInTableDefinitions();
    }

    public boolean supportsSelectForUpdate() throws SQLException {
        return _metaData.supportsSelectForUpdate();
    }

    public boolean supportsStoredProcedures() throws SQLException {
        return _metaData.supportsStoredProcedures();
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return _metaData.supportsSubqueriesInComparisons();
    }

    public boolean supportsSubqueriesInExists() throws SQLException {
        return _metaData.supportsSubqueriesInExists();
    }

    public boolean supportsSubqueriesInIns() throws SQLException {
        return _metaData.supportsSubqueriesInIns();
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return _metaData.supportsSubqueriesInQuantifieds();
    }

    public boolean supportsTableCorrelationNames() throws SQLException {
        return _metaData.supportsTableCorrelationNames();
    }

    public boolean supportsTransactionIsolationLevel(int level)
        throws SQLException {
        return _metaData.supportsTransactionIsolationLevel(level);
    }

    public boolean supportsTransactions() throws SQLException {
        return _metaData.supportsTransactions();
    }

    public boolean supportsUnion() throws SQLException {
        return _metaData.supportsUnion();
    }

    public boolean supportsUnionAll() throws SQLException {
        return _metaData.supportsUnionAll();
    }

    public boolean updatesAreDetected(int type) throws SQLException {
        return _metaData.updatesAreDetected(type);
    }

    public boolean usesLocalFilePerTable() throws SQLException {
        return _metaData.usesLocalFilePerTable();
    }

    public boolean usesLocalFiles() throws SQLException {
        return _metaData.usesLocalFiles();
    }

    // JDBC 3.0 methods (unsupported) follow; these are required to be able to 
    // compile against JDK 1.4
    public boolean supportsSavepoints() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsNamedParameters() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsMultipleOpenResults() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsGetGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSuperTypes(String catalog, String schemaPatter,
        String typeNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getSuperTables(String catalog, String schemaPatter,
        String tableNamePattern) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getAttributes(String catalog, String schemaPatter,
        String typeNamePattern, String attributeNamePattern)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsResultSetHoldability(int holdability)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDatabaseMajorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getDatabaseMinorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getJDBCMajorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getJDBCMinorVersion() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getSQLStateType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean locatorsUpdateCopy() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsStatementPooling() throws SQLException {
        throw new UnsupportedOperationException();
    }
}
