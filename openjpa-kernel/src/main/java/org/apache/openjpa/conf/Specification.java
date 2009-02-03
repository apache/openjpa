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

import java.text.MessageFormat;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * An immutable representation of a Specification supported by OpenJPA.
 * 
 * Available via {@linkplain OpenJPAConfiguration#getSpecificationInstance()()} 
 * for configuration that may depend on Specification version.
 * 
 * @author Pinaki Poddar
 *
 */
public class Specification {
    private String _name;
    private int    _major;
    private String _minor;
    private String _description;
    
    static final MessageFormat _format = new MessageFormat("{0} {1}.{2}");
    static final String _printableFormat = "<name> [<major>[.<minor>]]";
    static final MessageFormat _vformat = new MessageFormat("{0}.{1}");
    static final String _printableVersionFormat = "<major>[.<minor>]";
    private static Localizer _loc = Localizer.forPackage(Specification.class);
    
    private Specification(String name, int major, String minor) {
        this._name = name == null ? "" : name.trim();
        this._major = major;
        this._minor = minor == null ? "" : minor.trim();
    }
    
    /**
     * Construct from a String that encodes name and version fields.
     * 
     * @param fullName a encoded string in the following prescribed format.
     * <code>name major.minor</code> e.g. <code>JPA 2.0-draft</code>
     * Only the 'name' field is mandatory. 
     * 'major' version defaults to 1 and must be an integer. 
     * 'minor' version defaults to 0 and can be a String. 
     */
    public static Specification create(String fullName) {
        try {
            Object[] tokens = _format.parse(fullName);
            return new Specification(tokens[0].toString(),
                tokens.length > 1 ? Integer.parseInt(tokens[1].toString()) : 1,
                tokens.length > 2 ? tokens[2].toString() : "0");
        } catch (Exception e) {
            throw new UserException(_loc.get("spec-wrong-format", 
                fullName, _printableFormat));
        }
    }
    
    /**
     * Construct from a String and version.
     * 
     * @param name is the name of the Specification.
     * @param version a encoded string in the following prescribed format.
     * <code>major.minor</code> e.g. <code>2.0-draft</code>
     * 'major' version defaults to 1 and must be an integer. 
     * 'minor' version defaults to 0 and can be a String. 
     */
    public static Specification create(String name, String version) {
        try {
            Object[] tokens = _vformat.parse(version);
            return new Specification(name,
                tokens.length > 0 ? Integer.parseInt(tokens[0].toString()) : 1,
                tokens.length > 1 ? tokens[1].toString() : "0");
        } catch (Exception e) {
            throw new UserException(_loc.get("spec-wrong-version-format", 
                version, _printableVersionFormat));
        }
    }
    
    /**
     * Construct from a String and major and minor version.
     * 
     * @param name is the name of the Specification.
     * @param version a encoded string in the following prescribed format.
     * <code>major.minor</code> e.g. <code>2.0-draft</code>
     * 'major' version defaults to 1 and must be an integer. 
     * 'minor' version defaults to 0 and can be a String. 
     */
    public static Specification create(String name, int major, String minor) {
        return new Specification(name, major, minor);
    }
    
    /**
     * Construct from a String and major and minor version.
     * 
     * @param name is the name of the Specification.
     * @param version a encoded string in the following prescribed format.
     * <code>major.minor</code> e.g. <code>2.0-draft</code>
     * 'major' version defaults to 1 and must be an integer. 
     * 'minor' version defaults to 0 and can be a String. 
     */
    public static Specification create(String name, int major) {
        return new Specification(name, major, "0");
    }

    /**
     * Construct from a String and major and minor version.
     * 
     * @param name is the name of the Specification.
     * @param version a encoded string in the following prescribed format.
     * <code>major.minor</code> e.g. <code>2.0-draft</code>
     * 'major' version defaults to 1 and must be an integer. 
     * 'minor' version defaults to 0 and can be a String. 
     */
    public static Specification create(String name, int major, int minor) {
        return new Specification(name, major, ""+minor);
    }
    
    public Specification setDescription(String desc) {
        _description = desc;
        return this;
    }
    
    public String getName() {
        return _name;
    }

    public int getMajorVersion() {
        return _major;
    }

    public String getMinorVersion() {
        return _minor;
    }

    public String getDescription() {
        return _description;
    }

    /**
     * Get the Specification encoding format in {@link MessageFormat} syntax.   
     */
    public static String getFormat() {
        return _printableFormat;
    }
    
    /**
     * Get the Specification version encoding format in {@link MessageFormat} 
     * syntax.   
     */
    public static String getVersionFormat() {
        return _printableVersionFormat;
    }
    
    /**
     * Affirms if the given argument is equal to this receiver.
     * They are equal if
     *    other is a String that equals this receiver's name ignoring case and
     *    any leading or trailing blank spaces.
     *    other is a Specification whose name equals this receiver's name 
     *    ignoring case and any leading or trailing blank spaces.
     *    or if they are same reference (of course)
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (other instanceof String)
            return _name.equalsIgnoreCase(((String)other).trim());
        if (other instanceof Specification)
            return _name.equalsIgnoreCase((((Specification)other)._name)
                .trim());
        return false;
    }
    
    /**
     * Compares major version number of the given Specification with this 
     * receiver.

     * @return 0 if they are equal.
     *       > 0 if this receiver is higher version.
     *       < 0 if this receiver is lower version.
     */
    public int compareVersion(Specification other) {
        return _major > other._major ? 1 : _major == other._major ? 0 : -1;
        
    }
    
    public String toString() {
        return MessageFormat.format(_format.toPattern(), _name, _major, _minor);
    }
}
