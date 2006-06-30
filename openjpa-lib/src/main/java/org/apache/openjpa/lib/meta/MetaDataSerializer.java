/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.meta;

import java.io.*;
import java.util.*;
import org.apache.openjpa.lib.log.*;

/**
 * Interface for meta data serializers.
 * 
 * @author Abe White
 * @nojavadoc
 */
public interface MetaDataSerializer {
    public static final int COMPACT = 0;
    public static final int PRETTY = 1;
    public static final int APPEND = 2;
    public static final int VERBOSE = 4;

    /**
     * Serialize the current set of objects to the files from which they were
     * parsed. Any objects for which a source file cannot be determined will
     * not be included in the output.
     * 
     * @param flags bit flags specifying the output flags; e.g.
     * {@link PRETTY #PRETTY}
     */
    public void serialize(int flags) throws IOException;

    /**
     * Serialize the current set of objects to the files from which they were
     * parsed. The objects must implement the {@link SourceTracker} interface.
     * 
     * @param output if null, then serialize directly to the file system;
     * othwerwise, populate the specified {@link Map} with
     * keys that are the {@link File} instances, and
     * values that are the {@link String} contents of the metadata
     * @param flags bit flags specifying the output flags; e.g.
     * {@link PRETTY #PRETTY}
     */
    public void serialize(Map output, int flags) throws IOException;

    /**
     * Serialize the current set of objects to the given file.
     * 
     * @param flags bit flags specifying the output flags; e.g.
     * {@link PRETTY #PRETTY} | {@link APPEND #APPEND}
     */
    public void serialize(File file, int flags) throws IOException;

    /**
     * Serialize the current set of objects to the given stream.
     * 
     * @param flags bit flags specifying the output flags; e.g.
     * {@link PRETTY #PRETTY}
     */
    public void serialize(Writer out, int flags) throws IOException;
}
