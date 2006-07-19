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
package org.apache.openjpa.lib.conf;

/**
 * A double {@link Value}.
 *
 * @author Marc Prud'hommeaux
 */
public class DoubleValue extends Value {

    private double value;

    public DoubleValue(String prop) {
        super(prop);
    }

    public Class getValueType() {
        return double.class;
    }

    /**
     * The internal value.
     */
    public void set(double value) {
        double oldValue = this.value;
        this.value = value;
        if (oldValue != value)
            valueChanged();
    }

    /**
     * The internal value.
     */
    public double get() {
        return value;
    }

    protected String getInternalString() {
        return String.valueOf(value);
    }

    protected void setInternalString(String val) {
        if (val == null || val.length() == 0)
            set(0D);
        else
            set(Double.parseDouble(val));
    }

    protected void setInternalObject(Object obj) {
        if (obj == null)
            set(0D);
        else
            set(((Number) obj).doubleValue());
    }
}
