package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The data needed to login with a login/password
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordCredentials   {


  protected String loginRealm;

  protected String loginHandle;

  protected String loginPassword;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public PasswordCredentials () {
  }

  /**
  * @return loginRealm The realm of the user
  */
  @JsonProperty("loginRealm")
  public String getLoginRealm() {
    return loginRealm;
  }

  /**
  * @param loginRealm The realm of the user
  */
  @SuppressWarnings("unused")
  public void setLoginRealm(String loginRealm) {
    this.loginRealm = loginRealm;
  }

  /**
  * @return loginHandle The email of the user
  */
  @JsonProperty("loginHandle")
  public String getLoginHandle() {
    return loginHandle;
  }

  /**
  * @param loginHandle The email of the user
  */
  @SuppressWarnings("unused")
  public void setLoginHandle(String loginHandle) {
    this.loginHandle = loginHandle;
  }

  /**
  * @return loginPassword The password of the user
  */
  @JsonProperty("loginPassword")
  public String getLoginPassword() {
    return loginPassword;
  }

  /**
  * @param loginPassword The password of the user
  */
  @SuppressWarnings("unused")
  public void setLoginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PasswordCredentials passwordCredentials = (PasswordCredentials) o;
    return
            Objects.equals(loginRealm, passwordCredentials.loginRealm) && Objects.equals(loginHandle, passwordCredentials.loginHandle) && Objects.equals(loginPassword, passwordCredentials.loginPassword);

  }

  @Override
  public int hashCode() {
    return Objects.hash(loginRealm, loginHandle, loginPassword);
  }

  @Override
  public String toString() {
    return loginRealm + ", " + loginHandle + ", " + loginPassword;
  }

}
