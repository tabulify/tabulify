package com.tabulify.yaml;

import org.yaml.snakeyaml.resolver.Resolver;

public class YamlNoImplicitTagResolver extends Resolver {


  /**
   * https://bitbucket.org/snakeyaml/snakeyaml/wiki/Howto#markdown-header-how-to-avoid-implicit-types
   * do not resolve float and timestamp
   */
  protected void addImplicitResolvers() {
    // addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
    // addImplicitResolver(Tags.FLOAT, FLOAT, "-+0123456789.");
    // addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789");
    // addImplicitResolver(Tag.INT, INT, "-+0123456789");
    // addImplicitResolver(Tag.MERGE, MERGE, "<");
    // addImplicitResolver(Tag.NULL, NULL, "~nN\0");
    // addImplicitResolver(Tag.NULL, EMPTY, null);
  }

}
