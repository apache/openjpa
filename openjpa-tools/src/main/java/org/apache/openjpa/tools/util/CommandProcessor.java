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
package org.apache.openjpa.tools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processes command-line or other String-based options. <br>
 * User can register a set of command options. Then this processor will parse a
 * set of Strings to store the values for each of the registered options as well
 * as optionally any unrecognized option values. <br>
 * Provides conversion utility for the options to resolve their string input to
 * File, Integer, InputStream etc. <br>
 * Provides {@link #usage(Class) utility} to output available options.
 * 
 * @author Pinaki Poddar
 * 
 */
public class CommandProcessor {
    private final Map<Option<?>, Object> registeredOptions = new HashMap<Option<?>, Object>();
    private final Set<Option<String>> unregisteredOptions = new HashSet<Option<String>>();
    private boolean allowsUnregisteredOption = true;

    /**
     * Set the option values from the given arguments. All elements of the given
     * array is <em>not</em> consumed, only till the index that appears to be a
     * valid option.
     * 
     * @see #lastIndex(String[])
     * 
     * @param args
     *            an array of arguments.
     * 
     * @return the array elements that are not consumed.
     */
    public String[] setFrom(String[] args) {
        return setFrom(args, 0, args != null ? lastIndex(args) : 0);
    }

    /**
     * Set the option values from the given arguments between the given indices.
     * 
     * @see #lastIndex(String[])
     * 
     * @param args
     *            an array of arguments.
     * 
     * @return the array elements that are not consumed.
     */
    public String[] setFrom(String[] args, int from, int to) {
        if (args == null)
            return null;
        if (args.length == 0)
            return new String[0];
        assertValidIndex(from, args, "Initial index " + from + " is an invalid index to " + Arrays.toString(args));
        assertValidIndex(to, args, "Last index " + to + " is an invalid index to " + Arrays.toString(args));

        int i = from;
        for (; i <= to; i++) {
            String c = args[i];
            Option<?> command = findCommand(c);
            if (command == null) {
                throw new IllegalArgumentException(c + " is not a recongined option");
            }
            if (command.requiresInput()) {
                i++;
            }
            if (i > to) {
                throw new IllegalArgumentException("Command " + c + " requires a value, but no value is specified");
            }
            registeredOptions.put(command, command.convert(args[i]));
        }
        String[] remaining = new String[args.length - to];
        System.arraycopy(args, i - 1, remaining, 0, remaining.length);
        return remaining;
    }

    public boolean validate(String[] args) {
        int i = 0;
        for (Option<?> option : registeredOptions.keySet()) {
            if (option.isMandatory() && option.requiresInput())
                i += 2;
        }
        return args.length >= i;
    }

    /**
     * Generates a short description of usage based on registered options.
     */
    public String usage(Class<?> cls) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Usage:\r\n");
        buffer.append("  $ java ").append(cls.getName());

        List<Option> sortedOptions = new ArrayList<Option>();
        sortedOptions.addAll(registeredOptions.keySet());
        Collections.sort(sortedOptions);
        int L = 0;
        for (Option<?> option : sortedOptions) {
            L = Math.max(L, option.getName().length());
        }
        for (Option<?> option : sortedOptions) {
            buffer.append(" ");
            if (!option.isMandatory())
                buffer.append("[");
            buffer.append(option.getName()).append(" ").append("value");
            if (!option.isMandatory())
                buffer.append("]");
        }
        buffer.append("\r\n");
        for (Option<?> option : sortedOptions) {
            buffer.append("    ");
            buffer.append(option.getName());
            for (int i = 0; i < L - option.getName().length() + 4; i++)
                buffer.append(" ");
            buffer.append(option.getDescription()).append("\r\n");
        }
        return buffer.toString();
    }

    /**
     * Gets number of registered options.
     */
    public int getOptionCount() {
        return registeredOptions.size();
    }

    /**
     * Gets number of registered mandatory options.
     */
    public int getMandatoryOptionCount() {
        int i = 0;
        for (Option<?> option : registeredOptions.keySet()) {
            if (option.isMandatory())
                i++;
        }
        return i;
    }

    /**
     * Gets the last index in the given array that can be processed as an
     * option. The array elements are sequentially tested if they are a valid
     * option name (i.e. starts with - character) and if valid then the next
     * element is consumed as value, if the option requires a value. The search
     * ends when either the array is exhausted or encounters elements that are
     * not options.
     * 
     * @param args
     *            an array of arguments
     * @return the last index that will/can be consumed by this processor.
     */
    int lastIndex(String[] args) {
        int i = 0;
        for (; i < args.length;) {
            Option<?> cmd = findCommand(args[i]);
            if (cmd != null) {
                i++;
                if (cmd.requiresInput()) {
                    i++;
                }
            } else {
                return i;
            }
        }
        return i - 1;
    }
    
    public Option<String> register(boolean mandatory, boolean requiresValue, String... aliases) {
        return register(String.class, mandatory, requiresValue, aliases);
    }

    /**
     * Register the given aliases as a command option.
     * 
     * @param requiresValue
     *            if true then the option must be specified with a value.
     * @param aliases
     *            strings to recognize this option. Each must begin with a dash
     *            character.
     * 
     * @return the command that is registered
     */
    public <T> Option<T> register(Class<T> type, boolean mandatory, boolean requiresValue, String... aliases) {
        Option<T> option = new Option<T>(type, mandatory, requiresValue, aliases);
        registeredOptions.put(option, null);
        return option;
    }

    /**
     * Finds a command with the given name. If no command has been registered
     * with the given name, but this processor allows unrecognized options, then
     * as a result of this call, the unknown name is registered as an option.
     * 
     * @param option
     *            a command alias.
     * 
     * @return null if the given String is not a valid command option name.
     * 
     */
    Option<?> findCommand(String option) {
        if (!Option.isValidName(option))
            return null;
        for (Option<?> registeredOption : registeredOptions.keySet()) {
            if (registeredOption.match(option))
                return registeredOption;
        }
        for (Option<?> unregisteredOption : unregisteredOptions) {
            if (unregisteredOption.match(option))
                return unregisteredOption;
        }
        if (allowsUnregisteredOption) {
            Option<String> cmd = new Option<String>(String.class, false, false, option);
            unregisteredOptions.add(cmd);
            return cmd;
        } else {
            return null;
        }
    }

    /**
     * Gets all the unrecognized command options.
     * 
     * @return empty set if no commands are unrecognized.
     */
    public Set<Option<String>> getUnregisteredCommands() {
        return Collections.unmodifiableSet(unregisteredOptions);
    }

    <T> void assertValidIndex(int i, T[] a, String message) {
        if (i < 0 || (a != null && i >= a.length))
            throw new ArrayIndexOutOfBoundsException(message);
    }

    /**
     * Gets value of the option matching the given alias.
     * 
     * @param alias
     *            an alias.
     * 
     * @return value of the given option.
     */
    public <T> T getValue(String alias) {
        Option<?> cmd = findCommand(alias);
        return (T) getValue(cmd);
    }
    
    public boolean isSet(Option<?> option) {
        return registeredOptions.get(option) != null;
    }
    

    /**
     * Gets value of the given option.
     * 
     * @param opt
     *            an option.
     * 
     * @return value of the given option.
     */
    public <T> T getValue(Option<T> opt) {
        Object val = registeredOptions.get(opt);
        if (val == null)
            val = opt.getDefaultValue();
        return (T) val;
    }

    /**
     * @return the allowsUnregisteredOption
     */
    public boolean getAllowsUnregisteredOption() {
        return allowsUnregisteredOption;
    }

    /**
     * @param allowsUnregisteredOption
     *            the allowsUnregisteredOption to set
     */
    public void setAllowsUnregisteredOption(boolean allowsUnregisteredOption) {
        this.allowsUnregisteredOption = allowsUnregisteredOption;
    }

}
