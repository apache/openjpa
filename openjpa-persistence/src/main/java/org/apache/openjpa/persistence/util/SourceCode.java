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

package org.apache.openjpa.persistence.util;

import java.io.PrintWriter;
import java.util.*;

import org.apache.openjpa.lib.util.Localizer;


/**
 * A utility to help writing Java Source code dynamically.
 * 
 * Provides basic elements of Java Source Code e.g. Package, Class, Field, 
 * Method, Import, Annotation, Argument.
 * 
 * Mutator methods return the operating element for easy chaining. 
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
public class SourceCode {
	private static Localizer _loc = Localizer.forPackage(SourceCode.class);
	
	/**
	 * List of Java Keywords and primitive types. Populated statically.
	 */
    private static final ArrayList<String> reserved = new ArrayList<String>();
    private static final ArrayList<String> knownTypes = new ArrayList<String>();
	
	private static int TABSIZE                = 4;
	private static final String SPACE         = " ";
	private static final String BLANK         = "";
	private static final String SEMICOLON     = ";";
    public static final String  COMMA         = ",";
    public static final String  DOT           = ".";
    public static final String  EQUAL         = "=";
    public static final String  QUOTE         = "\"";
	private static final String[] BRACKET_BLOCK  = {"{", "}"};
	private static final String[] BRACKET_ARGS   = {"(", ")"};
    private static final String[] BRACKET_PARAMS = {"<", ">"};
	
	private Comment comment;
	private final Package pkg;
	private final Class   cls;
    private final Set<Import> imports = new TreeSet<Import>();
	
	
	/**
	 * Create source code for a top-level class with given fully-qualified 
	 * class name. 
	 */
	public SourceCode(String c) {
	    ClassName name = new ClassName(c);
	    this.cls = new Class(c);
        this.pkg = new Package(name.getPackageName());
	}
	
	/**
	 * Gets the top level class represented by this receiver.
	 */
	public Class getTopLevelClass() {
		return cls;
	}
	
	public Package getPackage() {
	    return pkg;
	}
	
    /**
     * Sets the tab size. Tabs are always written as spaces.
     */
    public SourceCode setTabSize(int t) {
        if (t>0) TABSIZE = Math.max(t, 8);
        return this;
    }

	boolean addImport(ClassName name) {
		String pkgName = name.getPackageName();
		if ("java.lang".equals(pkgName))
			return false;
		return imports.add(new Import(name));
	}
	
	
	public SourceCode addComment(boolean inline, String... lines) {
		if (comment == null) comment = new Comment();
		comment.makeInline(inline);
		for (String line:lines) comment.append(line);
		return this;
	}
	
	/**
	 * Prints the class to the given Writer.
	 * @param out
	 */
	public void write(PrintWriter out) {
		if (comment != null) {
		    comment.write(out, 0);
		      out.println();
		}
		if (pkg != null) {
		    pkg.write(out,0);
		    out.println();
		}
		for (Import imp:imports) {
			imp.write(out, 0);
		}
		out.println();
		cls.write(out, 0);
		out.flush();
	}
	
	/**
	 * Outputs <code>tab</code> number of spaces.
	 */
	static void tab(PrintWriter out, int tab) {
		for (int i=0; i<tab*TABSIZE; i++) {
			out.print(SPACE);
		}
	}
	
    static void writeList(PrintWriter out, String header, List<?> list) { 
        writeList(out, header, list, new String[]{BLANK, BLANK}, false);
    }
	
	static void writeList(PrintWriter out, String header, List<?> list, 
			String[] bracket, boolean writeEmpty) {
		if (list == null || list.isEmpty()) {
		    if (writeEmpty)
		        out.append(bracket[0]+bracket[1]);
			return;
		}
		out.append(header);
		out.append(bracket[0]);
		for (int i=0; i<list.size(); i++) {
			out.append(list.get(i).toString());
			if (i!=list.size()-1) out.append(COMMA);
		}
		out.append(bracket[1]);
	}
	
	static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0))+s.substring(1);
	}
	
	static boolean isValidToken(String s) {
		return s != null && s.length() > 0 && 
		      !reserved.contains(s) && isJavaIdentifier(s);
	}
	
	public static boolean isKnownType(String s) {
		return knownTypes.contains(s);
	}
	
	static boolean isEmpty(String s) {
		return s == null || s.length()==0;
	}
	
	static LinkedList<String> tokenize(String s, String delim) {
	    StringTokenizer tokenizer = new StringTokenizer(s, delim, false);
		LinkedList<String> tokens = new LinkedList<String>();
		while (tokenizer.hasMoreTokens())
			tokens.add(tokenizer.nextToken());
		return tokens;
	}
	
	public static boolean isJavaIdentifier(String s) {
        if (s == null || s.length() == 0 || 
        	!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i=1; i<s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
	
	
    public enum ACCESS {PUBLIC, PROTECTED, PRIVATE}
		
	/**
	 * Abstract element has a name, optional list of modifiers, annotations
	 * and arguments. 
	 */
	public abstract class Element<T> implements Comparable<Element<T>> {
		protected String name;
		protected ClassName type;
		protected ACCESS access;
		protected boolean isStatic;
		protected boolean isFinal;
		protected Comment comment;
		protected List<ClassName> params = new ArrayList<ClassName>();
		protected List<Annotation> annos = new ArrayList<Annotation>();
		
        protected Element(String name, ClassName type) {
            this.name = name;
            this.type = type;
        }

        public ClassName getType() {
			return type;
		}
				
		public Annotation addAnnotation(String a) {
			Annotation an = new Annotation(a);
			annos.add(an);
			return an;
		}
		
		public Element<T> addParameter(String param) {
		    params.add(new ClassName(param));
		    return this;
		}
		
		public int compareTo(Element<T> other) {
			return name.compareTo(other.name);
		}
		
		public T addComment(boolean inline, String... lines) {
			if (comment == null) comment = new Comment();
			comment.makeInline(inline);
			for (String line:lines) comment.append(line);
			return (T)this;
		}
		
		public T makePublic() {
			access = ACCESS.PUBLIC;
			return (T)this;
		}
		
		public T makeProtected() {
			access = ACCESS.PROTECTED;
			return (T)this;
		}
		
		public T makePrivate() {
			access = ACCESS.PRIVATE;
			return (T)this;
		}
		
		public T makeStatic() {
			isStatic = true;
			return (T)this;
		}
		
		public T makeFinal() {
			isFinal = true;
			return (T)this;
		}
		
		public void write(PrintWriter out, int tab) {
			if (comment != null) comment.write(out, tab);
			for (Annotation a:annos)
				a.write(out, tab);
			tab(out, tab);
			if (access != null) 
			    out.append(access.toString().toLowerCase() + SPACE);
			if (isStatic) 
			    out.append("static" + SPACE);
			if (isFinal) 
			    out.append("final" + SPACE);
		}
	}

	/**
	 * Represent <code>class</code> declaration.
	 *
	 */
	public class Class extends Element<Class> {
		private boolean isAbstract;
		private ClassName superCls;
		private List<ClassName> interfaces = new ArrayList<ClassName>();
	    private Set<Field> fields   = new TreeSet<Field>();
	    private Set<Method> methods = new TreeSet<Method>();
		
		public Class(String name) {
			super(name, new ClassName(name));
			makePublic();
		}
		
		public Class setSuper(String s) {
			superCls = new ClassName(s);
			return this;
		}
		
		public Class addInterface(String s) {
			interfaces.add(new ClassName(s));
			return this;
		}
		
		public Class makeAbstract() {
			isAbstract = true;
			return this;
		}
		
	    /**
	     * Adds getters and setters to every non-public field.
	     */
	    public Class markAsBean() {
	        for (Field f:fields)
	            f.markAsBean();
	        return this;
	    }

        public String getName() {
            return getType().getSimpleName();
        }
        
        public String getPackageName() {
            return getType().getPackageName();
        }
        
        public Field addField(String name, String type) {
            return addField(name, new ClassName(type));
        }

        public Field addField(String f, ClassName type) {
	        if (!isValidToken(f)) {
	            throw new IllegalArgumentException(
	                _loc.get("src-invalid-field",f).toString());
	        }
	        Field field = new Field(this, f, type);
	        
	        if (!fields.add(field))
	            throw new IllegalArgumentException(_loc.get(
	                "src-duplicate-field", field, this).toString());
	        return field;
	    }

        public Method addMethod(String m, String retType) {
            return addMethod(m, new ClassName(retType));
        }
        
	    protected Method addMethod(String m, ClassName retType) {
	        if (isEmpty(m) || !isValidToken(m)) {
	            throw new IllegalArgumentException(_loc.get(
	                "src-invalid-method",m).toString());
	        }
	        Method method = new Method(this, m, retType);
	        if (!methods.add(method)) 
	            throw new IllegalArgumentException(_loc.get(
	                "src-duplicate-method", method, this).toString());
	        return method;
	    }

	    public void write(PrintWriter out, int tab) {
			super.write(out, tab);
			if (isAbstract) 
			    out.append("abstract ");
			out.print("class ");
			out.print(type.simpleName);
			writeList(out, BLANK, params, BRACKET_PARAMS, false);
			if (superCls != null)
				out.print(" extends " + superCls + SPACE);
			writeList(out, "implements ", interfaces);
			out.println(SPACE + BRACKET_BLOCK[0]);
	        for (Field field:fields) 
	            field.write(out, 1);
	        for (Method method:methods) 
	            method.write(out, 1);
	        out.println(BRACKET_BLOCK[1]);
		}
	    
	    public String toString() {
	    	return getType().fullName;
	    }
	}

	/**
	 * Represents field declaration.
	 *
	 */
	public class Field extends Element<Field> {
	    private final Class owner;
		protected boolean isTransient;
		protected boolean isVolatile;
		
		Field(Class owner, String name, ClassName type) {
			super(name, type);
			this.owner = owner;
			makePrivate();
		}
		
		/**
		 * Adds bean-style getter setter method.
		 */
		public Field markAsBean() {
			addGetter();
			addSetter();
			return this;
		}
		
		public Field addGetter() {
			owner.addMethod("get"+ capitalize(name), type)
			     .makePublic()
			     .addCodeLine("return "+ name);
			return this;
		}
		
		public Field addSetter() {
			owner.addMethod("set"+ capitalize(name), "void")
			     .makePublic()
                 .addArgument(new Argument<ClassName,String>(type, name,SPACE))
			     .addCodeLine("this."+ name + " = " + name);
			return this;
		}
		
        public void makeVolatile() {
            isVolatile = true; 
        }
        
        public void makeTransient() {
            isTransient = true; 
        }
		
		public String toString() {
			return type + SPACE + name;
		}
		
		public void write(PrintWriter out, int tab) {
			super.write(out, tab);
			if (isVolatile) out.print("volatile ");
			if (isTransient) out.print("transient ");
			out.print(type);
			writeList(out, BLANK, params, BRACKET_PARAMS, false);
			out.println(SPACE + name + SEMICOLON);
		}
		
		public boolean equals(Object other) {
			if (other instanceof Field) {
				Field that = (Field)other;
				return name.equals(that.name);
			}
			return false;
		}
	}
	
	/**
	 * Represents Method declaration.
	 * 
	 *
	 */
	class Method  extends Element<Method> {
	    private final Class owner;
		private boolean isAbstract;
		private List<Argument<ClassName,String>> args = 
		    new ArrayList<Argument<ClassName,String>>();
		private List<String> codeLines = new ArrayList<String>();
		
        Method(Class owner, String n, String t) {
            this(owner, n, new ClassName(t));
        }
        
        public Method(Class owner, String name, ClassName returnType) {
            super(name, returnType);
            this.owner = owner;
            makePublic();
        }
		
		public Method addArgument(Argument<ClassName,String> arg) {
			args.add(arg);
			return this;
		}
		
		public Method addCodeLine(String line) {
			if (isAbstract)
                throw new IllegalStateException("abstract method " + name 
				    + " can not have body");
			if (!line.endsWith(SEMICOLON))
			    line = line + SEMICOLON;
			codeLines.add(line);
			return this;
		}
		
		public Method makeAbstract() {
			if (codeLines.isEmpty())
				isAbstract = true;
			else
                throw new IllegalStateException("method " + name + 
				    " can not be abstract. It has a body");
			return this;
		}
		
		
		public String toString() {
			return type + SPACE + name;
		}
		
		public void write(PrintWriter out, int tab) {
			out.println(BLANK);
			super.write(out, tab);
			if (isAbstract) out.append("abstract ");
			out.print(type + SPACE + name);
			writeList(out, BLANK, args, BRACKET_ARGS, true);
			if (isAbstract) {
				out.println(SEMICOLON);
				return;
			}
			out.println(SPACE + BRACKET_BLOCK[0]);
			for (String line : codeLines) {
				tab(out, tab+1);
				out.println(line);
			}
			tab(out, tab);
			out.println(BRACKET_BLOCK[1]);
		}
		
		public boolean equals(Object other) {
			if (other instanceof Method) {
				Method that = (Method)other;
                return name.equals(that.name) && args.equals(that.args);
			}
			return false;
		}
	}
	
	/**
	 * Represents <code>import</code> statement.
	 *
	 */
	class Import implements Comparable<Import> {
		private final ClassName name;
		
		public Import(ClassName name) {
			this.name = name;
		}
		
		public int compareTo(Import other) {
			return name.compareTo(other.name);
		}
		
		public void write(PrintWriter out, int tab) {
		    String pkg = name.getPackageName();
		    if (pkg.length() == 0 || pkg.equals(getPackage().name))
		        return;
		    out.println("import "+ name.getName() + SEMICOLON);
		}
		
		public boolean equals(Object other) {
			if (other instanceof Import) {
				Import that = (Import)other;
				return name.equals(that.name);
			}
			return false;
		}
	}
	
	/**
	 * Represents method argument.
	 *
	 */
	public class Argument<K,V> {
		final private K key;
		final private V value;
		final private String connector;
		
		Argument(K key, V value, String connector) {
			this.key = key;
			this.value = value;
			this.connector = connector;
		}
		
        public String toString() {
			return key + connector + value;
		}
	}
	
	/**
	 * Represents annotation.
	 *
	 */
	public class Annotation {
		private String name;
        private List<Argument<?,?>> args = new ArrayList<Argument<?,?>>();
		
		Annotation(String n) {
			name = n;
		}
		
        public Annotation addArgument(String key, String v, boolean quote) {
            return addArgument(new Argument<String,String>(key, 
                quote ? quote(v) : v, EQUAL));
        }
        
        public Annotation addArgument(String key, String v) {
            return addArgument(key, v, true);
        }
        
        public Annotation addArgument(String key, String[] vs) {
            StringBuffer tmp = new StringBuffer(BRACKET_BLOCK[0]);
            for (int i=0; i < vs.length; i++) {
                tmp.append(quote(vs[i]));
                tmp.append(i != vs.length-1 ? COMMA : BLANK);
            }
            tmp.append(BRACKET_BLOCK[1]);
            return addArgument(key, tmp.toString(), false);
        }
        
        public <K,V> Annotation addArgument(Argument<K,V> arg) {
            args.add(arg);
            return this;
        }
		
		public void write(PrintWriter out, int tab) {
			tab(out, tab);
			out.print("@"+name);
			writeList(out, BLANK, args, BRACKET_ARGS, false);
			out.println();
		}
		
		String quote(String s) {
		    return QUOTE + s + QUOTE;
		}
	}
	
	static class Package {
		private String name;
		
		Package(String p) {
			name = p;
		}
		
		public void write(PrintWriter out, int tab) {
			out.println("package " + name + SEMICOLON);
		}
	}
	
	class Comment {
		List<String> lines = new ArrayList<String>();
		private boolean inline = false;
		
		public void append(String line) {
			lines.add(line);
		}
		
		boolean isEmpty() {
			return lines.isEmpty();
		}
		
		void makeInline(boolean flag) {
			inline = flag;
		}
		public void write(PrintWriter out, int tab) {
			if (inline) {
				for (String l:lines) {
					tab(out, tab);
					out.println("// " + l);
				}
			} else {
				int i = 0;
				for (String l:lines) {
					tab(out, tab);
					if (i == 0) {
						out.println("/** ");
						tab(out, tab);
					} 
					out.println(" *  " + l);
					i++;
				}
				tab(out, tab);
				out.println("**/");
			}
		}
	}
	
	/**
	 * Represents fully-qualified name of a Java type.
	 * 
	 * Constructing a name adds it to the list of imports for the enclosing
	 * SourceCode.
	 *
	 */
	class ClassName implements Comparable<ClassName> {
        public final String fullName;
        public final String simpleName;
        public final String pkgName;
        private String  arrayMarker = BLANK;
        
	    ClassName(String name) {
	    	while (isArray(name)) {
	    		arrayMarker = arrayMarker + "[]"; 
	    		name = getComponentName(name);
	    	}
	        this.fullName = name;
	        int dot = fullName.lastIndexOf(DOT);
	        simpleName = (dot == -1) ? fullName : fullName.substring(dot+1);
	        pkgName = (dot == -1) ? BLANK : fullName.substring(0,dot);
            if (!isValidTypeName(name)) {
                throw new IllegalArgumentException(_loc.get("src-invalid-type", 
                    name).toString());
            }
            addImport(this);
	    }
	    
	    /**
	     * Gets fully qualified name of this receiver.
	     */
	    public String getName() {
	        return fullName + arrayMarker;
	    }
	    
        /**
         * Gets simple name of this receiver.
         */
	    public String getSimpleName() {
	        return simpleName + arrayMarker;
	    }
	    
	    /**
	     * Gets the package name of this receiver. Default package name is 
	     * represented as empty string.
	     */
	    public String getPackageName() {
	        return pkgName;
	    }
	    
	    /**
	     * Gets the simple name of this receiver.
	     */
	    public String toString() {
	        return getSimpleName();
	    }
	    
	    /**
	     * Compares by fully-qualified name.
	     */
	    public int compareTo(ClassName other) {
	        return fullName.compareTo(other.fullName);
	    }
	    
	    public boolean isValidTypeName(String s) {
	        return isValidPackageName(pkgName) 
	            && (isKnownType(s) || isValidToken(simpleName));
	    }
	    
	    boolean isValidPackageName(String s) {
	        if (isEmpty(s)) return true;
	        LinkedList<String> tokens = tokenize(s, DOT);
	        for (String token : tokens) {
	            if (!isValidToken(token))
	                return false;
	        }
	        return !s.endsWith(DOT);
	    }
	    
	    boolean isArray(String name) {
	    	return name.endsWith("[]");
	    }
	    
	    String getComponentName(String name) {
	    	return (!isArray(name)) ? name : 
	    		name.substring(0, name.length()-"[]".length());
	    }
	    
	}
	
	static {
		reserved.add("abstract");
		reserved.add("continue");
		reserved.add("for");
		reserved.add("new");
		reserved.add("switch");
		reserved.add("assert");
		reserved.add("default"); 	
		reserved.add("goto");
		reserved.add("package");
		reserved.add("synchronized");
		reserved.add("boolean");
		reserved.add("do");
		reserved.add("if");
		reserved.add("private");
		reserved.add("this");
		reserved.add("break");
		reserved.add("double");
		reserved.add("implements");
		reserved.add("protected");
		reserved.add("throw");
		reserved.add("byte");
		reserved.add("else");
		reserved.add("import");
		reserved.add("public");
		reserved.add("throws");
		reserved.add("case");
		reserved.add("enum");
		reserved.add("instanceof");
		reserved.add("return");
		reserved.add("transient");
		reserved.add("catch");
		reserved.add("extends");
		reserved.add("int");
		reserved.add("short");
		reserved.add("try");
		reserved.add("char");
		reserved.add("final");
		reserved.add("interface");
		reserved.add("static");
		reserved.add("void");
		reserved.add("class");
		reserved.add("finally");
		reserved.add("long");
		reserved.add("strictfp");
		reserved.add("volatile");
		reserved.add("const");
		reserved.add("float");
		reserved.add("native");
		reserved.add("super");
		reserved.add("while");
		
		knownTypes.add("boolean");
		knownTypes.add("byte");
		knownTypes.add("char");
		knownTypes.add("double");
		knownTypes.add("float");
		knownTypes.add("int");
		knownTypes.add("long");
		knownTypes.add("short");
		knownTypes.add("void");
		knownTypes.add("String");
	}
}
