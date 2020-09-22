package apache.openjpa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.apache.openjpa.lib.meta.ClassMetaDataIterator;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.UserException;
import org.junit.Assert;
import org.apache.openjpa.conf.*;


public class SpecificationTest {

	String myString = "JPA";
	String myCompleteString = "JPA 2.0-draft";
	String myCompleteString2 = "JPA 2.1-draft";
	String myHalfCompleteString = "JPA 2";
	String myHalfCompleteString2 = "JPQ 2";
	String myNullString = null;

	
	@Test
	public void EqualsTest() { 
		Specification spec = new Specification(myString);
		Assert.assertTrue(spec.equals(spec)); // parse() is overidded in Specification
	}

	@Test
	public void EqualsTest2() {
		Specification spec = new Specification(myString);
		Assert.assertFalse(spec.equals(myNullString)); // parse() is overidded in Specification
	}
		
	
	@Test 
	public void EqualsTest3() { 
		Specification spec = new Specification(myString);
		Specification spec2 = new Specification(myHalfCompleteString);
		Assert.assertFalse(spec.equals(spec2)); // parse() is overidded in Specification
	}

        @Test      
	public void EqualsTest4() { 
		Specification spec = new Specification(myString);
		Assert.assertFalse(spec.equals(1)); // parse() is overidded in Specification
	}
	
	@Test 
	public void EqualsTest5() {  
		Specification spec = new Specification(myHalfCompleteString);
		Specification spec2 = new Specification(myHalfCompleteString2);
		Assert.assertFalse(spec.equals(spec2)); 
	}
	
	@Test 
	public void EqualsTest6() { 
		Specification spec = new Specification(myCompleteString);
		Specification spec2 = new Specification(myHalfCompleteString2);
		Assert.assertFalse(spec.equals(spec2)); 
	}
	
	
}
