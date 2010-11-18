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

package org.apache.openjpa.persistence.jest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

/**
 * Loads some example Actor-Movie data.
 *  
 * @author Pinaki Poddar
 *
 */
public class DataLoader {
    @SuppressWarnings("deprecation")
    public static Object[][] ACTOR_DATA = {
        new Object[] {"m01", "Robert", "Redford", Actor.Gender.Male, new Date(1950, 1, 12)},
        new Object[] {"m02", "Robert", "De Niro", Actor.Gender.Male, new Date(1940, 4, 14)},
        new Object[] {"m03", "Al",     "Pacino",  Actor.Gender.Male, new Date(1950, 1, 12)},
        new Object[] {"m04", "Brad",   "Pitt",    Actor.Gender.Male, new Date(1940, 4, 14)},
        new Object[] {"m05", "Clint",  "Eastwood",Actor.Gender.Male, new Date(1950, 1, 12)},
        
        new Object[] {"f01", "Meryl",   "Streep",    Actor.Gender.Female, new Date(1940, 4, 14)},
        new Object[] {"f02", "Anglina", "Jolie",     Actor.Gender.Female, new Date(1950, 1, 12)},
        new Object[] {"f03", "Goldie",  "Hawn",      Actor.Gender.Female, new Date(1940, 4, 14)},
        new Object[] {"f04", "Diane",   "Keaton",    Actor.Gender.Female, new Date(1950, 1, 12)},
        new Object[] {"f05", "Catherine", "Hepburn", Actor.Gender.Female, new Date(1940, 4, 14)},
    };
    
    public static Object[][] MOVIE_DATA = {
        new Object[] {"One flew over the cuckoo's nest", 1980},
        new Object[] {"Everyone Says I Love You", 1980},
        new Object[] {"Where Eagles Dare", 1980},
        new Object[] {"Fight Club", 1980},
        new Object[] {"Horse Whisperer", 1980},
    };

    public void populate(EntityManager em) throws Exception {
        Long count = em.createQuery("select count(m) from Movie m", Long.class).getSingleResult();
        if (count != null && count.longValue() > 0) {
            System.err.println("Found " + count + " Movie records in the database");
            return;
        }
        
        
        List<Actor> actors = createActors();
        List<Movie> movies = createMovies();
        linkActorAndMovie(actors, movies);
        makePartner(actors);
        em.getTransaction().begin();
        for (Actor a : actors) {
            em.persist(a);
        }
        for (Movie m : movies) {
            em.persist(m);
        }
        em.getTransaction().commit();
    }
    
    List<Actor> createActors() {
        List<Actor> actors = new ArrayList<Actor>();
        for (Object[] a : ACTOR_DATA) {
            Actor actor = new Actor((String)a[0], (String)a[1], (String)a[2], (Actor.Gender)a[3], (Date)a[4]);
            actors.add(actor);
        }
        return actors;
    }
    
    List<Movie> createMovies() {
        List<Movie> movies = new ArrayList<Movie>();
        for (Object[] m : MOVIE_DATA) {
            Movie movie = new Movie((String)m[0], (Integer)m[1]);
            movies.add(movie);
        }
        return movies;
    }
    
    void linkActorAndMovie(List<Actor> actors, List<Movie> movies) {
        for (Actor a : actors) {
            int n = rng.nextInt(movies.size());
            for (int i = 0; i < n; i++) {
                Movie m = random(movies);
                a.addMovie(m);
                m.addActor(a);
            }
        }
    }
    
    void makePartner(List<Actor> actors) {
        for (Actor p : actors) {
            if (p.getPartner() != null)
                continue;
            Actor f = random(actors);
            if (f.getPartner() == null && p.getGender() != f.getGender()) {
               p.setPartner(f);
               f.setPartner(p);
            }
       }
    }
    
    /**
     * Select a random element from the given list. 
     */
    private <T> T random(List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }
    
    private static Random rng = new Random();
}
