package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.util.JsonTokenCipher;
import net.bytle.type.UriEnhanced;

/**
 * An interface to create all validation callback
 * that occurs by email.
 * <p>
 * ie we send a link that the user should click
 * to validate the action (ie registration, login, ...)
 */
public interface FlowCallback {

  /**
   * The parameter where the data is stored in the callback link
   */
  String URI_DATA_PARAMETER = "data";

  /**
   * The cipher
   */
  JsonTokenCipher DATA_CIPHER = JsonTokenCipher.BUFFER_BYTES;

  /**
   * The last part of the url
   */
  String LAST_URL_OPERATION_CALLBACK_PART = "callback";


  /**
   * Add dynamically the callback to the router
   *
   * @param router the router to build
   */
  void addCallback(Router router);

  /**
   * @return the path where to add the callback handler on the router
   */
  String getCallbackOperationPath();


  /**
   *
   * @return the operation path that is the origin, the start of the email flow that needs the callback.
   * It is used as the base operation to create a unique {@link #getCallbackOperationPath() callback operation path}
   */
  String getOriginOperationPath();

  /**
   * Return the validation object
   *
   * @param ctx   - the context
   * @param clazz - the class
   * @param <T>   the validation object class
   * @return the validation object
   */
  <T> T getCallbackData(RoutingContext ctx, Class<T> clazz);


  /**
   * @param validationObject - the object that goes into the data URL parameter
   * @param <T>              the validation object class
   * @return the validation uri that is found in the email
   */
  <T> UriEnhanced getCallbackUri(T validationObject);

}
