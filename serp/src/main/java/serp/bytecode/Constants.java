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
package serp.bytecode;

/**
 * Interface to track constants used in bytecode. Entities can access these
 * constants using the static <code>Constants.</code> field prefix,
 * or implement this interface themselves to conveniently import the
 * constants into their own namespace.
 * 
 * @author Abe White
 */
public interface Constants {
    // class magic number
    public static final int VALID_MAGIC = 0xcafebabe;

    // standard major, minor versions
    public static final int MAJOR_VERSION = 45;
    public static final int MINOR_VERSION = 3;

    // access constants for classes, fields, methods
    public static final int ACCESS_PUBLIC = 0x0001;
    public static final int ACCESS_PRIVATE = 0x0002;
    public static final int ACCESS_PROTECTED = 0x0004;
    public static final int ACCESS_STATIC = 0x0008;
    public static final int ACCESS_FINAL = 0x0010;
    public static final int ACCESS_SUPER = 0x0020;
    public static final int ACCESS_SYNCHRONIZED = 0x0020;
    public static final int ACCESS_VOLATILE = 0x0040;
    public static final int ACCESS_TRANSIENT = 0x0080;
    public static final int ACCESS_NATIVE = 0x0100;
    public static final int ACCESS_INTERFACE = 0x0200;
    public static final int ACCESS_ABSTRACT = 0x0400;
    public static final int ACCESS_STRICT = 0x0800;

    // attribute types the compiler must support
    public static final String ATTR_CODE = "Code";
    public static final String ATTR_CONST = "ConstantValue";
    public static final String ATTR_DEPRECATED = "Deprecated";
    public static final String ATTR_EXCEPTIONS = "Exceptions";
    public static final String ATTR_INNERCLASS = "InnerClasses";
    public static final String ATTR_LINENUMBERS = "LineNumberTable";
    public static final String ATTR_LOCALS = "LocalVariableTable";
    public static final String ATTR_LOCAL_TYPES = "LocalVariableTypeTable";
    public static final String ATTR_SOURCE = "SourceFile";
    public static final String ATTR_SYNTHETIC = "Synthetic";
    public static final String ATTR_UNKNOWN = "Unknown";

    // opcodes
    public static final int NOP = 0;
    public static final int ACONSTNULL = 1;
    public static final int ICONSTM1 = 2;
    public static final int ICONST0 = 3;
    public static final int ICONST1 = 4;
    public static final int ICONST2 = 5;
    public static final int ICONST3 = 6;
    public static final int ICONST4 = 7;
    public static final int ICONST5 = 8;
    public static final int LCONST0 = 9;
    public static final int LCONST1 = 10;
    public static final int FCONST0 = 11;
    public static final int FCONST1 = 12;
    public static final int FCONST2 = 13;
    public static final int DCONST0 = 14;
    public static final int DCONST1 = 15;
    public static final int BIPUSH = 16;
    public static final int SIPUSH = 17;
    public static final int LDC = 18;
    public static final int LDCW = 19;
    public static final int LDC2W = 20;
    public static final int ILOAD = 21;
    public static final int LLOAD = 22;
    public static final int FLOAD = 23;
    public static final int DLOAD = 24;
    public static final int ALOAD = 25;
    public static final int ILOAD0 = 26;
    public static final int ILOAD1 = 27;
    public static final int ILOAD2 = 28;
    public static final int ILOAD3 = 29;
    public static final int LLOAD0 = 30;
    public static final int LLOAD1 = 31;
    public static final int LLOAD2 = 32;
    public static final int LLOAD3 = 33;
    public static final int FLOAD0 = 34;
    public static final int FLOAD1 = 35;
    public static final int FLOAD2 = 36;
    public static final int FLOAD3 = 37;
    public static final int DLOAD0 = 38;
    public static final int DLOAD1 = 39;
    public static final int DLOAD2 = 40;
    public static final int DLOAD3 = 41;
    public static final int ALOAD0 = 42;
    public static final int ALOAD1 = 43;
    public static final int ALOAD2 = 44;
    public static final int ALOAD3 = 45;
    public static final int IALOAD = 46;
    public static final int LALOAD = 47;
    public static final int FALOAD = 48;
    public static final int DALOAD = 49;
    public static final int AALOAD = 50;
    public static final int BALOAD = 51;
    public static final int CALOAD = 52;
    public static final int SALOAD = 53;
    public static final int ISTORE = 54;
    public static final int LSTORE = 55;
    public static final int FSTORE = 56;
    public static final int DSTORE = 57;
    public static final int ASTORE = 58;
    public static final int ISTORE0 = 59;
    public static final int ISTORE1 = 60;
    public static final int ISTORE2 = 61;
    public static final int ISTORE3 = 62;
    public static final int LSTORE0 = 63;
    public static final int LSTORE1 = 64;
    public static final int LSTORE2 = 65;
    public static final int LSTORE3 = 66;
    public static final int FSTORE0 = 67;
    public static final int FSTORE1 = 68;
    public static final int FSTORE2 = 69;
    public static final int FSTORE3 = 70;
    public static final int DSTORE0 = 71;
    public static final int DSTORE1 = 72;
    public static final int DSTORE2 = 73;
    public static final int DSTORE3 = 74;
    public static final int ASTORE0 = 75;
    public static final int ASTORE1 = 76;
    public static final int ASTORE2 = 77;
    public static final int ASTORE3 = 78;
    public static final int IASTORE = 79;
    public static final int LASTORE = 80;
    public static final int FASTORE = 81;
    public static final int DASTORE = 82;
    public static final int AASTORE = 83;
    public static final int BASTORE = 84;
    public static final int CASTORE = 85;
    public static final int SASTORE = 86;
    public static final int POP = 87;
    public static final int POP2 = 88;
    public static final int DUP = 89;
    public static final int DUPX1 = 90;
    public static final int DUPX2 = 91;
    public static final int DUP2 = 92;
    public static final int DUP2X1 = 93;
    public static final int DUP2X2 = 94;
    public static final int SWAP = 95;
    public static final int IADD = 96;
    public static final int LADD = 97;
    public static final int FADD = 98;
    public static final int DADD = 99;
    public static final int ISUB = 100;
    public static final int LSUB = 101;
    public static final int FSUB = 102;
    public static final int DSUB = 103;
    public static final int IMUL = 104;
    public static final int LMUL = 105;
    public static final int FMUL = 106;
    public static final int DMUL = 107;
    public static final int IDIV = 108;
    public static final int LDIV = 109;
    public static final int FDIV = 110;
    public static final int DDIV = 111;
    public static final int IREM = 112;
    public static final int LREM = 113;
    public static final int FREM = 114;
    public static final int DREM = 115;
    public static final int INEG = 116;
    public static final int LNEG = 117;
    public static final int FNEG = 118;
    public static final int DNEG = 119;
    public static final int ISHL = 120;
    public static final int LSHL = 121;
    public static final int ISHR = 122;
    public static final int LSHR = 123;
    public static final int IUSHR = 124;
    public static final int LUSHR = 125;
    public static final int IAND = 126;
    public static final int LAND = 127;
    public static final int IOR = 128;
    public static final int LOR = 129;
    public static final int IXOR = 130;
    public static final int LXOR = 131;
    public static final int IINC = 132;
    public static final int I2L = 133;
    public static final int I2F = 134;
    public static final int I2D = 135;
    public static final int L2I = 136;
    public static final int L2F = 137;
    public static final int L2D = 138;
    public static final int F2I = 139;
    public static final int F2L = 140;
    public static final int F2D = 141;
    public static final int D2I = 142;
    public static final int D2L = 143;
    public static final int D2F = 144;
    public static final int I2B = 145;
    public static final int I2C = 146;
    public static final int I2S = 147;
    public static final int LCMP = 148;
    public static final int FCMPL = 149;
    public static final int FCMPG = 150;
    public static final int DCMPL = 151;
    public static final int DCMPG = 152;
    public static final int IFEQ = 153;
    public static final int IFNE = 154;
    public static final int IFLT = 155;
    public static final int IFGE = 156;
    public static final int IFGT = 157;
    public static final int IFLE = 158;
    public static final int IFICMPEQ = 159;
    public static final int IFICMPNE = 160;
    public static final int IFICMPLT = 161;
    public static final int IFICMPGE = 162;
    public static final int IFICMPGT = 163;
    public static final int IFICMPLE = 164;
    public static final int IFACMPEQ = 165;
    public static final int IFACMPNE = 166;
    public static final int GOTO = 167;
    public static final int JSR = 168;
    public static final int RET = 169;
    public static final int TABLESWITCH = 170;
    public static final int LOOKUPSWITCH = 171;
    public static final int IRETURN = 172;
    public static final int LRETURN = 173;
    public static final int FRETURN = 174;
    public static final int DRETURN = 175;
    public static final int ARETURN = 176;
    public static final int RETURN = 177;
    public static final int GETSTATIC = 178;
    public static final int PUTSTATIC = 179;
    public static final int GETFIELD = 180;
    public static final int PUTFIELD = 181;
    public static final int INVOKEVIRTUAL = 182;
    public static final int INVOKESPECIAL = 183;
    public static final int INVOKESTATIC = 184;
    public static final int INVOKEINTERFACE = 185;
    public static final int NEW = 187;
    public static final int NEWARRAY = 188;
    public static final int ANEWARRAY = 189;
    public static final int ARRAYLENGTH = 190;
    public static final int ATHROW = 191;
    public static final int CHECKCAST = 192;
    public static final int INSTANCEOF = 193;
    public static final int MONITORENTER = 194;
    public static final int MONITOREXIT = 195;
    public static final int WIDE = 196;
    public static final int MULTIANEWARRAY = 197;
    public static final int IFNULL = 198;
    public static final int IFNONNULL = 199;
    public static final int GOTOW = 200;
    public static final int JSRW = 201;

    // array types
    public static final int ARRAY_BOOLEAN = 4;
    public static final int ARRAY_CHAR = 5;
    public static final int ARRAY_FLOAT = 6;
    public static final int ARRAY_DOUBLE = 7;
    public static final int ARRAY_BYTE = 8;
    public static final int ARRAY_SHORT = 9;
    public static final int ARRAY_INT = 10;
    public static final int ARRAY_LONG = 11;

    // math operations
    public static final int MATH_ADD = IADD;
    public static final int MATH_SUB = ISUB;
    public static final int MATH_MUL = IMUL;
    public static final int MATH_DIV = IDIV;
    public static final int MATH_REM = IREM;
    public static final int MATH_NEG = INEG;
    public static final int MATH_SHL = ISHL;
    public static final int MATH_SHR = ISHR;
    public static final int MATH_USHR = IUSHR;
    public static final int MATH_AND = IAND;
    public static final int MATH_OR = IOR;
    public static final int MATH_XOR = IXOR;

    // human-readable opcode names
    public static final String[] OPCODE_NAMES = new String[] {
        "nop", "aconstnull", "iconstm1", "iconst0", "iconst1", "iconst2",
        "iconst3", "iconst4", "iconst5", "lconst0", "lconst1", "fconst0",
        "fconst1", "fconst2", "dconst0", "dconst1", "bipush", "sipush", "ldc",
        "ldcw", "ldc2w", "iload", "lload", "fload", "dload", "aload", "iload0",
        "iload1", "iload2", "iload3", "lload0", "lload1", "lload2", "lload3",
        "fload0", "fload1", "fload2", "fload3", "dload0", "dload1", "dload2",
        "dload3", "aload0", "aload1", "aload2", "aload3", "iaload", "laload",
        "faload", "daload", "aaload", "baload", "caload", "saload", "istore",
        "lstore", "fstore", "dstore", "astore", "istore0", "istore1", "istore2",
        "istore3", "lstore0", "lstore1", "lstore2", "lstore3", "fstore0",
        "fstore1", "fstore2", "fstore3", "dstore0", "dstore1", "dstore2",
        "dstore3", "astore0", "astore1", "astore2", "astore3", "iastore",
        "lastore", "fastore", "dastore", "aastore", "bastore", "castore",
        "sastore", "pop", "pop2", "dup", "dupx1", "dupx2", "dup2", "dup2x1",
        "dup2x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub",
        "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv",
        "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg",
        "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior",
        "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d",
        "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp",
        "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge",
        "ifgt", "ifle", "ificmpeq", "ificmpne", "ificmplt", "ificmpge",
        "ificmpgt", "ificmple", "ifacmpeq", "ifacmpne", "goto", "jsr", "ret",
        "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn",
        "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield",
        "putfield", "invokevirtual", "invokespecial", "invokestatic",
        "invokeinterface", "??", "new", "newarray", "anewarray", "arraylength",
        "athrow", "checkcast", "instanceof", "monitorenter", "monitorexit",
        "wide", "multianewarray", "ifnull", "ifnonnull", "gotow", "jsrw", };
}
