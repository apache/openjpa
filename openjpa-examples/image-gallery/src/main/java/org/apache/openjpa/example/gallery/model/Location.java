package org.apache.openjpa.example.gallery.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Location embeddable with several BV constraints applied.
 */
@Embeddable
public class Location {

    @NotNull(message="City must be specified.")
    private String city;
    
    private String street;
    
    private String state;

    @NotNull(message="Country must be specified.")
    @Size(message="Country must be 50 characters or less.", max=50)
    @Column(length=50)
    private String country;
    
    @Size(message="Zip code must be 10 characters or less.", max=10)
    @Pattern(message="Zip code must be 5 digits or use the 5+4 format.",
        regexp="^\\d{5}(([\\-]|[\\+])\\d{4})?$")
    @Column(length=10)
    private String zipCode;

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet() {
        return street;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getZipCode() {
        return zipCode;
    }
}
