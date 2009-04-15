package org.apache.openjpa.persistence.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.TemporaryClassLoader;

/**
 * Tests generation of canonical meta-model classes.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestCanonicalMetamodelGeneration extends junit.framework.TestCase {
	public static final char DOT   = '.';
	public static final char SLASH = '/';
	public static final String UNDERSCORE = "_";
	public static final String BLANK = "";
	public static final String PACKAGE = "org.apache.openjpa.persistence.meta.";
	public static final String[] DOMAIN_CLASSES = {
		PACKAGE + "DefaultFieldAccessMappedSuperclass",
		PACKAGE + "DefaultFieldAccessBase", 
		PACKAGE + "DefaultFieldAccessSubclass", 
		PACKAGE + "Embed0", 
		PACKAGE + "Embed1",
		PACKAGE + "ExplicitFieldAccessMixed", 
		PACKAGE + "ExplicitPropertyAccessMixed",
		PACKAGE + "ExplicitFieldAccess", 
		PACKAGE + "ExplicitPropertyAccess",
		PACKAGE + "ArrayMember"
	};
	public static String ADDITIONAL_CLASSPATH;
	private static boolean isMaven;
	private static boolean compiled; 
	
	public void setUp() throws Exception {
		if (!compiled) {
			String currentDir = System.getProperty("user.dir");
			isMaven = currentDir.endsWith("openjpa-persistence");
			String srcPath = isMaven ? BLANK : "openjpa-persistence";
			ADDITIONAL_CLASSPATH = currentDir + File.separator + 
			                       srcPath + File.separator + 
			                  "src.test.java".replace(DOT, File.separatorChar);
			System.err.println("Additional Path = " + ADDITIONAL_CLASSPATH);
			addClasspath(ADDITIONAL_CLASSPATH);
			deleteByteCode(false, DOMAIN_CLASSES);
			deleteByteCode(true, DOMAIN_CLASSES);
			deleteSourceCode(true, DOMAIN_CLASSES);
			
			compile(findSourceCode(false, DOMAIN_CLASSES));
			compiled = true;
		}
	}
	
	File findFile(String clsName, String extension) {
		String filePath = ADDITIONAL_CLASSPATH + File.separator + 
		clsName.replace(DOT, File.separatorChar) + extension ;
		File file = Files.getFile(filePath, null);
		assertTrue(filePath + " does not exist", file.exists());
		return file;
	}

	File findSourceCode(String clsName) {
		return findFile(clsName, ".java");
	}
	
	File findByteCode(String clsName) {
		return findFile(clsName, ".class");
	}
	
	File[] findSourceCode(boolean canonical, String...classNames) {
		File[] files = new File[classNames.length];
		for (int i = 0; i < classNames.length; i++) {
			files[i] = findSourceCode(classNames[i] 
			         +  (canonical ? UNDERSCORE : BLANK));
		}
		return files;
	}
	
	File[] findByteCode(boolean canonical, String...classNames) {
		File[] files = new File[classNames.length];
		for (int i = 0; i < classNames.length; i++) {
			files[i] = findByteCode(classNames[i] 
			         +  (canonical ? UNDERSCORE : BLANK));
		}
		return files;
	}
	
	/**
	 * Deletes the *.java of the given class names.
	 */
	void deleteSourceCode(boolean canonical, String...classNames) {
		for (String clsName : classNames) {
			try {
				findSourceCode(clsName + (canonical ? UNDERSCORE : BLANK))
					.delete();
			} catch (Throwable t) {
				
			}
		}
	}
	/**
	 * Deletes the *.class of the given class names.
	 */
	void deleteByteCode(boolean canonical, String...classNames) {
		for (String clsName : classNames) {
			try {
				findByteCode(clsName + (canonical ? UNDERSCORE : BLANK)).delete();
			} catch (Throwable t) {
				
			}
		}
	}
	
	public void testCanonicalModelIsGenerated() {
		findSourceCode(true, DOMAIN_CLASSES);
		findByteCode(true, DOMAIN_CLASSES);
	}
	
	public void testExplicitFieldAccessMixed() {
		Class<?> model = loadCanonicalClassFor(PACKAGE + "ExplicitFieldAccessMixed");
		assertField(model, "f1", "f2", "f4", "f5");
	}
	
	public void testExplicitFieldAccess() {
		Class<?> model = loadCanonicalClassFor(PACKAGE + "ExplicitFieldAccess");
		assertField(model, "f1", "f2", "f4", "f5");
	}
	
	public void testExplicitPropertyAccessMixed() {
		Class<?> model = loadCanonicalClassFor(PACKAGE + "ExplicitPropertyAccessMixed");
		assertField(model, "f1", "f3", "f4", "f5", "f6");
	}
	
	public void testExplicitPropertyAccess() {
		Class<?> model = loadCanonicalClassFor(PACKAGE + "ExplicitPropertyAccess");
		assertField(model, "f1", "f3", "f4", "f5", "f6");
	}
	
	public void testArrayMembers() {
		Class<?> model = loadCanonicalClassFor(PACKAGE + "ArrayMember");
		assertField(model, "Array", "array");
	}
	
	/**
	 * Compiles the given *.java files with an annotation processor. 
	 */
	void compile(File...files) {
		List<File> classpaths = getClasspath(isMaven 
				? "surefire.test.class.path" : "java.class.path");
		List<File> classOutput = Collections.singletonList
			(new File(ADDITIONAL_CLASSPATH)); 
		
		List<? extends Processor> processors = Arrays.asList(new Processor[]{
				new AnnotationProcessor6()});
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager mgr = compiler.getStandardFileManager(null, 
				null, null);
		try {
			mgr.setLocation(StandardLocation.CLASS_PATH, classpaths);
			mgr.setLocation(StandardLocation.CLASS_OUTPUT, classOutput);
			mgr.setLocation(StandardLocation.SOURCE_OUTPUT, classOutput);
			print("Javac Compiler Source output= ", mgr.getLocation(StandardLocation.SOURCE_OUTPUT));
			print("Javac Compiler Class output = ", mgr.getLocation(StandardLocation.CLASS_OUTPUT));
			print("Javac Compiler Classpath    = ", mgr.getLocation(StandardLocation.CLASS_PATH));
		} catch (IOException e) {
			fail();
		}
		List<String> options = Arrays.asList(new String[]{"-Alog"});
		CompilationTask task = compiler.getTask(null, // writer
				mgr, 
				null, // listener, 
				options, // options 
				null, // classes 
				mgr.getJavaFileObjects(files));
		task.setProcessors(processors);
		task.call();
	}

	void addClasspath(String newPath) {
		String sysprop = isMaven ? "surefire.test.class.path" : "java.class.path";
		String currentPath = System.getProperty(sysprop);
		System.setProperty(sysprop, currentPath + File.pathSeparator + newPath);
	}
	
	List<File> getClasspath(String sysprop) {
		List<File> result = new ArrayList<File>();
		String[] paths = System.getProperty(sysprop).split(File.pathSeparator);
		for (String path : paths) {
			result.add(new File(path));
		}
		return result;
	}
	
	Class<?> loadCanonicalClassFor(String clsName) {
		try {
			GeneratedClassLoader loader = new GeneratedClassLoader(
					this.getClass().getClassLoader());
			return loader.loadClass(clsName + UNDERSCORE);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to load canonical class for " + clsName);
		}
		return null;
	}
	
	/**
	 * Assert the given class has the given fields and no other field.
	 */
	void assertField(Class<?> cls, String... fieldNames) {
		for (String f : fieldNames) {
			try {
				cls.getField(f);
			} catch (NoSuchFieldException e) {
				fail(f + " not found in " + cls);
			}
		}
		assertEquals("Actual members : " + Arrays.toString(cls.getFields()) + 
				" expected " + Arrays.toString(fieldNames), 
				fieldNames.length, cls.getFields().length);
	}
	
	void print(String header, Iterable<? extends File> location) {
		Iterator<? extends File> files = location.iterator();
		System.err.println(header + files.next().getAbsolutePath());
		while (files.hasNext())
			System.err.println("\t" + files.next().getAbsolutePath());
		
	}
	
	
	public class GeneratedClassLoader extends TemporaryClassLoader {
		public GeneratedClassLoader(ClassLoader loader) {
			super(loader);
		}
	
		@Override
		public InputStream getResourceAsStream(String clsName)  {
			String path = ADDITIONAL_CLASSPATH + File.separator + 
				clsName.replace(SLASH, File.separatorChar);
			try {
				return new FileInputStream(Files.getFile(path, null));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				fail(clsName + " loacation " + path + " not loadable");
			}
			return null;
		}
	}
}
