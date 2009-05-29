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
package org.apache.openjpa.persistence.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.ValidationMode;
import javax.validation.BeanDescriptor;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.validation.AbstractValidator;
import org.apache.openjpa.validation.ValidationException;

public class ValidatorImpl extends AbstractValidator {
    
    private ValidatorFactory _validatorFactory = null;
    private Validator _validator = null;
    private ValidationMode _mode = ValidationMode.AUTO;
    
    // A map storing the validation groups to use for a particular event type
    private Map<Integer, Class<?>[]> _validationGroups = 
        new HashMap<Integer,Class<?>[]>();
        
    // Lookup table for event to group property mapping 
    private static HashMap<String, Integer> _vgMapping = 
        new HashMap<String, Integer> ();
            
    public static final String VG_PRE_PERSIST = 
        "javax.persistence.validation.group.pre-persist";
    public static final String VG_PRE_REMOVE =
        "javax.persistence.validation.group.pre-remove";
    public static final String VG_PRE_UPDATE =
        "javax.persistence.validation.group.pre-update";
    
    static {
        _vgMapping.put(VG_PRE_PERSIST,
            LifecycleEvent.BEFORE_STORE);
        _vgMapping.put(VG_PRE_REMOVE,
            LifecycleEvent.BEFORE_DELETE);
        _vgMapping.put(VG_PRE_UPDATE,
            LifecycleEvent.BEFORE_UPDATE); 
    }

    /**
     * Default constructor.  Builds a default validator factory, if available
     * and creates the validator.
     */
    public ValidatorImpl() {
        // Add the default validation groups
        _validatorFactory = getDefaultValidatorFactory();
        if (_validatorFactory != null)
            _validator = _validatorFactory.getValidator();
        addDefaultValidationGroups();
    }
    
    /**
     * Type-specific constructor
     * @param validatorFactory Instance of validator factory to use.  Specify
     *        null to use the default factory.
     * @param mode ValdiationMode enum value
     */
    public ValidatorImpl(ValidatorFactory validatorFactory,
        ValidationMode mode) {
        if (validatorFactory != null) {
            _validatorFactory = validatorFactory;
        } else {
            _validatorFactory = getDefaultValidatorFactory();
        }
        if (_validatorFactory != null)
            _validator = _validatorFactory.getValidator();
        addDefaultValidationGroups();
    }

    /**
     * Generic-type constructor 
     * @param validatorFactory an instance to the validatorFactory
     * @param mode validation mode enum as string value
     */    
    public ValidatorImpl(Object validatorFactory,
        String mode) {        
        if (validatorFactory != null && validatorFactory instanceof
                ValidatorFactory) {
            _validatorFactory = (ValidatorFactory)validatorFactory;
        } else {
            _validatorFactory = getDefaultValidatorFactory();
        }
        _mode = Enum.valueOf(ValidationMode.class, mode);
        if (_validatorFactory != null)
            _validator = _validatorFactory.getValidator();
        addDefaultValidationGroups();
    }

    /**
     * Add a validation group for the specific property.  The properties map
     * to a specific lifecycle event.  To disable validation for a group, set
     * the validation group to null.
     * 
     * @param validationGroupName
     * @param vgs
     */
    public void addValidationGroup(String validationGroupName, Class<?>...vgs) {
        Integer event = findEvent(validationGroupName);
        if (event != null) {
            _validationGroups.put(event, vgs);
            return;
        }
        // TODO: Add a localized exception
        throw new IllegalArgumentException();
    }
            
    /**
     * Add a validation group for a specified event.  Event definitions
     * are defined in LifecycleEvent.  To disable validation for a group, set
     * the validation group to null.
     * 
     * @param event
     * @param validationGroup
     */
    public void addValidationGroup(Integer event, Class<?>... validationGroup) {
        _validationGroups.put(event, validationGroup);        
    }
    
    /**
     * Return the validation groups to be validated for a specified event
     * @param event Lifecycle event id
     * @return An array of validation groups
     */
    public Class<?>[] getValidationGroup(Integer event) {
        return _validationGroups.get(event);
    }
    
    /**
     * Returns whether the Validator is validating for the 
     * specified event.  Based on whether validation groups are specified for
     * the event.
     * 
     * @param event the event to check for validation
     * @return returns true if validating for this particular event
     */
    public boolean isValidating(Integer event) {
        return _validationGroups.get(event) != null;
    }

    /**
     * Returns the validation constraints for the specified class
     * 
     * @param cls Class for which constraints to return
     * @return The validation bean descriptor
     */
    public BeanDescriptor getConstraintsForClass(Class<?> cls) {
        return _validator.getConstraintsForClass(cls);
    }

    /**
     * Validates a given instance
     * 
     * @param <T> The instance to validate
     * @param arg0 The class, of type T to validate
     * @param arg1 The property to validate
     * @param arg2 The property value to validate
     * @param event The event id
     * @return A Validation exception if the validator produces one or more
     *         constraint violations.
     */
    @Override
    public <T> ValidationException validate(T arg0, int event) { 
        if (!isValidating(event))
            return null;
        Set<ConstraintViolation<T>> violations = 
            _validator.validate(arg0, getValidationGroup(event));
        if (violations != null && violations.size() > 0) {
            return new ValidationException(
                new ConstraintViolationException(
                    (Set)violations));
        }
        return null;
    }

    /**
     * Validates a property of a given instance
     * 
     * @param <T> The instance to validate
     * @param arg0 The class, of type T to validate
     * @param arg1 The property to validate
     * @param arg2 The property value to validate
     * @param event The event id
     * @return A Validation exception if the validator produces one or more
     *         constraint violations.
     */
    @Override
    public <T> ValidationException validateProperty(T arg0, String property,
        int event) {
        if (!isValidating(event))
            return null;
        Set<ConstraintViolation<T>> violations = 
            _validator.validateProperty(arg0, property, 
                getValidationGroup(event));
        if (violations != null && violations.size() > 0) {
            return new ValidationException(
                new ConstraintViolationException(
                        (Set)violations));
        }
        return null;
    }

    /**
     * Validates a value based upon the constraints applied to a given class
     * attribute.
     * @param <T> The instance type to base validation upon
     * @param arg0 The class of type T to validate
     * @param arg1 The property to validate
     * @param arg2 The property value to validate
     * @param event The event id
     * @return A Validation exception if the validator produces one or more
     *         constraint violations.
     */
    @Override
    public <T> ValidationException validateValue(Class<T> arg0, 
        String arg1, Object arg2, int event)  {
        if (!isValidating(event))
            return null;
        Set<ConstraintViolation<T>> violations = 
            _validator.validateValue(arg0, arg1, arg2, 
                getValidationGroup(event));
        if (violations != null && violations.size() > 0) {
            return new ValidationException(
                new ConstraintViolationException(
                    (Set)violations));
        }
        return null;
    }

    /**
     * Returns whether validation is active for the given event.
     * 
     * @param <T>
     * @param arg0 Type being validated
     * @param event event type
     * @return true if validation is active for the specified event
     */
    @Override
    public <T> boolean validating(T arg0, int event) {
        // TODO: This method will also make a determination based upon which 
        // groups are validating and the group defined on the class
        return isValidating(event);
    }
    
    // Lookup the lifecycle event id for the validationProperty
    private Integer findEvent(String validationProperty) {
        return _vgMapping.get(validationProperty);
    }
    
    // Get the default validator factory
    private ValidatorFactory getDefaultValidatorFactory() {
        ValidatorFactory factory = 
            Validation.buildDefaultValidatorFactory();
        return factory;
    }
    
    // Per JSR-317, the pre-persist and pre-update groups will validate using
    // the default validation group and pre-remove will not validate (no 
    // validation group)
    private void addDefaultValidationGroups() {
        addValidationGroup(VG_PRE_PERSIST, 
            javax.validation.groups.Default.class);
        addValidationGroup(VG_PRE_UPDATE, 
                javax.validation.groups.Default.class);
    }
}
