package test.test;

import junit.framework.TestCase;

import org.apache.openjpa.enhance.PersistenceCapable;

import test.SomeEntity;

public class SomeEntityTest extends TestCase {
            
	public void testEnhancement() {
		SomeEntity o = new SomeEntity();
		assertTrue("SomeEntity is not PersistenceCapable", o instanceof PersistenceCapable);
	}
}
