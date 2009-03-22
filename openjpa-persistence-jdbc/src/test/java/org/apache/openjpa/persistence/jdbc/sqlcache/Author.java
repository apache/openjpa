package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

@Entity
@DiscriminatorValue("AUTHOR")
public class Author extends Person {
    private String name;
    
    @ManyToMany(fetch=FetchType.LAZY)
    private Set<Book> books;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Author() {
        super();
    }

    public Author(String firstName, String lastName, short age, int yob) {
        super(firstName, lastName, age, yob);
    }

    public Set<Book> getBooks() {
        return books;
    }
    
    public void addBook(Book b) {
        if (books == null)
            books = new HashSet<Book>();
        if (books.add(b)) {
            b.addAuthor(this);
        }
    }
}
