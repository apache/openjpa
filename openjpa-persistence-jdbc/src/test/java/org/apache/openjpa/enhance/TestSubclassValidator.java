/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.enhance;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.TemporaryClassLoader;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.common.apps.Department;
import org.apache.openjpa.persistence.common.apps.RuntimeTest2;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.EnhancementProject;
import org.apache.xbean.asm9.tree.ClassNode;
import org.junit.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class TestSubclassValidator extends SingleEMFTestCase {
    @Override
    public void setUp() throws Exception {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
                Department.class,
                RuntimeTest2.class,
                EnhanceableGetterEntity.class,
                UnenhancedPropertyAccess.class,
                CLEAR_TABLES);
    }

    @Test
    public void testBcSubclassValidator() throws Exception {
        EnhancementProject project = new EnhancementProject();
        TemporaryClassLoader tempCl = new TemporaryClassLoader(this.getClass().getClassLoader());
        final OpenJPAConfiguration conf = emf.getConfiguration();

        final MetaDataRepository repos = conf.newMetaDataRepositoryInstance();
        repos.setSourceMode(MetaDataModes.MODE_META);

        final Log log = conf.getLog(OpenJPAConfiguration.LOG_ENHANCE);

        {
            ClassNode classNode = AsmHelper.readClassNode(EnhanceableGetterEntity.class.getClassLoader(), EnhanceableGetterEntity.class.getName());
            project.loadClass(EnhanceableGetterEntity.class.getName(), tempCl);
            final ClassMetaData meta = repos.getMetaData(tempCl.loadClass(EnhanceableGetterEntity.class.getName()), tempCl, false);
            PCSubclassValidator subclassValidator = new PCSubclassValidator(meta, classNode, log, true);
            subclassValidator.assertCanSubclass();
        }

        {
            ClassNode classNode = AsmHelper.readClassNode(UnenhancedPropertyAccess.class.getClassLoader(), UnenhancedPropertyAccess.class.getName());
            project.loadClass(UnenhancedPropertyAccess.class.getName(), tempCl);
            final ClassMetaData meta = repos.getMetaData(tempCl.loadClass(UnenhancedPropertyAccess.class.getName()), tempCl, false);
            PCSubclassValidator subclassValidator = new PCSubclassValidator(meta, classNode, log, true);
            subclassValidator.assertCanSubclass();
        }
    }

    @Entity
    @Access(AccessType.PROPERTY)
    public static class EnhanceableGetterEntity {

        @Id
        private String id;

        @Basic
        protected String name;

        @Basic
        private String another;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDifferent() {
            return another;
        }

        public void setDifferent(String another) {
            this.another = another;
        }
    }
}
