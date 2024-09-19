/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.proxy.delayed.hset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.proxy.delayed.Award;
import org.apache.openjpa.persistence.proxy.delayed.Certification;
import org.apache.openjpa.persistence.proxy.delayed.DelayedProxyCollectionsTestCase;
import org.apache.openjpa.persistence.proxy.delayed.IAccount;
import org.apache.openjpa.persistence.proxy.delayed.IDepartment;
import org.apache.openjpa.persistence.proxy.delayed.IEmployee;
import org.apache.openjpa.persistence.proxy.delayed.IMember;
import org.apache.openjpa.persistence.proxy.delayed.IUserIdentity;
import org.apache.openjpa.persistence.proxy.delayed.Location;
import org.apache.openjpa.persistence.proxy.delayed.Product;

public class TestDelayedHashSetProxy extends DelayedProxyCollectionsTestCase {

    public static Object[] _pcList = {
        Employee.class,
        Department.class,
        UserIdentity.class,
        Member.class,
        Account.class
    };

    @Override
    public void setUp() {
        super.setUp(_pcList);
    }

    @Override
    public void setUp(Object... props){
        List<Object> parms = new ArrayList<>();
        // Add package-specific types
        parms.addAll(Arrays.asList(_pcList));
        // Add properties from super
        parms.addAll(Arrays.asList(props));
        super.setUp(parms.toArray());
    }

    @Override
    public IUserIdentity findUserIdentity(EntityManager em, int id) {
        return em.find(UserIdentity.class, id);
    }

    @Override
    public IDepartment findDepartment(EntityManager em, int id) {
        return em.find(Department.class, id);
    }

    @Override
    public IUserIdentity createUserIdentity() {
        UserIdentity ui = new UserIdentity();
        return ui;
    }

    @Override
    public IAccount createAccount(String name, IUserIdentity ui) {
        IAccount acct = new Account(name, ui);
        return acct;
    }

    @Override
    public IDepartment createDepartment() {
        Department d = new Department();
        return d;
    }

    @Override
    public IMember createMember(String name) {
        Member m = new Member();
        m.setName(name);
        return m;
    }

    @Override
    public IEmployee createEmployee() {
        Employee e = new Employee();
        return e;
    }

    @Override
    public Collection<IEmployee> createEmployees() {
        return new HashSet<>();
    }

    @Override
    public Collection<Product> createProducts() {
        return new HashSet<>();
    }

    @Override
    public Collection<Award> createAwards() {
        return new HashSet<>();
    }

    @Override
    public Collection<Location> createLocations() {
        return new HashSet<>();
    }

    @Override
    public Collection<Certification> createCertifications() {
        return new HashSet<>();
    }

    @Override
    public Collection<IAccount> createAccounts() {
        return new HashSet<>();
    }

    @Override
    public IEmployee getEmployee(Collection<IEmployee> emps, int idx) {
        if (emps == null || emps.iterator() == null) {
            return null;
        }
        return emps.iterator().next();
    }

    @Override
    public Product getProduct(Collection<Product> products, int idx) {
        if (products == null || products.iterator() == null) {
            return null;
        }
        return products.iterator().next();
    }
}
