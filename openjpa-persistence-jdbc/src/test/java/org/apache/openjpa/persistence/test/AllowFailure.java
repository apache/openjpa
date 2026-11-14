/**
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
package org.apache.openjpa.persistence.test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Signals to the harness to ignore if the annotated test case/method fail.
 *
 * @author Pinaki Poddar
 *
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@ExtendWith(AllowFailureExtension.class)
public @interface AllowFailure {
    public static final String ALLOW_FAILURE_LOG = "log";
    public static final String ALLOW_FAILURE_IGNORE = "ignore";
    public static final String ALLOW_FAILURE_SYS_PROP = "tests.openjpa.allowfailure";

    boolean value() default true;
    String message() default "";
}

class AllowFailureExtension implements TestExecutionExceptionHandler, ExecutionCondition {
    private static final Logger logger = Logger.getLogger(AllowFailureExtension.class.getCanonicalName());
    private static String allowFailureConfig = System.getProperty(AllowFailure.ALLOW_FAILURE_SYS_PROP, AllowFailure.ALLOW_FAILURE_IGNORE);

    private AllowFailure getAllowFailure(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestMethod(), AllowFailure.class)
                .orElse(AnnotationSupport.findAnnotation(context.getTestClass(), AllowFailure.class).orElse(null));
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        AllowFailure allowFailure = getAllowFailure(context);
        if (allowFailure != null && allowFailure.value() && AllowFailure.ALLOW_FAILURE_IGNORE.equalsIgnoreCase(allowFailureConfig)) {
            return ConditionEvaluationResult.disabled("Disabled with tests.openjpa.allowfailure=ignore option");
        }
        return ConditionEvaluationResult.enabled(null);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        AllowFailure allowFailure = getAllowFailure(context);

        if (allowFailure != null && allowFailure.value() && AllowFailure.ALLOW_FAILURE_LOG.equalsIgnoreCase(allowFailureConfig)) {
            logger.warning("*** FAILED (but ignored): " + context.getRequiredTestMethod().getName());
            logger.warning("***              Reason : " + allowFailure.message());
            logger.log(Level.WARNING, "Stacktrace of failure", throwable);
            return;
        }

        throw throwable;
    }
}
