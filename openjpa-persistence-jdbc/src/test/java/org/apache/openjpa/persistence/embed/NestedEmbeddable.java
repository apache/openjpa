package org.apache.openjpa.persistence.embed;

import javax.persistence.Embeddable;

@Embeddable
public class NestedEmbeddable
{
  private String field1;
  private String field2;

  public String getField1()
  {
    return this.field1;
  }

  public void setField1(String field1) {
    this.field1 = field1;
  }

  public String getField2() {
    return this.field2;
  }

  public void setField2(String field2) {
    this.field2 = field2;
  }
}
