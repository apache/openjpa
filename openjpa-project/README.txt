                           OpenJPA 2.0.0 Early Access 2
                           ----------------------------

Content
-------
   * Overview
   * License
   * Notices
   * Prerequisites
   * Documentation
   * Getting Involved
   * Included Changes
     * Sub-tasks
     * Bugs
     * Improvements
     * New Features
     * Test


Overview
--------
   This distribution of Apache OpenJPA builds upon the prior releases which 
   provided a feature-rich implementation of the Java Persistence API (JPA 1.0)
   part of Java Community Process JSR-220 (Enterprise JavaBeans 3.0) by
   includng some Early Access functionality from the Java Community Process
   JSR-317 (JPA 2.0) public draft on 03/13/2009 and is being made available
   for testing and evaluation purposes only. 

   Some of the key features included in this distribution:
     * JPA 2.0 API and persistence and orm schemas
     * Support for nested embeddables and relationships within embeddables
     * Support for collections of embeddables and basic types
     * A programmatic query construction API based upon the 10/2008 revision of
       the JSR-317 specification
     * A standardized Level 2 cache interface
     * Enhanced map collection support
     * Support for standard javax.persistence configuration properties
     * A new prepared query cache for the caching of the SQL underlying JPQL
       and find queries
     * Support for derived identities
     * The ability to specify an order column on ordered collections
     * Significant enhancements to JPQL
     * Automatic orphan removal
     * Support for individual entity detachment, including the ability to
       cascade
     * Methods to retrieve active and all supported properties on the entity
       manager and entity manager factory
     * New lock modes, including pessimistic locking on a per entity manager
       and query method level
     * Support for query and lock timeout hints on a per entity manager and 
       query method level
     * Specification of explicit persistent access type on persistent classes
       and attributes

   This early access distribution is based upon the contributions provided in
   development iterations 1 through 7, as defined in the JPA 2.0 Roadmap at:

   The JPA 2.0 Roadmap contains a complete list of implemented features and
   feature summaries, including what is on deck for future iterations.

   Additional information on the OpenJPA project may be found at the project
   web site: http://openjpa.apache.org


License
-------
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements. See the NOTICE file distributed with this
   work for additional information regarding copyright ownership. The ASF
   licenses this file to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance with the
   License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations
   under the License.

   The license may also be found in LICENSE.txt included in each assembly.


Notices
-------
   Copyright 2006-2009 Apache Software Foundation

   This product includes software developed at
   The Apache Software Foundation (http://www.apache.org/).

   This is an implementation of an early-draft specification developed under the   Java Community Process (JCP) and is made available for testing and evaluation   purposes only. The code is not compatible with any specification of the JCP.

   The complete list of notices can be found in NOTICE.txt included in each
   assembly.


Prerequisites
-------------
   In normal usage, OpenJPA requires Java 5 or higher and a relational 
   database of some sort.


Documentation
-------------
   If you have questions about OpenJPA, a good source of information is the
   online product manual. You can find the manual for the current release as
   well as older releases of OpenJPA at
   http://openjpa.apache.org/documentation.html

   If you can't find what you're looking for in the manual or would like more
   clarification you please post to the OpenJPA development mailing list.
   Information on all of the OpenJPA mailing lists may be found here:
   http://openjpa.apache.org/mailing-lists.html


Getting Involved
----------------
   The Apache OpenJPA project is being built by the open source community for
   the open source community - we welcome your input and contributions!

   What we are looking for
        * Source code and fixes contributions
        * Documentation assistance
        * Product and feature suggestions
        * Detailed and constructive feedback
        * Articles and whitepapers

   How do I Contribute?
        * To discuss Apache OpenJPA topics check out the mailing lists.
        * Informal discussion also occurs on the #openjpa IRC channel on
       freenode.net.
        * Bugs and other issues can be posted on the project JIRA.


Included Changes
----------------

Sub-task
--------
    * [OPENJPA-722] - persist - clear - merge scenario doesn't work
    * [OPENJPA-744] - Extra SQL on LAZY/EAGER  ManyToOne relation
    * [OPENJPA-754] - Un-scheduled pre/postUpdate callbacks from persist.
    * [OPENJPA-765] - Check for insertable or updateable before checking value for null
    * [OPENJPA-769] - Add compatibility option to use previous column naming convention
    * [OPENJPA-770] - Use annotations instead of excluding tests in pom.xml
    * [OPENJPA-782] - Support for collections of embeddables and basic types
    * [OPENJPA-803] - Update SequenceGenerator to support schema and catalog
    * [OPENJPA-804] - JPA 2.0 spec API update - iteration 1
    * [OPENJPA-805] - JPQL updates - iteration 1
    * [OPENJPA-809] - Support JPA 2.0 Cache Interface
    * [OPENJPA-823] - Add JPA 2.0 schemas and appropriately validate for version
    * [OPENJPA-837] - OpenJPA 2.0: Update OpenJPA documentation with new persistence schemas
    * [OPENJPA-849] - Add metadata-type getter methods to EntityManager
    * [OPENJPA-850] - Support equivalent names for plug-in value
    * [OPENJPA-851] - Support for enhanced map collection (and corresponding annotations)
    * [OPENJPA-855] - JPA2 Query support for Index function
    * [OPENJPA-856] - JPA2 Query support for entity type expressions
    * [OPENJPA-861] - Update the manual for XML column support for MySQL
    * [OPENJPA-865] - JPA2 Query support for collection-valued input parameters in IN Expression predicate
    * [OPENJPA-869] - Support @OrderColumn annotation and XML-based definition - phase 2
    * [OPENJPA-870] - Support orphanRemoval attribute on relationships
    * [OPENJPA-871] - Support derived identity 
    * [OPENJPA-878] - Support default query hint for query timeout
    * [OPENJPA-879] - JPA2 Query support for general and qualified identification variable 
    * [OPENJPA-880] - Use maven-remote-resources-plugin to generate required legal files
    * [OPENJPA-885] - Support clear methods on EntityManager, including new CascadeType.CLEAR 
    * [OPENJPA-891] - JPA2 LockTypeMode Support
    * [OPENJPA-903] - org.apache.openjpa.persistence.exception.TestException hangs when run using PostgreSQL 8.3 database 
    * [OPENJPA-905] - org.apache.openjpa.persistence.kernel.TestProxies2
    * [OPENJPA-906] - org.apache.openjpa.persistence.jpql.expressions.TestEntityTypeExpression
    * [OPENJPA-907] - org.apache.openjpa.persistence.jdbc.update.TestParentChild
    * [OPENJPA-908] - org.apache.openjpa.persistence.annotations.TestOneToOne
    * [OPENJPA-926] - Support explicit access types including @Access annotation and AccessType enum and XML
    * [OPENJPA-930] - @AttributeOverride updates 
    * [OPENJPA-931] - Support derived identity (spec 2.4.1.2 Ex 5(a), 6 (a))
    * [OPENJPA-937] - @AssociationOverride updates 
    * [OPENJPA-946] - Oracle create table(s) exceptions
    * [OPENJPA-957] - Support lock timeout hint on applicable methods
    * [OPENJPA-960] - Support unwrap methods on EntityManager and Query interfaces
    * [OPENJPA-961] - Support projection of element collection from a JPQL query
    * [OPENJPA-963] - Add query timeout tests for PU and Map properties
    * [OPENJPA-964] - Finish updating sql-error-state- codes.xml for query timeout support on other DBs
    * [OPENJPA-967] - JPA2 Query support for selction of KEY, VALUE, ENTRY of a Map value
    * [OPENJPA-972] - Support standard provider properties in PersistenceProvider class
    * [OPENJPA-978] - Allow third argument of JPQL SUBSTRING function to be optional
    * [OPENJPA-990] - setHint should return IllegalArgumentException for invalid query/lock timeout values
    * [OPENJPA-1012] - Test failures in TestManagedInterface with @ManagedInterface annotation when using JDK6
    * [OPENJPA-1023] - Message files contain windows EOL characters
    * [OPENJPA-1024] - JPA2 Query scalar expression  in subquery 
    * [OPENJPA-1026] - Ensure newlines at end of fie
    * [OPENJPA-1027] - Document changes for detach methods
    * [OPENJPA-1032] - Revert OpenJPAQuery.getPositionalParameters method signature back to 1.x signature
    * [OPENJPA-1033] - Update supported database and driver matrix
    * [OPENJPA-1034] - Removal of OrderColumn attributes per latest spec draft
    * [OPENJPA-1035] - JPA2 Query allow map key/value path as argument of scalar functions
    * [OPENJPA-1055] - Added MapKeyEnumerated and MapKeyTemporal annotations and XML. 
    * [OPENJPA-1062] - Include OSGi bundle metadata
    * [OPENJPA-1064] - JPA2 Query comparisons over instances of embeddable class are not supported
    * [OPENJPA-1068] - Support Bean Validation: Entity validation upon lifecycle events
    * [OPENJPA-1069] - OrderBy annotation applied to an element collection of basic type doesn't require property or field name 
    * [OPENJPA-1076] - PersistenceProviderResolver interface and PersistenceProviderResolverHolder class
    * [OPENJPA-1077] - Validation-mode element support added to persistence.xml and to createEMF properties Map
    * [OPENJPA-1078] - Cache mode elements added to persistence.xml
    * [OPENJPA-1082] - Validation target groups via persistence.xml or createEMF properties Map
    * [OPENJPA-1090] - Oracle failures due to the following warning "This database dictionary "Oracle" does not support auto-assigned column values.  The column "pid" may not behave as desired."
    * [OPENJPA-1094] - JPA2 Query support for KEY appear in subquery
    * [OPENJPA-1098] - JPA2 Query support subselect_identification_variable
    * [OPENJPA-1101] - OSGi Integration tests
    * [OPENJPA-1102] - Support application/container provided ValidatorFactory
    * [OPENJPA-1103] - Remove early-access disclaimer from the NOTICE files once the spec is released
    * [OPENJPA-1106] - Integration tests for Bean Validation providers
    * [OPENJPA-1111] - Validation mode of callback should cause a PersistenceException when no provider is available
    * [OPENJPA-1113] - Reflection class performance improvement
    * [OPENJPA-1114] - Bean Validation APIs should be an optional runtime dependency
    * [OPENJPA-1115] - Finish support for delimited identifiers

Bug
---
    * [OPENJPA-207] - failure when composite ID has another composite ID as a field 
    * [OPENJPA-218] - pcNewInstance and ApplicationId
    * [OPENJPA-377] - RuntimeUnenhancedClasses support can go into a "half baked" state
    * [OPENJPA-466] - Primary key constraint violated using (Oracle) sequence to generate ID in multithreaded app
    * [OPENJPA-557] - Primary key sequences broken with postgres schemas
    * [OPENJPA-580] - Need a better algorithm to find DBDictionary classname in DBDictionaryFactory
    * [OPENJPA-732] - Updates to entities via Lifecycle callback methods 
    * [OPENJPA-751] - Typos in the manual
    * [OPENJPA-755] - OpenJPA thows EntityExistsException trying persist a preexisting, detached entity
    * [OPENJPA-761] - SchemaTool failed with a NPE in ForeignKey.join
    * [OPENJPA-762] - Batch execution fails for Oracle when batch limit set to -1 (unlimited batch size)
    * [OPENJPA-764] - Query parsing error with IN expression and String functions such as UPPER()
    * [OPENJPA-777] - Exception is thrown during retrieval of an entity which contains a persistent collection of embeddable
    * [OPENJPA-787] - slices query.getSingleResult is broken
    * [OPENJPA-792] - An entity persist may fail when @MappedSupercalss is specified. 
    * [OPENJPA-795] - enhancer throws an exception when parsing column name "first.name" because it thinks 'first' is a table name
    * [OPENJPA-811] - With Oracle, OpenJPA allows setting non-nullable field to null
    * [OPENJPA-815] - Exception is thrown when retrieving an entity which contains an embeddable and the embeddable contains a toMany relation 
    * [OPENJPA-818] - TCK module should use Geronimo JPA 2.0 EA jar
    * [OPENJPA-819] - NPE when no metadata is defined for a persistent class
    * [OPENJPA-828] - Externalizer fails with ClassCastException with runtime enhancement
    * [OPENJPA-834] - State field mapped to XML column has incorrect value when loaded from database
    * [OPENJPA-835] - Loading nested toMany EAGER relation resuled in PersistenceException
    * [OPENJPA-836] - after em.clear the datacache is inconsistent
    * [OPENJPA-838] - fix parameter setting problem when QuerySQLCache is on
    * [OPENJPA-843] - Unnecessary version update on inverse-side of a 1-m relationship
    * [OPENJPA-847] - Retrieving database generated keys gets never enabled
    * [OPENJPA-853] - Informix cursor not open problem if synchronizeMapping set true
    * [OPENJPA-863] - Unexpected mere-cascade behavior when cascade.all/merge specified on both sides of relationships !!!
    * [OPENJPA-864] - Subquery problems with SYNTAX_DATABASE (Oracle)
    * [OPENJPA-866] - DBDictionary.maxTableNameLength is not checked when using SynchronizeMappings
    * [OPENJPA-872] - Compound custom id in bidirectional many-to-one
    * [OPENJPA-873] - @MappedSuperClass Cause Null Pointer Exception in Class With IdClass
    * [OPENJPA-883] - Documentation is out of date for some MySQLDictionary properties
    * [OPENJPA-884] - Logging oversight in DB2Dictionary
    * [OPENJPA-886] - Certain query failing after svn:739123
    * [OPENJPA-887] - Assertion oversight in TestLibService
    * [OPENJPA-890] - Typos and inconsistent method signature styles in the user manual
    * [OPENJPA-892] - Incorrect package name of javax.jdo for Query class image
    * [OPENJPA-896] - Several source files include Windows EoL chars
    * [OPENJPA-898] - hints don't work for NamedNativeQuery
    * [OPENJPA-912] - Potential NPE in setInverseRelation
    * [OPENJPA-913] - A deadlock issue happens when DirtyListener is used
    * [OPENJPA-916] - DistributedTemplate is incorrectly setting some attributes on the statements
    * [OPENJPA-917] - stored procedures throw InvalidStateException when using getSingleResult() or getResultList()
    * [OPENJPA-919] - JUnit for Bi-directional OneToOne with null relationships
    * [OPENJPA-922] - setByteArrayInputStream being used in stead of setBytes
    * [OPENJPA-923] - Update documentation for properly configuring Oracle, PostgreSQL, and DB2
    * [OPENJPA-925] - Bidirectional OneToOne relation incorrectly set in loadEagerJoin
    * [OPENJPA-927] - Fix definition of javax.persistence.query.timeout property
    * [OPENJPA-928] - getSupportedProperties() shows wsjpa property by default
    * [OPENJPA-933] - Database version detection in MySQLDictionary is not reliable
    * [OPENJPA-947] - Overly verbose TestCases
    * [OPENJPA-951] - Javadoc jar file does not contain legal files
    * [OPENJPA-954] - openjpa-slice build fails due to tests relying on openjpa-persistence-jdbc/src/test/java/org/apache/openjpa/persistence/test/AllowFailure.java
    * [OPENJPA-955] - MethodQL parameter passing broken
    * [OPENJPA-965] - Open up FinderCacheImpl for non-JDBC or JDBC-like  implementation of preparing statement/query execution
    * [OPENJPA-970] - SchemaToolTask does not have "dropTables" argument
    * [OPENJPA-973] - Allow DB2 JCC driver to work with Informix database
    * [OPENJPA-974] - Docs:  Add new Exception types
    * [OPENJPA-992] - Failed to throw EntityExistException on duplicated persist in DB2
    * [OPENJPA-999] - Missing sql-warning in the localizer.properties
    * [OPENJPA-1002] - Select range doesn't work on Oracle JDBC driver
    * [OPENJPA-1004] - Derived Identity fails when parent id is auto-generated
    * [OPENJPA-1006] - Disabling QueryCaching at runtime does not work
    * [OPENJPA-1028] - ClassCastException during findBy when embeddable is involved
    * [OPENJPA-1029] - SQLServerDictionary causes NumberFormatException if MS SQL Server JDBC driver is used
    * [OPENJPA-1031] - Update docs that refer to OpenJPAEntityManager.getExtent(..)
    * [OPENJPA-1041] - OrderBy on nested embeddables is not working
    * [OPENJPA-1046] - Unique Constraint on MappedSupperClass causes NullPointerException
    * [OPENJPA-1047] - Unique Constraint on sibling classes causes name conflict
    * [OPENJPA-1049] - Query against a MappedSuperclass is not supported
    * [OPENJPA-1051] - [patch] Mappingtool doesn't check name conflicts if MappingDefaultsImpl is called with multiple columns.
    * [OPENJPA-1053] - Updating an entity by setting an embeddable to it does not work properly if the embeddable has a cascade delete relationship with another entity
    * [OPENJPA-1054] - Large result sets do not work with MySQL
    * [OPENJPA-1057] - Foreign keys are not properly set for ConstraintUpdateManager to determine the correct order of the sql
    * [OPENJPA-1058] - Duplicate rows in DB when UniqueConstraint set
    * [OPENJPA-1060] - Attempting to returning a list over RMI/IIOP results in serialization exception
    * [OPENJPA-1067] - SetQueryTimeout(x) where x != 0 causes SQLException with DB2 on Z/OS
    * [OPENJPA-1072] - Nested embeddable with a relationship to an Entity with a generated id doesn't persist the relationship
    * [OPENJPA-1088] - Build updates for openjpa-examples and openjpa-integration/examples
    * [OPENJPA-1091] - ReverseMappingTool fails for openjpa-examples/reversemapping sample
    * [OPENJPA-1099] - <xmp> tag in Javadoc comments causes Javadoc corruption

** Improvement
    * [OPENJPA-213] - @Column with precision and scale should result in NUMERIC(precision, scale)
    * [OPENJPA-736] - Combine insert and select SQL together for generated Id strategy=GenerationType.IDENTITY 
    * [OPENJPA-742] - Add line number and column number to QueryMetaData
    * [OPENJPA-752] - ProxySetupStateManager.setProxyData routing through PersistanceCapable caused "PersistenceException: null"
    * [OPENJPA-772] - Proper maven pluginManagement and use ianal-maven-plugin for enforcing legal files
    * [OPENJPA-775] - Some Firedird setup tricks
    * [OPENJPA-776] - Firebird 2 dictionary which supports sequences
    * [OPENJPA-778] - cleaning up build for openjpa-kernel
    * [OPENJPA-779] - patch for eclipse .project and .classpath files...
    * [OPENJPA-780] - code review for DistributedStoreManager
    * [OPENJPA-781] - openjpa-jdbc depends on postgres driver, should be "provided"
    * [OPENJPA-783] - openjpa-lib/pom.xml has extraneous code
    * [OPENJPA-784] - more pom.xml dependency cleanup
    * [OPENJPA-817] - Order of inserts lost when using ConstraintUpdateManager
    * [OPENJPA-854] - Testcases should not specify log level
    * [OPENJPA-858] - Allow postPersist be invoked immediately after persist()
    * [OPENJPA-876] - Better test profiles for proprietary databases (DB2, Oracle) and continuous build
    * [OPENJPA-881] - Enable connection pooling for testcases. 
    * [OPENJPA-882] - Upgrade to latest Geronimo Spec releases
    * [OPENJPA-901] - Use hosted JAI artifacts as the default for the docbook build
    * [OPENJPA-910] - Allow multiple keys for the same property to be specified at different levels
    * [OPENJPA-949] - Allow override of Surefire test excludes from cmdline
    * [OPENJPA-952] - Utilize Sun JDK's Attach API to dynamically load the OpenJPA enhancer agent
    * [OPENJPA-968] - Change in default detach() behavior for JPA 2.0
    * [OPENJPA-975] - Oracle needs ability to not have an escape character for search strings.
    * [OPENJPA-983] - FirebirdDictionary improvements
    * [OPENJPA-988] - Refactor JPA2/MixedLockManager to conform architectual module dependency
    * [OPENJPA-1000] - Consistent usage of serialVersionUID
    * [OPENJPA-1022] - Support distinct LockModeType.READ/OPTIMISTIC & WRITE/OPTIMISTIC_FORCE_INCREMENT
    * [OPENJPA-1038] - Enhancer java.lang.StackOverflowError exception when circular dependencies are encountered within embeddables
    * [OPENJPA-1045] - Add opt-in and opt-out configuration for L2 DataCache
    * [OPENJPA-1056] - Add support for Sybase in the query timeout tests
    * [OPENJPA-1063] - Create a new openjpa-all artifact to include runtime depends
    * [OPENJPA-1066] - Generated ID starting with 0 can cause unexpected results

New Feature
-----------
    * [OPENJPA-723] - Feature request for PostgreSQL XML Column Mapping
    * [OPENJPA-767] - Better OSGi Integration
    * [OPENJPA-773] - Upgrade to JPA 2
    * [OPENJPA-800] - OpenJPA 2.0 iteration 1 primary task 
    * [OPENJPA-807] - OpenJPA 2.0 iteration 2 primary task
    * [OPENJPA-808] - OpenJPA 2.0 iteration 3 primary task
    * [OPENJPA-831] - OpenJPA 2.0 iteration 1.5 (holiday) primary task
    * [OPENJPA-846] - XML column support for MySQL
    * [OPENJPA-875] - OpenJPA 2.0 iteration 4 primary task
    * [OPENJPA-918] - Stored procedures not handling returns properly
    * [OPENJPA-956] - OpenJPA 2.0 iteration 5 primary task
    * [OPENJPA-966] - Support Replication-enabled MySQL  
    * [OPENJPA-1005] - Add PersistenceXMLSchemaVersion(); support
    * [OPENJPA-1007] - OpenJPA 2.0 iteration 6 primary task

Test
----
    * [OPENJPA-247] - new-delete-new-find doesn't work
    * [OPENJPA-833] - An improved test case for XML column mapping
    * [OPENJPA-998] - Add @Ignore annotation support for testcases
    * [OPENJPA-1015] - Enforce 80-column line width for source code
    * [OPENJPA-1071] - Derby test suit speed-up
    * [OPENJPA-1073] - Upgrade to latest maven-surefire-plugin
    * [OPENJPA-1092] - enable test case for jpql


Included Changes in prior Milestone 1
-------------------------------------

Sub-task
--------
    * [OPENJPA-765] - Check for insertable or updateable before checking value for null
    * [OPENJPA-857] - Detect and store JPA version
    * [OPENJPA-899] - Add support for JPA2.0 method Query.getHints()
    * [OPENJPA-900] - Add support for JPA2.0 method Query.getSupportedHints()
    * [OPENJPA-958] - Support lock mode on Named Query
    * [OPENJPA-1013] - Build strictly-typed Criteria API

Bug
---
    * [OPENJPA-828] - Externalizer fails with ClassCastException with runtime enhancement
    * [OPENJPA-886] - Certain query failing after svn:739123
    * [OPENJPA-955] - MethodQL parameter passing broken
    * [OPENJPA-1039] - Dynamic query predicates must not treat AND OR operators as associative

Improvement
-----------
    * [OPENJPA-703] - Cache ResultObjectProvider data to improve query performance
    * [OPENJPA-858] - Allow postPersist be invoked immediately after persist()
    * [OPENJPA-968] - Change in default detach() behavior for JPA 2.0

New Feature
-----------
    * [OPENJPA-800] - OpenJPA 2.0 iteration 1 primary task 
    * [OPENJPA-807] - OpenJPA 2.0 iteration 2 primary task
    * [OPENJPA-831] - OpenJPA 2.0 iteration 1.5 (holiday) primary task
    * [OPENJPA-966] - Support Replication-enabled MySQL  
    * [OPENJPA-1008] - Generate meta-model for JPA 2.0
    * [OPENJPA-1009] - Populate canonical meta-model for strictly typed Criteria Query building
    * [OPENJPA-1010] - Instantiate meta-model classes for JPA 2.0 from source code annotations
    * [OPENJPA-1011] - Instantiate meta-model classes for JPA 2.0 from XML descriptors
    * [OPENJPA-1014] - Build weakly-typed Criteria API

Task
----
    * [OPENJPA-995] - Migrate existing Criteria Query implementation as OpenJPA extension to prepare for upcoming JPA 2.0 spec changes

