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
package org.apache.openjpa.util;

import java.util.stream.Stream;

public class BlacklistClassResolver {
    private static final String MATCH_ANY = "*";

    public static final BlacklistClassResolver DEFAULT = new BlacklistClassResolver(
        toArray(System.getProperty(
            "openjpa.serialization.class.blacklist",
            "org.codehaus.groovy.runtime.,org.apache.commons.collections4.functors.,org.apache.xalan")),
        toArray(System.getProperty("openjpa.serialization.class.whitelist")));

    private final boolean allWhite;
    private final boolean allBlack;
    private final String[] blacklist;
    private final String[] whitelist;

    protected BlacklistClassResolver(final String[] blacklist, final String[] whitelist) {
        allWhite = Stream.of(whitelist).anyMatch(white -> MATCH_ANY.equals(white));
        allBlack = Stream.of(blacklist).anyMatch(black -> MATCH_ANY.equals(black));

        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    protected boolean isBlacklisted(final String name) {
        if (allWhite || contains(whitelist, name)) {
            return false;
        }
        return allBlack || contains(blacklist, name);
    }

    public final String check(final String name) {
        if (isBlacklisted(name)) {
            throw new SecurityException(name + " is not whitelisted as deserialisable, prevented before loading.");
        }
        return name;
    }

    private static String[] toArray(final String property) {
        return property == null
            ? new String[] {}
            : Stream.of(property.split(" *, *"))
                .filter(item -> item != null && !item.isEmpty())
                .toArray(String[]::new);
    }

    private static boolean contains(final String[] list, String name) {
        for (final String white : list) {
            if (name.startsWith(white)) {
                return true;
            }
        }
        return false;
    }
}
