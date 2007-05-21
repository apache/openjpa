package org.apache.openjpa.lib.conf;

import junit.framework.TestCase;

public class TestValue extends TestCase {
    
    private static class SimpleValue extends Value {

        protected String getInternalString() {
            return null;
        }

        public Class getValueType() {
            return null;
        }

        protected void setInternalObject(Object obj) {
            
        }

        protected void setInternalString(String str) {
        }
        
    }
    
    public void testSetAliasesByValue() {
        String alias = "alias";
        String aName = "Johnny";
        String bName = "Pete";
        String [] aStrings = { alias, aName };
        
        SimpleValue sValue = new SimpleValue();
        sValue.setAliases(aStrings);
        sValue.setAlias(alias, bName);
        assertEquals("Did not set the new alias", bName, 
                sValue.getAliases()[1]);
        assertEquals("Array of aliases not set by value", aName, aStrings[1]);
    }
}
