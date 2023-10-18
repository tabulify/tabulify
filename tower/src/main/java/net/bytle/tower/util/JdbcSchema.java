package net.bytle.tower.util;

public class JdbcSchema {
  private final Builder Builder;


  public JdbcSchema(Builder Builder) {
    this.Builder = Builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getLocation() {
    return this.Builder.location;
  }

  public String getSchema() {
    return this.Builder.schema;
  }

  public static class Builder {
    public String schema;
    private String location;

    public Builder setLocation(String location) {
      this.location = location;
      return this;
    }

    public JdbcSchema build() {
      return new JdbcSchema(this);
    }

    public Builder setSchema(String schema) {
      this.schema = schema;
      return this;
    }

  }
}
