/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;

/**
 * Represents a SQL join.
 *
 * @author Abe White
 */
public class Join
    implements Cloneable, JoinSyntaxes {

    public static final int TYPE_INNER = 0;
    public static final int TYPE_OUTER = 1;
    public static final int TYPE_CROSS = 2;

    private int _type = TYPE_INNER;

    private int _alias1;
    private int _alias2;
    private Table _table1;
    private Table _table2;
    private ForeignKey _fk;
    private boolean _inverse;

    /**
     * Constructor for inner and outer joins.
     */
    Join(Table table1, int alias1, Table table2, int alias2, ForeignKey fk,
        boolean inverse) {
        _table1 = table1;
        _alias1 = alias1;
        _table2 = table2;
        _alias2 = alias2;
        _fk = fk;
        _inverse = inverse;
    }

    /**
     * Private default constructor.
     */
    private Join() {
    }

    public int getType() {
        return _type;
    }

    public void setType(int type) {
        _type = type;
    }

    public String getAlias1() {
        return SelectImpl.toAlias(_alias1);
    }

    public String getAlias2() {
        return SelectImpl.toAlias(_alias2);
    }

    int getIndex1() {
        return _alias1;
    }

    int getIndex2() {
        return _alias2;
    }

    public Table getTable1() {
        return _table1;
    }

    public Table getTable2() {
        return _table2;
    }

    public ForeignKey getForeignKey() {
        return _fk;
    }

    public boolean isForeignKeyInversed() {
        return _inverse;
    }

    /**
     * Return a join that is this join in reverse.
     */
    public Join reverse() {
        Join join = new Join();
        join._type = _type;
        join._table1 = _table2;
        join._alias1 = _alias2;
        join._table2 = _table1;
        join._alias2 = _alias1;
        join._fk = _fk;
        join._inverse = !_inverse;
        return join;
    }

    public int hashCode() {
        return _alias1 ^ _alias2;
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof Join))
            return false;

        Join j = (Join) other;
        return (_alias1 == j._alias1 && _alias2 == j._alias2)
            || (_alias1 == j._alias2 && _alias2 == j._alias1);
    }

    public String toString() {
        String typeString;
        if (_type == TYPE_CROSS)
            typeString = "cross";
        else if (_type == TYPE_INNER)
            typeString = "inner";
        else
            typeString = "outer";
        return "<" + System.identityHashCode(this) + "> t"
            + _alias1 + "->t" + _alias2 + " (" + typeString + ")";
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (Exception e) {
            return null;
        }
    }
}

