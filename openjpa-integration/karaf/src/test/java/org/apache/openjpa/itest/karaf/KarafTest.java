/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openjpa.itest.karaf;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.openjpa.integration.tasklist.model.Task;
import org.apache.openjpa.integration.tasklist.model.TaskService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test to prove the openjpa feature works in apache karaf.
 * The test does not use Aries JPA to keep it as bare as possible. Keep in mind that
 * this is not the best practice to use JPA in OSGi.
 * 
 * The DataSource is created using a config and pax-jdbc-config. Openjpa accesses the 
 * DataSource using jndi and aries jndi.
 */
@RunWith(PaxExam.class)
public class KarafTest {
    private static final String DATASOURCE_H2_CFG = "etc/org.ops4j.datasource-h2.cfg";

    Logger LOG = LoggerFactory.getLogger(KarafTest.class);

    @Inject
    TaskService taskService;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven()
            .groupId("org.apache.karaf")
            .artifactId("apache-karaf")
            .version("4.0.7").type("tar.gz");
        MavenArtifactUrlReference openjpa = maven()
            .groupId("org.apache.openjpa")
            .artifactId("openjpa-features")
            .versionAsInProject()
            .type("xml")
            .classifier("features");
        return new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf")
                .unpackDirectory(new File("target/exam")),
            keepRuntimeFolder(),
            features(openjpa, "openjpa", "pax-jdbc-h2", "pax-jdbc-config" , "scr" , "jndi"),
            mavenBundle("org.apache.openjpa", "openjpa-integration-tasklist-model").versionAsInProject(),
            editConfigurationFilePut(DATASOURCE_H2_CFG, "osgi.jdbc.driver.name", "H2"),
            editConfigurationFilePut(DATASOURCE_H2_CFG, "url", "jdbc:h2:test"),
            editConfigurationFilePut(DATASOURCE_H2_CFG, "dataSourceName", "tasklist"),
            //KarafDistributionOption.debugConfiguration("5005", true),
        };
    }
    
    @Test
    public void test1() throws Exception {
        Task task = new Task();
        task.setId(1);
        task.setTitle("Task1");
        taskService.addTask(task);

        Collection<Task> tasks = taskService.getTasks();
        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals("Task1", tasks.iterator().next().getTitle());
    }

}
