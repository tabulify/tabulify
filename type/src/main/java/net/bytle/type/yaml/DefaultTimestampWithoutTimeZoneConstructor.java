package net.bytle.type.yaml;

import net.bytle.type.time.Timestamp;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.regex.Pattern;

/**
 * This constructor will change the default behavior of a YAML timestamp
 *
 * A Yaml timestamp without timezone is considered a timestamp with the timezone UTC
 *
 * By applying this constructor
 *
 * A Yaml timestamp without timezone is considered a timestamp WITHOUT timezone
 *
 * Timestamp spec:
 * https://yaml.org/type/timestamp.html
 *
 * Example:
 * https://bitbucket.org/asomov/snakeyaml/src/master/src/test/java/examples/jodatime/JodaTimeExampleTest.java
 */
public class DefaultTimestampWithoutTimeZoneConstructor extends Constructor {


  /**
   * The regexp can be found in the spec
   * https://yaml.org/type/timestamp.html
   */
  public static final Pattern NO_TIME_ZONE_TIMESTAMP_PATTERN;

  static {
    String YEAR = "[0-9][0-9][0-9][0-9]";
    String MONTH = "[0-9][0-9]?";
    String DAY = "[0-9][0-9]?";
    String T = "(?:[Tt]|[ \t]+)";
    String HOUR = "[0-9][0-9]?";
    String MINUTES = "[0-9][0-9]";
    String SECOND = "[0-9][0-9]";
    String FRACTION = "(?:\\.[0-9]*)?";
    NO_TIME_ZONE_TIMESTAMP_PATTERN = Pattern
      .compile("^(?:"+YEAR+ "-" + MONTH + "-" + DAY + T + HOUR + ":" + MINUTES + ":" + SECOND + FRACTION + ")$");
  }


  public static final ConstructYamlTimestamp defaultSnakeYamlTimeStamp =  new ConstructYamlTimestamp();

  public DefaultTimestampWithoutTimeZoneConstructor() {
    this.yamlConstructors.put(Tag.TIMESTAMP, new InternalConstructTimestamp());
  }


  private class InternalConstructTimestamp extends AbstractConstruct {

    public Object construct(Node node) {
      String val = (String) constructScalar((ScalarNode) node);
      if (NO_TIME_ZONE_TIMESTAMP_PATTERN.matcher(val).find()){
        return Timestamp.createFromString(val).toLocalDateTime();
      } else {
        return defaultSnakeYamlTimeStamp.construct(node);
      }
    }
  }


}

