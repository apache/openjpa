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
package org.apache.openjpa.persistence.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Entity with a composite primary key, the target of bulk updates and
 * deletes with a subquery over the same entity.
 */
@Entity
@Table(name = "BSQ_LINE")
@IdClass(BulkSubqueryLine.LineId.class)
public class BulkSubqueryLine {

    @Id
    private long orderId;

    @Id
    private int lineNo;

    private int quantity;

    public BulkSubqueryLine() {
    }

    public BulkSubqueryLine(long orderId, int lineNo, int quantity) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.quantity = quantity;
    }

    public long getOrderId() {
        return orderId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Identity of {@link BulkSubqueryLine}.
     */
    public static class LineId {

        private long orderId;

        private int lineNo;

        public LineId() {
        }

        public LineId(long orderId, int lineNo) {
            this.orderId = orderId;
            this.lineNo = lineNo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LineId)) {
                return false;
            }
            LineId other = (LineId) o;
            return orderId == other.orderId && lineNo == other.lineNo;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(orderId) * 31 + lineNo;
        }
    }
}
