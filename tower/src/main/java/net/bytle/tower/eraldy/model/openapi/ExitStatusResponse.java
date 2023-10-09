package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A response with a status code and status message  Example of error on Github: {   \&quot;message\&quot;: \&quot;Requires authentication\&quot;,   \&quot;documentation_url\&quot;: \&quot;https://docs.github.com/rest/reference/users#list-email-addresses-for-the-authenticated-user\&quot; }  Example of error in Google hiting https://www.googleapis.com/oauth2/v1/userinfo {   \&quot;error\&quot;: {     \&quot;code\&quot;: 401,     \&quot;message\&quot;: \&quot;Request is missing required authentication credential. Expected OAuth 2 access token, login cookie or other valid authentication credential. See https://developers.google.com/identity/sign-in/web/devconsole-project.\&quot;,     \&quot;status\&quot;: \&quot;UNAUTHENTICATED\&quot;   } }
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExitStatusResponse   {

  private Integer code;
  private String message;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ExitStatusResponse () {
  }

  /**
  * @return code The status code
  */
  @JsonProperty("code")
  public Integer getCode() {
    return code;
  }

  /**
  * @param code The status code
  */
  @SuppressWarnings("unused")
  public void setCode(Integer code) {
    this.code = code;
  }

  /**
  * @return message A feedback message
  */
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  /**
  * @param message A feedback message
  */
  @SuppressWarnings("unused")
  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExitStatusResponse exitStatusResponse = (ExitStatusResponse) o;
    return Objects.equals(code, exitStatusResponse.code) &&
        Objects.equals(message, exitStatusResponse.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message);
  }

  @Override
  public String toString() {
    return "class ExitStatusResponse {\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
