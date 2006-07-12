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
package serp.bytecode.visitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import serp.bytecode.Attribute;
import serp.bytecode.BCClass;
import serp.bytecode.BCField;
import serp.bytecode.BCMethod;
import serp.bytecode.ClassInstruction;
import serp.bytecode.Code;
import serp.bytecode.ConstantInstruction;
import serp.bytecode.ConstantValue;
import serp.bytecode.Constants;
import serp.bytecode.ExceptionHandler;
import serp.bytecode.Exceptions;
import serp.bytecode.GetFieldInstruction;
import serp.bytecode.IIncInstruction;
import serp.bytecode.IfInstruction;
import serp.bytecode.InnerClass;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.LineNumber;
import serp.bytecode.LoadInstruction;
import serp.bytecode.LocalVariable;
import serp.bytecode.LocalVariableType;
import serp.bytecode.LookupSwitchInstruction;
import serp.bytecode.MethodInstruction;
import serp.bytecode.MultiANewArrayInstruction;
import serp.bytecode.NewArrayInstruction;
import serp.bytecode.Project;
import serp.bytecode.PutFieldInstruction;
import serp.bytecode.RetInstruction;
import serp.bytecode.SourceFile;
import serp.bytecode.StoreInstruction;
import serp.bytecode.TableSwitchInstruction;
import serp.bytecode.WideInstruction;
import serp.bytecode.lowlevel.ClassEntry;
import serp.bytecode.lowlevel.ConstantPool;
import serp.bytecode.lowlevel.DoubleEntry;
import serp.bytecode.lowlevel.Entry;
import serp.bytecode.lowlevel.FieldEntry;
import serp.bytecode.lowlevel.FloatEntry;
import serp.bytecode.lowlevel.IntEntry;
import serp.bytecode.lowlevel.InterfaceMethodEntry;
import serp.bytecode.lowlevel.LongEntry;
import serp.bytecode.lowlevel.MethodEntry;
import serp.bytecode.lowlevel.NameAndTypeEntry;
import serp.bytecode.lowlevel.StringEntry;
import serp.bytecode.lowlevel.UTF8Entry;

/**
 * Visitor type that outputs a detailed, formatted document of the
 * visited entity; similar to the <i>javap -c</i> command but more detailed.
 *
 * @author Abe White
 */
public class PrettyPrintVisitor extends BCVisitor {

    private PrintWriter _out = null;
    private String _prefix = "";
    private int _entryCount = 0;

    /**
     * Invoke with the class or file names to pretty print; the
     * functionality is similar to the <i>javap -c</i> command, but more
     * detailed.
     */
    public static void main(String[] args)
        throws ClassNotFoundException, IOException {
        if (args.length == 0) {
            System.err.println("Usage: java "
                + PrettyPrintVisitor.class.getName()
                + " <class name | .class file>+");
            System.exit(1);
        }

        PrettyPrintVisitor ppv = new PrettyPrintVisitor();
        Project project = new Project();
        BCClass type;
        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".class"))
                type = project.loadClass(new File(args[i]));
            else
                type = project.loadClass(Class.forName(args[i], false,
                    PrettyPrintVisitor.class.getClassLoader()));
            ppv.visit(type);
        }
    }

    /**
     * Constructor; all pritning will go to stdout.
     */
    public PrettyPrintVisitor() {
        _out = new PrintWriter(System.out);
    }

    /**
     * Constructor.
     *
     * @param out the stream to print to
     */
    public PrettyPrintVisitor(PrintWriter out) {
        _out = out;
    }

    public void visit(VisitAcceptor entity) {
        super.visit(entity);
        _out.flush();
    }

    public void enterProject(Project obj) {
        openBlock("Project");

        println("name=" + obj.getName());
    }

    public void exitProject(Project obj) {
        closeBlock();
    }

    public void enterBCClass(BCClass obj) {
        openBlock("Class");

        println("magic=" + obj.getMagic());
        println("minor=" + obj.getMinorVersion());
        println("major=" + obj.getMajorVersion());
        println("access=" + obj.getAccessFlags());
        println("name=" + obj.getIndex() + " <" + obj.getName() + ">");
        println("super=" + obj.getSuperclassIndex()
            + " <" + obj.getSuperclassName() + ">");

        int[] indexes = obj.getDeclaredInterfaceIndexes();
        String[] names = obj.getDeclaredInterfaceNames();
        for (int i = 0; i < indexes.length; i++)
            println("interface=" + indexes[i] + " <" + names[i] + ">");
    }

    public void exitBCClass(BCClass obj) {
        closeBlock();
    }

    public void enterBCField(BCField obj) {
        openBlock("Field");
        println("access=" + obj.getAccessFlags());
        println("name=" + obj.getNameIndex() + " <" + obj.getName() + ">");
        println("type=" + obj.getDescriptorIndex()
            + " <" + obj.getTypeName() + ">");
    }

    public void exitBCField(BCField obj) {
        closeBlock();
    }

    public void enterBCMethod(BCMethod obj) {
        openBlock("Method");
        println("access=" + obj.getAccessFlags());
        println("name=" + obj.getNameIndex() + " <" + obj.getName() + ">");
        println("descriptor=" + obj.getDescriptorIndex());
        println("return=" + obj.getReturnName());
        String[] params = obj.getParamNames();
        for (int i = 0; i < params.length; i++)
            println("param=" + params[i]);
    }

    public void exitBCMethod(BCMethod obj) {
        closeBlock();
    }

    public void enterAttribute(Attribute obj) {
        openBlock(obj.getName());
    }

    public void exitAttribute(Attribute obj) {
        closeBlock();
    }

    public void enterConstantValue(ConstantValue obj) {
        println("value=" + obj.getValueIndex() + " <" + obj.getTypeName()
            + "=" + obj.getValue() + ">");
    }

    public void enterExceptions(Exceptions obj) {
        int[] indexes = obj.getExceptionIndexes();
        String[] names = obj.getExceptionNames();
        for (int i = 0; i < indexes.length; i++)
            println("exception=" + indexes[i] + " <" + names[i] + ">");
    }

    public void enterSourceFile(SourceFile obj) {
        println("source=" + obj.getFileIndex() + " <"
            + obj.getFileName() + ">");
    }

    public void enterCode(Code obj) {
        println("maxStack=" + obj.getMaxStack());
        println("maxLocals=" + obj.getMaxLocals());
        println("");
    }

    public void enterExceptionHandler(ExceptionHandler obj) {
        openBlock("ExceptionHandler");
        println("startPc=" + obj.getTryStartPc());
        println("endPc=" + obj.getTryEndPc());
        println("handlerPc=" + obj.getHandlerStartPc());
        println("catch=" + obj.getCatchIndex() + " <"
            + obj.getCatchName() + ">");
    }

    public void exitExceptionHandler(ExceptionHandler obj) {
        closeBlock();
    }

    public void enterInnerClass(InnerClass obj) {
        openBlock("InnerClass");
        println("access=" + obj.getAccessFlags());
        println("name=" + obj.getNameIndex() + " <" + obj.getName() + ">");
        println("type=" + obj.getTypeIndex() + "<" + obj.getTypeName() + ">");
        println("declarer=" + obj.getDeclarerIndex() + "<"
            + obj.getDeclarerName() + ">");
    }

    public void exitInnerClass(InnerClass obj) {
        closeBlock();
    }

    public void enterLineNumber(LineNumber obj) {
        openBlock("LineNumber");
        println("startPc=" + obj.getStartPc());
        println("line=" + obj.getLine());
    }

    public void exitLineNumber(LineNumber obj) {
        closeBlock();
    }

    public void enterLocalVariable(LocalVariable obj) {
        openBlock("LocalVariable");
        println("startPc=" + obj.getStartPc());
        println("length=" + obj.getLength());
        println("local=" + obj.getLocal());
        println("name=" + obj.getNameIndex() + " <" + obj.getName() + ">");
        println("type=" + obj.getTypeIndex() + " <" + obj.getTypeName() + ">");
    }

    public void exitLocalVariable(LocalVariable obj) {
        closeBlock();
    }

    public void enterLocalVariableType(LocalVariableType obj) {
        openBlock("LocalVariableType");
        println("startPc=" + obj.getStartPc());
        println("length=" + obj.getLength());
        println("local=" + obj.getLocal());
        println("name=" + obj.getNameIndex() + " <" + obj.getName() + ">");
        println("signature=" + obj.getTypeIndex() + " <"
            + obj.getTypeName() + ">");
    }

    public void exitLocalVariableType(LocalVariableType obj) {
        closeBlock();
    }

    public void enterInstruction(Instruction obj) {
        _out.print(_prefix + obj.getByteIndex() + " " + obj.getName() + " ");
    }

    public void exitInstruction(Instruction obj) {
        _out.println();
    }

    public void enterClassInstruction(ClassInstruction obj) {
        _out.print(obj.getTypeIndex() + " <" + obj.getTypeName() + ">");
    }

    public void enterConstantInstruction(ConstantInstruction obj) {
        _out.print("<" + obj.getValue() + ">");
    }

    public void enterGetFieldInstruction(GetFieldInstruction obj) {
        _out.print(obj.getFieldIndex() + " <" + obj.getFieldTypeName()
            + " " + obj.getFieldDeclarerName()
            + "." + obj.getFieldName() + ">");
    }

    public void enterIIncInstruction(IIncInstruction obj) {
        _out.print(obj.getLocal() + " ");
        if (obj.getIncrement() < 0)
            _out.print("-");
        _out.print(obj.getIncrement());
    }

    public void enterJumpInstruction(JumpInstruction obj) {
        _out.print(obj.getOffset());
    }

    public void enterIfInstruction(IfInstruction obj) {
        _out.print(obj.getOffset());
    }

    public void enterLoadInstruction(LoadInstruction obj) {
        _out.print("<" + obj.getLocal() + ">");
    }

    public void enterLookupSwitchInstruction(LookupSwitchInstruction obj) {
        _out.println();
        _prefix += "  ";

        int[] offsets = obj.getOffsets();
        int[] matches = obj.getMatches();
        for (int i = 0; i < offsets.length; i++)
            println("case " + matches[i] + "=" + offsets[i]);
        _out.print(_prefix + "default=" + obj.getDefaultOffset());

        _prefix = _prefix.substring(2);
    }

    public void enterMethodInstruction(MethodInstruction obj) {
        _out.print(obj.getMethodIndex() + " <" + obj.getMethodReturnName()
            + " " + obj.getMethodDeclarerName()
            + "." + obj.getMethodName() + "(");
        String[] params = obj.getMethodParamNames();
        int dotIndex;
        for (int i = 0; i < params.length; i++) {
            dotIndex = params[i].lastIndexOf('.');
            if (dotIndex != -1)
                params[i] = params[i].substring(dotIndex + 1);

            _out.print(params[i]);
            if (i != params.length - 1)
                _out.print(", ");
        }

        _out.print(")>");
    }

    public void enterMultiANewArrayInstruction(MultiANewArrayInstruction obj) {
        _out.print(obj.getTypeIndex() + " " + obj.getDimensions()
            + " <" + obj.getTypeName());
        String post = "";
        for (int i = 0; i < obj.getDimensions(); i++)
            post += "[]";
        _out.print(post + ">");
    }

    public void enterNewArrayInstruction(NewArrayInstruction obj) {
        _out.print(obj.getTypeCode() + " <" + obj.getTypeName() + "[]>");
    }

    public void enterPutFieldInstruction(PutFieldInstruction obj) {
        _out.print(obj.getFieldIndex() + " <" + obj.getFieldTypeName()
            + " " + obj.getFieldDeclarerName()
            + "." + obj.getFieldName() + ">");
    }

    public void enterRetInstruction(RetInstruction obj) {
        _out.print(obj.getLocal());
    }

    public void enterStoreInstruction(StoreInstruction obj) {
        _out.print("<" + obj.getLocal() + ">");
    }

    public void enterTableSwitchInstruction(TableSwitchInstruction obj) {
        _out.println();
        _prefix += "  ";

        println("low=" + obj.getLow());
        println("high=" + obj.getHigh());
        int[] offsets = obj.getOffsets();
        for (int i = 0; i < offsets.length; i++)
            println("case=" + offsets[i]);
        _out.print(_prefix + "default=" + obj.getDefaultOffset());

        _prefix = _prefix.substring(2);
    }

    public void enterWideInstruction(WideInstruction obj) {
        int ins = obj.getInstruction();
        _out.print(ins + " <" + Constants.OPCODE_NAMES[ins] + ">");
    }

    public void enterConstantPool(ConstantPool obj) {
        _entryCount = 0;
        openBlock("ConstantPool");
    }

    public void exitConstantPool(ConstantPool obj) {
        closeBlock();
    }

    public void enterEntry(Entry obj) {
        String name = obj.getClass().getName();
        openBlock(++_entryCount + ": "
            + name.substring(name.lastIndexOf('.') + 1));
    }

    public void exitEntry(Entry obj) {
        closeBlock();
    }

    public void enterClassEntry(ClassEntry obj) {
        println("name=" + obj.getNameIndex());
    }

    public void enterDoubleEntry(DoubleEntry obj) {
        println("value=" + obj.getValue());
    }

    public void enterFieldEntry(FieldEntry obj) {
        println("class=" + obj.getClassIndex());
        println("nameAndType=" + obj.getNameAndTypeIndex());
    }

    public void enterFloatEntry(FloatEntry obj) {
        println("value=" + obj.getValue());
    }

    public void enterIntEntry(IntEntry obj) {
        println("value=" + obj.getValue());
    }

    public void enterInterfaceMethodEntry(InterfaceMethodEntry obj) {
        println("class=" + obj.getClassIndex());
        println("nameAndType=" + obj.getNameAndTypeIndex());
    }

    public void enterLongEntry(LongEntry obj) {
        println("value=" + obj.getValue());
    }

    public void enterMethodEntry(MethodEntry obj) {
        println("class=" + obj.getClassIndex());
        println("nameAndType=" + obj.getNameAndTypeIndex());
    }

    public void enterNameAndTypeEntry(NameAndTypeEntry obj) {
        println("name=" + obj.getNameIndex());
        println("descriptor=" + obj.getDescriptorIndex());
    }

    public void enterStringEntry(StringEntry obj) {
        println("index=" + obj.getStringIndex());
    }

    public void enterUTF8Entry(UTF8Entry obj) {
        println("value=" + obj.getValue());
    }

    private void println(String ln) {
        _out.print(_prefix);
        _out.println(ln);
    }

    private void openBlock(String name) {
        println(name + " {");
        _prefix += "  ";
    }

    private void closeBlock() {
        _prefix = _prefix.substring(2);
        println("}");
    }
}
