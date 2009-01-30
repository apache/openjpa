package org.apache.openjpa.persistence.jdbc.sqlcache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;


@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class Person {
    @Id
    @GeneratedValue
    private long id;
    
    private String firstName;
    private String lastName;
    private short age;
    private int   yob;
    
    public Person() {
        this("?", "?", (short)0, 0);
    }
    public Person(String firstName, String lastName, short age, int yob) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.yob = yob;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public short getAge() {
        return age;
    }
    public void setAge(short age) {
        this.age = age;
    }
    public int getBirthYear() {
        return yob;
    }
    public void setBirthYear(int yob) {
        this.yob = yob;
    }
    public long getId() {
        return id;
    }
}
