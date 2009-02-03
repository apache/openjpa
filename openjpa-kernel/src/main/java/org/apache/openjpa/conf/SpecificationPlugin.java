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
package org.apache.openjpa.conf;

import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * A plug-in for Specification that enforces certain overwriting rules.
 * 
 * @author Pinaki Poddar
 *
 */
public class SpecificationPlugin extends ObjectValue {
    private Log _log;
    
    protected static final Localizer _loc = Localizer.forPackage
        (SpecificationPlugin.class);
    
    public SpecificationPlugin(String prop, Log log) {
        super(prop);
        _log = log; 
    }
    
    @Override
    public Class<?> getValueType() {
        return Specification.class;
    }
    
    /**
     * Set a value from the given String after validating.
     * 
     * @param str can be null to set the Specification to null.
     * If non-null, then the String must be in Specification format
     * @see Specification#create(String)  
     */
    @Override
    public void setString(String str) {
        if (str == null)
            set(null);
        else {
            this.set(Specification.create(str));
        }
    }
    
    /**
     * Set a value from the given object after validating.
     * 
     * @param obj can be null to set the Specification to null.
     */
    @Override
    public void set(Object obj) {
        if (obj == null) {
            super.set(null);
            return;
        }
        if (obj instanceof Specification == false) {
            throw new UserException(_loc.get("spec-wrong-obj", obj, 
                obj.getClass())).setFatal(true);
        }
        validateOverwrite((Specification)obj);
        super.set(obj);
    }
    
    /**
     * Validates if the currently Specification is set.
     * Given newSpec must be equal to the current Specification and must have
     * a major version number equal or less than the current one.
     * 
     * @exception fatal UserException if newSpec is not equal to the current
     * Specification or has a higher major version.
     * 
     * @see Specification#equals(Object)
     */
    protected void validateOverwrite(Specification newSpec) {
        Specification current = (Specification)get();
        if (current != null) {
            if (!current.equals(newSpec)) {
                throw new UserException(_loc.get("spec-different", newSpec, 
                    current)).setFatal(true);
            }
            if (current.compareVersion(newSpec) < 0) {
                throw new UserException(_loc.get("spec-version-higher", 
                    newSpec, current)).setFatal(true);
            }
            if (current.compareVersion(newSpec) > 0) {
                _log.warn(_loc.get("spec-version-lower", newSpec, current));
            }
        }
    }
}
