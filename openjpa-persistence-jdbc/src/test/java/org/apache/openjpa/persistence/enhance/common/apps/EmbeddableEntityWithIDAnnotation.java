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

package org.apache.openjpa.persistence.enhance.common.apps;

// default package

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Id;

/**
 * BillingNoteId entity. @author MyEclipse Persistence Tools
 */
@Embeddable
public class EmbeddableEntityWithIDAnnotation implements java.io.Serializable
{

	private static final long serialVersionUID = 558333273831654654L;

	private Long id;

    private Long seqNo = new Long(2012);

    public EmbeddableEntityWithIDAnnotation( )
    {
    }

    public EmbeddableEntityWithIDAnnotation( Long id, Long seqNo )
    {
        this.id = id;
        this.seqNo = seqNo;
    }

    // Property accessors
    @Id
    @Column(unique=true, nullable=false, precision=12, scale=0)
    public Long getId()
    {
        return this.id;
    }

    public void setId( Long id )
    {
        this.id = id;
    }

    @Column( name = "EmbeddableEntitySeqNo", nullable = false, precision = 12, scale = 0 )
    public Long getSeqNo()
    {
        return this.seqNo;
    }

    public void setSeqNo( Long seqNo )
    {
        this.seqNo = seqNo;
    }

    @Override
    public boolean equals( Object other )
    {
        if ( ( this == other ) )
            return true;
        if ( ( other == null ) )
            return false;
        if ( !( other instanceof EmbeddableEntityWithIDAnnotation ) )
            return false;
        EmbeddableEntityWithIDAnnotation castOther = ( EmbeddableEntityWithIDAnnotation ) other;

        return ( ( this.getId( ) == castOther.getId( ) )
        		|| ( this.getId( ) != null && castOther.getId( ) != null
        		&& this.getId( ).equals( castOther.getId( ) ) ) )
                && ( ( this.getSeqNo( ) == castOther.getSeqNo( ) )
                		|| ( this.getSeqNo( ) != null && castOther.getSeqNo( ) != null
                		&& this.getSeqNo( ).equals( castOther.getSeqNo( ) ) ) );
    }

    @Override
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + ( getId( ) == null ? 0 : this.getId( ).hashCode( ) );
        result = 37 * result + ( getSeqNo( ) == null ? 0 : this.getSeqNo( ).hashCode( ) );
        return result;
    }

}
