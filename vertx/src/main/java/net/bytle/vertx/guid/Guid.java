package net.bytle.vertx.guid;

/**
 * A guid is a string identifier
 * Example: usr-xxmnmnsd
 */
public abstract class Guid {


  private String hash;

  @Override
  public String toString() {

    String hash = this.getHashOrNull();
    if (hash == null) {
      // A guid is serialized through Jackson as we hash it with a secret
      return toStringLocalIds();
    }
    return hash;

  }

  /**
   *
   * @return a toString that shows the local ids
   */
  public abstract String toStringLocalIds();

  /**
   * When receiving a request, building the guid at request time, it's easier to set it
   * to use it after wards if there is any error.
   * @return the hash if present or null
   * If null, uses {@link net.bytle.vertx.jackson.JacksonMapperManager#getSerializer(Class) Jackson}
   * (as we hash it with a secret, it's not in the Pojo)
   */
  public String getHashOrNull(){
    return this.hash;
  }

  public void setHash(String hash){
    this.hash = hash;
  }

}
