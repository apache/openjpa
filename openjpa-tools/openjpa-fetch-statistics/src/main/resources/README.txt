Apache OpenJPA - README.txt
Licensed under Apache License 2.0 - http://www.apache.org/licenses/LICENSE-2.0
--------------------------------------------------------------------------------

Open JPA Fetching Statistics Tool monitors the persistent fields 
fetching and finds out the fields which are never accessed. Based on the 
statistic data, user can set the field access type to LAZY to improve the 
performance by eliminating the data loading and processing time.

Note: Open JPA Fetching Statistics Tool works with the runtime enhancement.

Usage instructions: 

1 Classpath Configuration
    
    Append the path of  openjpa-fetch-statistics-version-SNAPSHOT.jar file 
    to the classpath of the application before starting the application.
    
2 Statistics Collecting and Monitoring     
    
    Start the application with openjpa-fetch-statistics-<version>-SNAPSHOT.jar
    in the classpath. The tool will start collecting the statistics of the 
    persistent field fetching and output the list of the persistent fields 
    which have never been fetched to the log file every ten minutes.It will 
    output the last list when the JVM is shutdown.
    
3 Termination
    Shut down the JVM to stop the fetching statistics collecting. 
    Roll back the classpath configuration.
    Start the application without the fetching statistics jar in the classpath
    
Performance Consideration

There will be performance impact when the fetching statistics collection is on. 
The recommendation is to use it in the testing environment.

Apache OpenJPA - README.txt
Licensed under Apache License 2.0 - http://www.apache.org/licenses/LICENSE-2.0
--------------------------------------------------------------------------------

Open JPA Fetch Statistics Tool monitors persistent field access and determines which fields are never used. This tool
can be used to help tune an application. 

Note: Open JPA Fetching Statistics Tool works with the runtime enhancement.

Usage instructions: 

1.] Configuration
  * Append the path of openjpa-fetch-statistics-version-SNAPSHOT.jar file to the classpath prior to lanuching the JVM.
    
2.] Statistics Collecting and Monitoring     
  * When this tool is configured, it will be active for all persistence units in the JVM. Statistics will be dumped via the 
  openjpa.Runtime channel with the INFO level every 10 minutes, or when the JVM terminates. Any field that is logged 
  has not been accessed by an application.
    
3.] Configuration removal
  * Stop the JVM.
  * Remove openjpa-fetch-statistics-version-SNAPSHOT.jar from the classpath.
    
Performance Consideration

There will be a large performance impact when running this tooling. It is not supported, nor recommended for production
use. This tool should not be used on a production machine.

