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

package org.apache.openjpa.lib.util.git;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtils {
    static final Pattern fullRevisionPattern = Pattern.compile("[0-9A-Fa-f]+(([Mm]+)?)");
    static final Pattern revisionPattern = Pattern.compile("[0-9A-Fa-f]+");
    
    /**
     *  A public worker method that takes the output from running the ant script in the pom.xml that
     *  removes the trailing 'M' produced with builds that have uncommitted changes.
     *  
     *  For example: fef543bM to fef543b (267342907)
     *  
     *  @param gitinfo
     *  @return The formatted int version, or -1 if gitinfo is null or unparsable.
     */
    public static int convertGitInfoToPCEnhancerVersion(String gitinfo) {
        if (gitinfo == null || fullRevisionPattern.matcher(gitinfo).matches() == false) {
            return -1;
        }
        
        Matcher matcher = revisionPattern.matcher(gitinfo);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(), 16);
        }
        
        return -1;
    }
}
