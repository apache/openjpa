package org.apache.openjpa.persistence.embed;

import javax.persistence.*;
import java.util.*;

@Embeddable
public class JobInfo {
    
    String jobDescription;
    
    @ManyToOne 
    ProgramManager pm; // Bidirectional
    
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }
    
    public String getJobDescription() {
        return jobDescription;
    }
 
    public void setProgramManager(ProgramManager pm) {
        this.pm = pm;
    }
    
    public ProgramManager getProgramManager() {
        return pm;
    }
}
