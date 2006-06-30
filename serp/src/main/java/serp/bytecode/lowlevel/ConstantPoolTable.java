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
package serp.bytecode.lowlevel;

import java.io.*;

/**
 * Efficient representation of the constant pool as a table. This class
 * can be used to parse out bits of information from bytecode without
 * instantiating a full {@link serp.bytecode.BCClass}.
 * 
 * @author Abe White
 */
public class ConstantPoolTable {
    private byte[] _bytecode = null;
    private int[] _table = null;
    private int _idx = 0;

    /**
     * Allows static computation of the byte index after the constant
     * pool without caching constant pool information.
     */
    public static int getEndIndex(byte[] b) {
        return parse(b, null);
    }

    /**
     * Parse class bytecode, returning end index of pool.
     */
    private static int parse(byte[] b, int[] table) {
        // each entry is the index in the byte array of the data for a const
        // pool entry
        int entries = (table == null) ? readUnsignedShort(b, 8) : table.length;
        int idx = 10;
        for (int i = 1; i < entries; i++) {
            if (table != null)
                table[i] = idx + 1; // skip entry type
            switch (b[idx]) {
            case 1: // utf8
                idx += 3 + readUnsignedShort(b, idx + 1);
                break;
            case 3: // integer
            case 4: // float
            case 9: // field
            case 10: // method
            case 11: // interface method
            case 12: // name
                idx += 5;
                break;
            case 5: // long
            case 6: // double
                idx += 9;
                i++; // wide entry
                break;
            default:
                idx += 3;
            }
        }
        return idx;
    }

    /**
     * Read a byte value at the given offset into the given bytecode.
     */
    public static int readByte(byte[] b, int idx) {
        return b[idx] & 0xFF;
    }

    /**
     * Read an unsigned short value at the given offset into the given bytecode.
     */
    public static int readUnsignedShort(byte[] b, int idx) {
        return(readByte(b, idx) << 8) | readByte(b, idx + 1);
    }

    /**
     * Read an int value at the given offset into the given bytecode.
     */
    public static int readInt(byte[] b, int idx) {
        return(readByte(b, idx) << 24) | (readByte(b, idx + 1) << 16)
            | (readByte(b, idx + 2) << 8) | readByte(b, idx + 3);
    }

    /**
     * Read a long value at the given offset into the given bytecode.
     */
    public static long readLong(byte[] b, int idx) {
        return(readInt(b, idx) << 32) | readInt(b, idx + 4);
    }

    /**
     * Read a UTF-8 string value at the given offset into the given bytecode.
     */
    public static String readString(byte[] b, int idx) {
        int len = readUnsignedShort(b, idx);
        try {
            return new String(b, idx + 2, len, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new ClassFormatError(uee.toString());
        }
    }

    /**
     * Constructor; supply class bytecode.
     */
    public ConstantPoolTable(byte[] b) {
        _bytecode = b;
        _table = new int[readUnsignedShort(b, 8)];
        _idx = parse(b, _table);
    }

    /**
     * Constructor; supply input stream to bytecode.
     */
    public ConstantPoolTable(InputStream in) throws IOException {
        this(toByteArray(in));
    }

    /**
     * Read the contents of the given stream.
     */
    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int r; (r = in.read(buf)) != -1; bout.write(buf, 0, r));
        return bout.toByteArray();
    }

    /**
     * Return the index into the bytecode of the end of the constant pool.
     */
    public int getEndIndex() {
        return _idx;
    }

    /**
     * Return the given table entry.
     */
    public int get(int idx) {
        return _table[idx];
    }

    /**
     * Read a byte value at the given offset.
     */
    public int readByte(int idx) {
        return readByte(_bytecode, idx);
    }

    /**
     * Read an unsigned short value at the given offset.
     */
    public int readUnsignedShort(int idx) {
        return readUnsignedShort(_bytecode, idx);
    }

    /**
     * Read an int value at the given offset.
     */
    public int readInt(int idx) {
        return readInt(_bytecode, idx);
    }

    /**
     * Read a long value at the given offset.
     */
    public long readLong(int idx) {
        return readLong(_bytecode, idx);
    }

    /**
     * Read a UTF-8 string value at the given offset.
     */
    public String readString(int idx) {
        return readString(_bytecode, idx);
    }
}
