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

package org.apache.openjpa.persistence.jest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A request carries requisite data for a JPA operation to be performed. 
 * The request is populated by parsing an input data stream.
 * 
 * 
 * @author Pinaki Poddar
 * 
 */
@SuppressWarnings("serial")
public abstract class JESTRequest implements Request {
    private String _method;
    private String _protocol;
    private String _action;
    private String _body;
    private LinkedHashMap<String, String> _qualifiers = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> _params = new LinkedHashMap<String, String>();
    private Map<String, List<String>> _headers = new HashMap<String, List<String>>();
    private ParseState _state;
    private StringBuffer buf = new StringBuffer();
    private LinkedList<Token> _stack = new LinkedList<Token>();
    
    public static final List<String> METHODS = Arrays.asList(new String[]{"GET","POST","PUT","DELETE"});
    
    /**
     * Parse States.
     */
    static enum ParseState {
        INIT, ACTION, QUALIFIER_KEY, QUALIFIER_VALUE, PARAM_KEY, PARAM_VALUE, END
    };


    public String getMethod() {
        return _method;
    }

    void setMethod(String method) {
        if (_method == null) {
            if (method != null && METHODS.contains(method.toUpperCase())) {
                _method = method.toUpperCase();
            } else {
                throw new IllegalArgumentException("Unsupported method " + method);
            }
        } else if (!_method.equalsIgnoreCase(method)) {
            throw new IllegalStateException("Method can not be changed to [" + method + "]. " +
                "Current method [" + _method + "]");
        }
    }

    public String getProtocol() {
        return _protocol == null ? "HTTP/1.1" : _protocol;
    }

    void setProtocol(String protocol) {
        if (_protocol == null) {
            if (protocol != null && protocol.toUpperCase().startsWith("HTTP")) {
                _protocol = protocol.toUpperCase();
            } else {
                throw new IllegalArgumentException("Unsupported protocol " + protocol);
            }
        } else if (!_protocol.equalsIgnoreCase(protocol)) {
            throw new IllegalStateException("Protocol can not be changed to [" + protocol + "]. " +
                "Current protocol [" + _protocol + "]");
        }
    }

    /**
     * Sets an action. Once set, an action can not be modified.
     * 
     * @param action
     */
    private void setAction(String action) {
        if (_action == null) {
            _action = action;
        } else if (!_action.equals(action)) {
            throw new IllegalStateException("Action can not be [" + action + "]. Already set to [" + _action + "]");
        }
    }

    public String getAction() {
        return _action == null ? "" : _action;
    }

    public String getBody() {
        return _body;
    }

    private void setQualifier(String key, String value) {
        _qualifiers.put(key, value);
    }

    public String getQualifier(String key) {
        return _qualifiers.get(key);
    }

    public Map<String, String> getQualifiers() {
        return Collections.unmodifiableMap(_qualifiers);
    }

    public boolean hasQualifier(String key) {
        return _qualifiers.containsKey(key);
    }

    private void setParameter(String key, String value) {
        _params.put(key, value);
    }

    public String getParameter(String key) {
        return _params.get(key);
    }

    public boolean hasParameter(String key) {
        return _params.containsKey(key);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(_params);
    }
    
    public Map.Entry<String, String> getParameter(int n) {
        if (n >= _params.size())
            throw new NoSuchElementException("Index " + n + " size " + _params.size());
        int i = 0;
        for (Map.Entry<String, String> entry : _params.entrySet()) {
            if (i == n) {
                return entry;
            }
            i++;
        }
        return null;
    }

    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(_headers);
    }

    public List<String> getHeader(String key) {
        return _headers.get(key);
    }
    
    
    public void read(List<String> lines) throws IOException {
        parse(lines.get(0));
        int i = 1;
        for (; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() == 0) {
                break;
            } else {
                parseHeader(line);
            }
        }
        parseBody(lines.subList(i, lines.size()));
    }

    protected void parseHeader(String line) throws IOException {
        String key = null;
        StringBuilder token = new StringBuilder();
        int N = line.length();
        for (int i = 0; i < N; i++) {
            char c = line.charAt(i);
            if (c == ':' && key == null) {
                key = token.toString().trim();
                token.delete(0, token.length());
            } else {
                token.append(c);
            }
        }
        if (key != null) {
            _headers.put(key, Collections.singletonList(token.toString().trim()));
        }
    }

    protected void parseBody(List<String> lines) {
        if (lines == null || lines.isEmpty())
            return;
        for (String line : lines) {
            if (_body == null) {
                _body = line;
            } else {
                _body = _body + line;
            }
        }
    }

    /**
     * Parses JEST stream and populates a request.
     * 
     */
     protected void parse(String s) {
            char[] chars = s.toCharArray();
            _state = ParseState.INIT;
            _stack.clear();

            for (int i = 0; i < chars.length; i++) {
                char ch = chars[i];
                switch (_state) {
                    case INIT:
                        if (ch == '/') {
                            transit(ParseState.ACTION);
                        } else if (!Character.isWhitespace(ch)) {
                            parseError(ch, i, s, true, ' ');
                        }
                        break;

                    case ACTION:
                        if (ch == '/') {
                            transit(ParseState.QUALIFIER_KEY);
                        } else if (ch == '?') {
                            transit(ParseState.PARAM_KEY);
                        } else {
                            buf.append(ch);
                        }
                        break;

                    case QUALIFIER_KEY:
                        if (Character.isJavaIdentifierPart(ch)) {
                            buf.append(ch);
                        } else if (ch == '=') {
                            transit(ParseState.QUALIFIER_VALUE);
                        } else if (ch == '/') {
                            transit(ParseState.QUALIFIER_KEY);
                        } else if (ch == '?') {
                            transit(ParseState.PARAM_KEY);
                        } else {
                            parseError(ch, i, s, true, '/', '?', '=');
                        }
                        break;

                    case QUALIFIER_VALUE:
                        if (Character.isJavaIdentifierPart(ch)) {
                            buf.append(ch);
                        } else if (ch == '/') {
                            transit(ParseState.QUALIFIER_KEY);
                        } else if (ch == '?') {
                            transit(ParseState.PARAM_KEY);
                        } else {
                            parseError(ch, i, s, true, '/', '?');
                        }
                        break;

                    case PARAM_KEY:
                        if (Character.isJavaIdentifierPart(ch)) {
                            buf.append(ch);
                        } else if (ch == '=') {
                            if (isQueryKey())
                                buf.append(ch);
                            else
                                transit(ParseState.PARAM_VALUE);
                        } else if (ch == ';') {
                            transit(ParseState.PARAM_KEY);
                        } else if (isQueryKey() && isQueryChar(ch)) {
                            buf.append(ch);
                        } else {
                            parseError(ch, i, s, true, ';', '=');
                        }
                        break;

                    case PARAM_VALUE:
                        if (Character.isJavaIdentifierPart(ch)) {
                            buf.append(ch);
                        } else if (ch == ';') {
                            transit(ParseState.PARAM_KEY);
                        } else {
                            parseError(ch, i, s, true, ';');
                        }
                        break;
                    default:
                        throw new RuntimeException("ParseError: '" + ch + "' at " + i + " in [" + s + "]. "
                            + "Unknown state " + _state);
                }
            }
            if (buf.length() > 0) {
                transit(ParseState.END);
            }
        }

        /**
         * Affirms if parsing a query string.
         */
        private boolean isQueryKey() {
            return "query".equals(_action) && _stack.size() == 1;
        }
        
        /**
         * Affirms if the given character is valid in a query string 
         */
        private boolean isQueryChar(char c) {
            return c == ' ' || c == '.' || c == ':' || c == '?' || c == '\'';
        }

        /**
         * Transitions to a new parse state.
         * 
         * @param to target parse state
         */
        void transit(ParseState to) {
            String token = buf.toString();
            switch (_state) {
                case ACTION:
                    setAction(token);
                    break;
                case QUALIFIER_KEY:
                    setQualifier(token, null);
                    break;
                case QUALIFIER_VALUE:
                    setQualifier(_stack.peekLast().getValue(), token);
                    break;
                case PARAM_KEY:
                    setParameter(token, null);
                    break;
                case PARAM_VALUE:
                    setParameter(_stack.peekLast().getValue(), token);
                    break;

            }
            if (_state != ParseState.INIT && to != ParseState.END) {
                _stack.add(new Token(_state, token));
            }
            buf.delete(0, buf.length());
            _state = to;
        }

        protected void parseError(char ch, int pos, String line, boolean java, char... expected) {
            throw new RuntimeException("ParseError: Encountered '" + ch + "' at " + pos + " in [" + line + "] while "
                + "parsing " + _state + ". Expected " + Arrays.toString(expected) + (java ? " or Java identifer" : ""));

        }

        /**
         * Token in a JEST stream.
         * 
         */
        static class Token {
            final ParseState _type;
            final String _value;

            public Token(ParseState type, String value) {
                _type = type;
                _value = value;
            }

            public ParseState getType() {
                return _type;
            }

            public String getValue() {
                return _value;
            }

            public String toString() {
                return _value + "[" + _type + "]";
            }
        }
    
}
