/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package openbook.domain;


import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * A persistent entity represents a Customer.
 *
 * <br><b>Persistent Identity</b>: auto-generated identity.
 * <br><b>Mapping</b>:
 *
 * <br><b>Design Notes</b>: No setter for identity value.
 *
 * @author Pinaki Poddar
 *
 */
@Entity
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue
    private long id;
    private String name;
    private String email;

    public Customer() {
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Create a {@link ShoppingCart} for this customer.
     */
    public ShoppingCart newCart() {
        return new ShoppingCart(this);
    }

    @Override
    public String toString() {
        return name;
    }

    @Version
    private int version;

    public int getVersion() {
        return version;
    }

}
