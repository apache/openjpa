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
package org.apache.openjpa.enhance.stats;

import java.util.Locale;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PCEnhancer.AuxiliaryEnhancer;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;

/**
 * FetchStatisticsAuxEnhancer adds the call back function to each persistent fields in the persistent entity which
 * will invoke the hit method from FetchStatsCollector whenever the field is fetched at runtime.
 */
public class FetchStatisticsAuxEnhancer implements AuxiliaryEnhancer {

    private final String IGNORE_METHODS_REGEX = "(pc(.)*GenericContext)?(pc(.)*StateManager)?"
        + "(pc(.)*DetachedState)?(pc(.)*EnhancementContractVersion)?(pc(.)*ManagedFieldCount)?(pc(.)*GetVersion)?";

    @Override
    public void run(ClassNode classNode, ClassMetaData cmd) {
        addEnhancement(classNode, cmd);
    }

    @Override
    public boolean skipEnhance(MethodNode m) {
        return false;
    }

    private void addEnhancement(ClassNode classNode, ClassMetaData cmd) {
        Log log = cmd.getRepository().getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
        FetchStatsCollector.setlogger(log);
        String className = classNode.name.replace("/", ".");
        for (MethodNode meth : classNode.methods) {
            String methodName = meth.name;
            FieldMetaData fmd = getFieldName(methodName, cmd);
            if (fmd != null && needsTracking(fmd, methodName, cmd)) {
                String fqn = className + "." + fmd.getName();
                FetchStatsCollector.registerField(fqn);
                FetchStatsCollector.registerEntity(cmd);

                InsnList instructions = new InsnList();

                instructions.add(AsmHelper.getLoadConstantInsn(fqn));
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(FetchStatsCollector.class),
                                                    "hit",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))));
                meth.instructions.insert(instructions);
            }
        }
    }

    private boolean needsTracking(FieldMetaData fmd, String methName, ClassMetaData cmd) {
        // Skim out primary key(s), versions, and LAZY fields
        if (fmd.isPrimaryKey() || fmd.isVersion() || !fmd.isInDefaultFetchGroup())
            return false;

        if (AccessCode.isField(fmd) && methName.toLowerCase(Locale.ENGLISH).startsWith("pcget")) {
            return true;
        } else if (AccessCode.isProperty(fmd) && methName.toLowerCase(Locale.ENGLISH).startsWith("get")
            || methName.toLowerCase(Locale.ENGLISH).startsWith("pcis")) {
            return true;
        }
        return false;
    }

    private FieldMetaData getFieldName(String methName, ClassMetaData cmd) {
        FieldMetaData res = null;
        String fieldName = null;
        if (methName.matches(IGNORE_METHODS_REGEX)) {
            return res;
        } else if (methName.startsWith("pcGet")) {
            // field access
            fieldName = methName.substring(5);
        } else if (methName.toLowerCase(Locale.ENGLISH).startsWith("get")) {
            // property access
            fieldName = StringUtil.uncapitalize(methName.substring(3));
        } else if (methName.startsWith("pcis")) {
            fieldName = methName.substring(4).toLowerCase(Locale.ENGLISH);
        }

        for (FieldMetaData fmd : cmd.getDeclaredFields()) {
            String fmdName = fmd.getName();
            if (fmdName.equals(fieldName)) {
                return fmd;
            }
        }
        return null;
    }
}
