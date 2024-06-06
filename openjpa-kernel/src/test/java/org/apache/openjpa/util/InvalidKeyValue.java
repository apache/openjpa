package org.apache.openjpa.util;

public class InvalidKeyValue {
    @Override
    public int hashCode() {
        // Genera un valore casuale, violando la consistenza
        return (int) (Math.random() * 1000);
    }
}
