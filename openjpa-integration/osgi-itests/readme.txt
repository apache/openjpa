Setting up Apache Felix Karaf for OpenJPA 2.0.x

Installing and running Apache Felix Karaf
---------------------------------------------------
1) Download and extract Apache Felix Karaf 1.0.0 from:

   http://www.apache.org/dist/felix/apache-felix-karaf-1.0.0.zip
   http://www.apache.org/dist/felix/apache-felix-karaf-1.0.0.tar.gz

2) Start Karaf under Java SE 5 or 6:
 
  cd apache-felix-karaf-1.0.0/bin
  karaf


Installing and running the Felix Web Console and Jetty:
-------------------------------------------------------
1) Install the following bundles:

   a) osgi:install http://www.apache.org/dist/felix/org.osgi.compendium-1.4.0.jar
   b) osgi:install http://www.apache.org/dist/felix/org.apache.felix.scr-1.0.8.jar
   c) osgi:install http://www.apache.org/dist/felix/org.apache.felix.http.jetty-2.0.2.jar
   d) osgi:install http://www.apache.org/dist/felix/org.apache.felix.webconsole-2.0.0.jar

2) Start the web console:

   a) osgi:start <bundle id for org.apache.felix.http.jetty>
   b) osgi:start <bundle id for org.apache.felix.webconsole>

   Note:  You will have to start the Config, Event and Scr bundles for those
     tabs to function in the web console.

3) Access the web console in a browser:

   http://localhost:8080/system/console
   uid = admin
   pwd = admin


Installing OpenJPA 2.0.x
--------------------------------------------
1) Install the following prereq bundles:

   a) osgi:install http://repo1.maven.org/maven2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar
   b) osgi:install http://repo1.maven.org/maven2/commons-lang/commons-lang/2.4/commons-lang-2.4.jar
   c) osgi:install http://repo1.maven.org/maven2/commons-pool/commons-pool/1.5/commons-pool-1.5.jar
   d) osgi:install http://repo1.maven.org/maven2/org/apache/geronimo/specs/geronimo-jms_1.1_spec/1.1.1/geronimo-jms_1.1_spec-1.1.1.jar
   e) osgi:install http://repo1.maven.org/maven2/org/apache/geronimo/specs/geronimo-jta_1.1_spec/1.1.1/geronimo-jta_1.1_spec-1.1.1.jar
   f) osgi:install http://mirrors.ibiblio.org/pub/mirrors/maven2/org/apache/geronimo/specs/geronimo-jpa_2.0_spec/1.0-PFD2/geronimo-jpa_2.0_spec-1.0-PFD2.jar
   g) osgi:install http://mirrors.ibiblio.org/pub/mirrors/maven2/org/apache/geronimo/specs/geronimo-validation_1.0_spec/1.0-CR5/geronimo-validation_1.0_spec-1.0-CR5.jar

2) Install OpenJPA 2.0.x:

  For OpenJPA 2.0.0-M3:
  a) osgi:install http://people.apache.org/~dwoods/openjpa/2.0.0-M3/staging-repo/org/apache/openjpa/openjpa/2.0.0-M3/openjpa-2.0.0-M3.jar

  For a locally built OpenJPA trunk (2.0.0-SNAPSHOT):
  a) osgi:install file:///<m2_repo>/org/apache/openjpa/openjpa/2.0.0-SNAPSHOT/openjpa-2.0.0-SNAPSHOT.jar

3) Check the Karaf logfile for any problems:

   apache-felix-karaf-1.0.0/data/log/karaf.log 


Installing an OSGi and/or Blueprint based sample app:
-----------------------------------------------------
This step is still a work-in-progress and TBD...

1) Build the openjpa-integration/osgi-itests module in trunk (2.0.0-SNAPSHOT)
from source.

2) Install the bundlized HelloJPA example:

   a) osgi:install file:///<m2_repo>/org/apache/openjpa/openjpa-integration-osgi-itests/2.0.0-SNAPSHOT/openjpa-integration-osgi-itests-2.0.0-SNAPSHOT.jar
   b) osgi:start <bundle id for openjpa-integration-osgi-itests>

Note:  At this point the start should fail with a message like:
    Bundle start
    org.osgi.framework.BundleException: Activator start error in bundle
    org.apache.openjpa.openjpa-integration-osgi-itests [45].
    . . .
    Caused by: javax.persistence.PersistenceException: No persistence providers
    available for "hellojpa" after trying the following discovered
    implementations: NONE
	at javax.persistence.Persistence.createEntityManagerFactory(Persistence.java:189)
	at hellojpa.Main.main(Main.java:38)
	at hellojpa.Main.start(Main.java:81)
	at org.apache.felix.framework.util.SecureAction.startActivator(SecureAction.java:667)
	at org.apache.felix.framework.Felix.activateBundle(Felix.java:1699)
	... 15 more


2) To reinstall the sample app, you'll need to stop and uninstall it first, even if the initial install failed:

   a) osgi:stop <bundle id for openjpa-integration-osgi-itests>
   b) osgi:uninstall <bundle id for openjpa-integration-osgi-itests>


Shutting down Karaf:
---------------------
1) From the Karaf cmdline:

   osgi:shutdown

