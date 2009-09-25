package org.apache.openjpa.persistence.entity;

import javax.persistence.Basic;
import javax.persistence.Id;

/**
 * This class doesn't have an @Entity and @Basic on purpose.
 */
public class MixedMappingLocation {
    @Id
    int id;

    String basic1;

    @Basic
    String basic2;
}
