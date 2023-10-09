package net.bytle.tower.util;

import net.bytle.java.JavaEnvs;

public class Env {

  /**
   * Define if the code is running in a dev environment
   */
  public static final boolean IS_DEV;
  static String SYSTEM_PROPERTY_NAME = "tower.web.environment";
  static String ENV_VARIABLE_NAME = "TOWER_WEB_ENVIRONMENT";

  static {

    if (JavaEnvs.IS_DEV) {

      IS_DEV = true;

    } else {

      /**
       * On an idea of
       * https://github.com/vert-x3/vertx-web/blob/master/vertx-web-common/src/main/java/io/vertx/ext/web/common/WebEnvironment.java
       */
      final String mode = System.getProperty(Env.SYSTEM_PROPERTY_NAME, System.getenv(Env.ENV_VARIABLE_NAME));
      IS_DEV = "dev".equalsIgnoreCase(mode) || "Development".equalsIgnoreCase(mode);

    }

  }


}
