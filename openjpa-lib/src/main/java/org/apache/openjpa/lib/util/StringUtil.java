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
package org.apache.openjpa.lib.util;

import java.util.ArrayList;
import java.util.List;

public final class StringUtil {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private StringUtil() {
    }

    /**
     * Splits the given string on the given token. Follows the semantics
     * of the Java 1.4 {@link String#split(String,int)} method, but does
     * not treat the given token as a regular expression.
     */
    public static String[] split(String str, String token, int max) {
        if (str == null || str.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("token: [" + token + "]");
        }

        // split on token
        List<String> ret = new ArrayList<>();
        int start = 0;
        int len = str.length();
        int tlen = token.length();

        int pos = 0;
        while (pos != -1) {
            pos = str.indexOf(token, start);
            if (pos != -1) {
                ret.add(str.substring(start, pos));
                start = pos + tlen;
            }
        }
        if (start < len) {
            ret.add(str.substring(start));
        } else if (start == len) {
            ret.add("");
        }


        // now take max into account; this isn't the most efficient way
        // of doing things since we split the maximum number of times
        // regardless of the given parameters, but it makes things easy
        if (max == 0) {
            int size = ret.size();
            // discard any trailing empty splits
            while (ret.get(--size).isEmpty()) {
                ret.remove(size);
            }
        } else if (max > 0 && ret.size() > max) {
            // move all splits over max into the last split
            StringBuilder sb = new StringBuilder(256);
            sb.append(ret.get(max-1));
            ret.remove(max-1);
            while (ret.size() >= max) {
                sb.append(token).append(ret.get(max-1));
                ret.remove(max-1);
            }
            ret.add(sb.toString());
        }
        return ret.toArray(new String[ret.size()]);
    }

}
