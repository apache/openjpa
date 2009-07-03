package org.apache.openjpa.persistence.criteria;

import java.sql.Date;

import javax.persistence.*;

@Entity
public class Request {
     @Id
     int id;
     
     private short status;
    
     @ManyToOne(optional = false, fetch = FetchType.LAZY)
     private Account account;

     Date requestTime;

     public int getId() {
         return id;
     }
     
     public void setId(int id) {
         this.id = id;
     }
     
     public short getStatus() {
         return status;
     }

     public void setStatus(short status) {
         this.status = status;
     }

     
     public Account getAccount() {
         return account;
     }

     public void setAccount(Account account) {
         this.account = account;
     }
    
     public Date getRequestTime() {
         return requestTime;
     }
     
     public void setRequestTime(Date requestTime) {
         this.requestTime = requestTime;
     }
     
}
