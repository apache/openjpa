package org.apache.openjpa.persistence.embed;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name="EmpEmbedTest")
public class Employee {
    @Id
    int empId;
    
    @Embedded 
    ContactInfo contactInfo;
    
    @Embedded 
    JobInfo jobInfo;

    @Embedded 
    LocationDetails location;

    @ElementCollection // use default table (PERSON_NICKNAMES)
    @Column(name="name", length=50)
    protected Set<String> nickNames = new HashSet<String>();
    
    public int getEmpId() {
        return empId;
    }
    
    public void setEmpId(int empId) {
        this.empId = empId;
    }
    
    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public ContactInfo getContactInfo() {
        return contactInfo;
    }
    
    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
    
    public JobInfo getJobInfo() {
        return jobInfo;
    }
    
    public LocationDetails getLocationDetails() {
        return location;
    }
    
    public void setLocationDetails(LocationDetails location) {
        this.location = location;
    }
    
    public Set<String> getNickNames() {
        return nickNames;
    }
    
    public void setNickNames(Set<String> nickNames) {
        this.nickNames = nickNames;
    }
    
    public void addNickName(String nickName) {
        nickNames.add(nickName);
    }
}
