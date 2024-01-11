package net.bytle.type.yaml;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.time.Timestamp;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.regex.Pattern;

/**
 * This constructor will change the default behavior of a YAML timestamp
 * <p>
 * A Yaml timestamp without timezone is considered a timestamp with the timezone UTC
 * <p>
 * By applying this constructor
 * <p>
 * A Yaml timestamp without timezone is considered a timestamp WITHOUT timezone
 * <p>
 * Timestamp spec:
 * <a href="https://yaml.org/type/timestamp.html">...</a>
 * <p>
 * Example:
 * <a href="https://bitbucket.org/asomov/snakeyaml/src/master/src/test/java/examples/jodatime/JodaTimeExampleTest.java">...</a>
 */
public class DefaultTimestampWithoutTimeZoneConstructor extends Constructor {


  /**
   * The regexp can be found in the spec
   * <a href="https://yaml.org/type/timestamp.html">...</a>
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
      String val = constructScalar((ScalarNode) node);
      if (NO_TIME_ZONE_TIMESTAMP_PATTERN.matcher(val).find()){
          try {
              return Timestamp.createFromString(val).toLocalDateTime();
          } catch (CastException e) {
              throw new InternalException("Due to the match, the string should be a valid t",e);
          }
      } else {
        return defaultSnakeYamlTimeStamp.construct(node);
      }
    }
  }


}
