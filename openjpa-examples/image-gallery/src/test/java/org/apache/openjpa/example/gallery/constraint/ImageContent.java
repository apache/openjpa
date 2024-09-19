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
package org.apache.openjpa.example.gallery.constraint;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.openjpa.example.gallery.ImageType.GIF;
import static org.apache.openjpa.example.gallery.ImageType.JPEG;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import org.apache.openjpa.example.gallery.ImageType;

/**
 * Attribute-level annotation used to specify an image content constraint.  Uses
 * ImageContentValidator to perform the validation.
 */
@Documented
@Constraint(validatedBy = ImageContentValidator.class)
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ImageContent {
    String message() default "Image data is not a supported format.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    ImageType[] value() default { GIF, JPEG };
}
