package net.bytle.vertx;


import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.auth.AuthQueryProperty;

public class ValidationUtil {


  public static UriEnhanced validateAndGetRedirectUriAsUri(String redirectUri) {
    if (redirectUri == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri cannot be null", AuthQueryProperty.REDIRECT_URI.toString(), null);
    }
    UriEnhanced uri;
    try {
      uri = UriEnhanced.createFromString(redirectUri);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri is not a valid URI", AuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
    try {
      uri.getHost();
    } catch (NotFoundException e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri should be absolute", AuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
    return uri;
  }
}
