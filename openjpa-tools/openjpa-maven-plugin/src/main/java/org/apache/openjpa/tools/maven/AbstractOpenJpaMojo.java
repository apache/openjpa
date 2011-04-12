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
package org.apache.openjpa.tools.maven;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.openjpa.lib.util.Options;
import org.codehaus.plexus.util.FileUtils;

/**
 * Base class for  OpenJPA maven tasks.
 * 
 * @version $Id$
 */
public abstract class AbstractOpenJpaMojo extends AbstractMojo 
{

    /**
     * Location where <code>persistence-enabled</code> classes are located.
     * 
     * @parameter expression="${openjpa.classes}"
     *            default-value="${project.build.outputDirectory}"
     * @required
     */
    protected File classes;
    
    /**
     * Comma separated list of includes to scan searchDir to pass to the jobs.
     * This may be used to restrict the OpenJPA tasks to e.g. a single package which
     * contains all the entities.
     *   
     * @parameter default-value="**\/*.class"
     */
    private String includes;

    /**
     * Comma separated list of excludes to scan searchDir to pass to the jobs.
     * This option may be used to stop OpenJPA tasks from scanning non-JPA classes
     * (which usually leads to warnings such as "Type xxx has no metadata")
     * 
     * @parameter default-value="";
     */
    private String excludes;

    /**
     * Additional properties passed to the OpenJPA tools.
     * 
     * @parameter
     */
    private Properties toolProperties;

    /**
     * Used if a non-default file location for the persistence.xml should be used
     * If not specified, the default one in META-INF/persistence.xml will be used.
     * Please note that this is not a resource location but a file path!
     *  
     * @parameter
     */
    private String persistenceXmlFile;
    
    /**
     * <p>This setting can be used to override any openjpa.ConnectionDriverName set in the
     * persistence.xml. It can also be used if the persistence.xml contains no connection
     * information at all.<P>
     * 
     * Sample:
     * <pre>
     * &lt;connectionDriverName&gt;com.mchange.v2.c3p0.ComboPooledDataSource&lt;/connectionDriverName&gt;
     * </pre>
     * 
     * This is most times used in conjunction with {@link #connectionProperties}.
     * 
     * @parameter
     */
    private String connectionDriverName;

    /** the string used for passing information about the connectionDriverName */
    protected static final String OPTION_CONNECTION_DRIVER_NAME = "ConnectionDriverName";

    /**
     * <p>Used to define the credentials or any other connection properties.</p>
     * 
     * Sample:
     * <pre>
     * &lt;connectionProperties&gt;
     *   driverClass=com.mysql.jdbc.Driver,
     *   jdbcUrl=jdbc:mysql://localhost/mydatabase,
     *   user=root,
     *   password=,
     *   minPoolSize=5,
     *   acquireRetryAttempts=3,
     *   maxPoolSize=20
     * &lt;/connectionProperties&gt;
     * </pre>
     *
     * This is most times used in conjunction with {@link #connectionDriverName}.
     *
     * @parameter
     */
    private String connectionProperties;
    
    /** the string used for passing information about the connectionProperties */
    protected static final String OPTION_CONNECTION_PROPERTIES = "ConnectionProperties";

    
    /**
     * List of all class path elements that will be searched for the
     * <code>persistence-enabled</code> classes and resources expected by
     * PCEnhancer.
     * 
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> compileClasspathElements;
    
    /**
     * Setting this parameter to <code>true</code> will force
     * the execution of this mojo, even if it would get skipped usually.
     *  
     * @parameter expression="${forceOpenJpaExecution}"
     *            default-value=false
     * @required
     */
    private boolean forceMojoExecution; 
    
    /**
     * The Maven Project Object
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /** the properties option is used for passing information about the persistence.xml file location */
    protected static final String OPTION_PROPERTIES_FILE = "propertiesFile";
    
    /** 
     * The properties option is used for passing information about the persistence.xml 
     * classpath resource and the default unit 
     */
    protected static final String OPTION_PROPERTIES = "properties";

    /**
     * When <code>true</code>, skip the execution.
     * @since 1.0
     * @parameter default-value="false"
     */
    private boolean skip;

    /**
     * default ct
     */
    public AbstractOpenJpaMojo() 
    {
        super();
    }

    /**
     * The File where the class files of the entities to enhance reside
     * @return normaly the entity classes are located in target/classes
     */
    protected File getEntityClasses()
    {
        return classes; 
    }
    
    /**
     * This function retrieves the injected classpath elements for the current mojo.
     * @return List of classpath elements for the compile phase
     */
    protected List<String> getClasspathElements() 
    {
        return compileClasspathElements;
    }
    
    /**
     * Get the options for the various OpenJPA tools.
     * @return populated Options
     */
    protected abstract Options getOptions();
    
    /**
     * <p>Determine if the mojo execution should get skipped.</p>
     * This is the case if:
     * <ul>
     *   <li>{@link #skip} is <code>true</code></li>
     *   <li>if the mojo gets executed on a project with packaging type 'pom' and
     *       {@link #forceMojoExecution} is <code>false</code></li>
     * </ul>
     * 
     * @return <code>true</code> if the mojo execution should be skipped.
     */
    protected boolean skipMojo() 
    {
        if ( skip )
        {
            getLog().info( "Skip sql execution" );
            return true;
        }
        
        if ( !forceMojoExecution && project != null && "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping sql execution for project with packaging type 'pom'" );
            return true;
        }
        
        return false;
    }
    
    /**
     * This function will usually get called by {@link #getOptions()}
     * @return the Options filled with the initial values
     */
    protected Options createOptions()
    {
        Options opts = new Options();
        if ( toolProperties != null )
        {
          opts.putAll( toolProperties );
        }
        
        if ( persistenceXmlFile != null )
        {
            opts.put( OPTION_PROPERTIES_FILE, persistenceXmlFile );
            getLog().debug( "using special persistence XML file: " + persistenceXmlFile );
        }

        if ( connectionDriverName != null )
        {
            opts.put( OPTION_CONNECTION_DRIVER_NAME, connectionDriverName );
        }

        if ( connectionProperties != null )
        {
            opts.put( OPTION_CONNECTION_PROPERTIES, connectionProperties );
        }

        return opts;
    }
    
    /**
     * This will prepare the current ClassLoader and add all jars and local
     * classpaths (e.g. target/classes) needed by the OpenJPA task.
     * 
     * @throws MojoExecutionException on any error inside the mojo
     */
    protected void extendRealmClasspath() 
        throws MojoExecutionException 
    { 
        List<URL> urls = new ArrayList<URL>();

        for(String fileName: getClasspathElements()) { 
            File pathElem = new File(fileName);
            try
            {
                URL url = pathElem.toURI().toURL();
                urls.add( url );
                getLog().debug( "Added classpathElement URL " + url );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "Error in adding the classpath " + pathElem, e );
            }
        }

        ClassLoader jpaRealm =
            new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), getClass().getClassLoader() );

        // set the new ClassLoader as default for this Thread
        Thread.currentThread().setContextClassLoader( jpaRealm );
    }
    
    /**
     * Locates and returns a list of class files found under specified class
     * directory.
     * 
     * @return list of class files.
     * @throws MojoExecutionException if there was an error scanning class file
     *             resources.
     */
    protected List<File> findEntityClassFiles() throws MojoExecutionException 
    {
        List<File> files = new ArrayList<File>();
    
        try
        {
            files = (List<File>) FileUtils.getFiles( getEntityClasses(), includes, excludes );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error while scanning for '" + includes + "' in " + "'"
                                              + getEntityClasses().getAbsolutePath() + "'.", e );
        }

        return files;
    }

    /**
     * @param files List of files
     * @return the paths of the given files as String[]
     */
    protected String[] getFilePaths( List<File> files ) 
    {
        String[] args = new String[ files.size() ];
        for ( int i = 0; i < files.size(); i++ )
        {
            File file = files.get( i );
    
            args[ i ] = file.getAbsolutePath();
        }
        return args;
    }


}
