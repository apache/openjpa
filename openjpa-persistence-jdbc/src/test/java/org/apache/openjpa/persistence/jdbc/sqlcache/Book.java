package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

@Entity
@DiscriminatorValue("BOOK")
public class Book extends Merchandise {
    private String title;
    
    @ManyToMany(fetch=FetchType.EAGER)
    private Set<Author> authors;

    public Book() {
        this("?");
    }
    
    public Book(String title) {
        setTitle(title);
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void addAuthor(Author a) {
        if (authors == null)
            authors = new HashSet<Author>();
        if (authors.add(a)) {
            a.addBook(this);
        }
    }
}
