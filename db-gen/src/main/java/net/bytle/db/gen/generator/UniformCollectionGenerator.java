package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;


/**
 * Distribution Generator that will return a value randomly chosen between a min and a max
 */
public class UniformCollectionGenerator<T> implements CollectionGeneratorOnce<T>, CollectionGeneratorScale<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UniformCollectionGenerator.class);


  private final Number range;
  private final Class<T> clazz;


  private Object o;
  private GenColumnDef columnDef;

  // Domain
  private Object min;
  private Object max;
  private Integer step = 1;


  /**
   * Distribution Generator that will return a value randomly chosen between a min and a max
   *
   * @param columnDef
   * @param min
   * @param max
   */
  public UniformCollectionGenerator(GenColumnDef<T> columnDef, Object min, Object max) {

    // Save the args
    this.columnDef = columnDef;

    // To cast the returned value back
    clazz = columnDef.getClazz();

    // Default
    SqlDataType sqlType = columnDef.getDataType();
    switch (sqlType.getTypeCode()) {
      case (Types.DOUBLE):
      case Types.FLOAT:
        this.min = min != null ? min : 0.0;
        this.max = max != null ? max : 10.0;
        this.o = ((Double) this.max - (Double) this.min)/2;
        this.range = ((Double) this.max - (Double) this.min);
        break;
      case Types.INTEGER:
        this.min = min != null ? min : 0;
        this.max = max != null ? max : 10;
        this.o = ((Integer) this.max- (Integer) this.min)/2;
        this.range = ((Integer) this.max - (Integer) this.min);
        break;
      case Types.NUMERIC:
        this.min = min != null ? min : BigDecimal.valueOf(0);
        this.max = max != null ? max : BigDecimal.valueOf(10);
        this.range = (((BigDecimal) this.max).min((BigDecimal) this.min));
        BigDecimal half = ((BigDecimal) this.range).divide(BigDecimal.valueOf(2),0);
        this.o = ((BigDecimal) this.min).add(half);
        break;
      case Types.DATE:
        Date minDefault = Date.valueOf(LocalDate.now().minusDays(10));
        Date maxDefault = Date.valueOf(LocalDate.now());
        this.min = min != null ? min : clazz.cast(minDefault);
        this.max = max != null ? max : clazz.cast(maxDefault);
        range = ((int) DAYS.between(((Date) this.min).toLocalDate(), ((Date) this.max).toLocalDate()));
        o = Date.valueOf(((Date) this.min).toLocalDate().plusDays((int)range/2));
        break;
      case Types.TIMESTAMP:
        Timestamp minTimestampDefault = Timestamp.valueOf(LocalDateTime.now().minusDays(10));
        Timestamp maxTimeStampDefault = Timestamp.valueOf(LocalDateTime.now());
        this.min = min != null ? min : clazz.cast(minTimestampDefault);
        this.max = max != null ? max : clazz.cast(maxTimeStampDefault);
        range = (((Timestamp) this.max).getTime() - ((Timestamp) this.min).getTime()) / 1000;
        this.o = Timestamp.valueOf(((Timestamp) this.min).toLocalDateTime().plus(((long) range)/2, SECONDS));
        break;
      case Types.CHAR:
      case Types.VARCHAR:
        int minCharDefault = Integer.parseInt("0061", 16); // a
        int maxCharDefault = Integer.parseInt("007A", 16); // z
        if (min != null) {
          if (min instanceof String) {
            if (((String) min).length() > 1) {
              throw new RuntimeException("The minimum value (" + min + ") defined for the column (" + columnDef + ") is a string that has more than one character. This is not possible to be able to calculate the range");
            }
            this.min = ((String) min).charAt(0);
          } else if (min instanceof Character) {
            this.min = (int) (Character) min;
          } else {
            throw new RuntimeException("The minimum value (" + min + ") defined for the column (" + columnDef + ") is a not a string or a character.");
          }
        } else {
          this.min = minCharDefault;
        }
        if (max != null) {
          if (max instanceof String) {
            if (((String) max).length() > 1) {
              throw new RuntimeException("The maximum value (" + max + ") defined for the column (" + columnDef + ") is a string that has more than one character. This is not possible to be able to calculate the range");
            }
            this.max = ((String) max).charAt(0);
          } else if (max instanceof Character) {
            this.max = (int) (Character) max;
          } else {
            throw new RuntimeException("The maximum value (" + max + ") defined for the column (" + columnDef + ") is a not a string or a character.");
          }
        } else {
          this.max = maxCharDefault;
        }
        range = ((char) (int) this.max - (char) (int) this.min) / step;
        this.o = this.min;
        break;
      default:
        throw new RuntimeException("The data type with the type code (" + sqlType.getTypeCode() + "," + sqlType.getClazz().getSimpleName() + ") is not supported for the column " + columnDef.getFullyQualifiedName());
    }

  }


  public static <T> UniformCollectionGenerator<T> of(GenColumnDef<T> columnDef) {

    return new UniformCollectionGenerator<T>(columnDef, null, null);

  }


  private String getString() {
    Integer precision = this.columnDef.getPrecisionOrMax();
    if (precision == null) {
      precision = CollectionGeneratorOnce.MAX_STRING_PRECISION;
      LOGGER.warn(
        Strings.multiline("The precision for the column (" + this.columnDef + ") is unknown",
          "The max precision for its data type (" + columnDef.getDataType().getTypeName() + ") is unknown",
          "The precision was then set to " + precision
        ));
    }

    String s = "hello";
    if (s.length() > precision) {
      s = s.substring(0, precision);
    }
    return s;
  }

  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {


    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.DOUBLE:
        o = Math.random() * (Double) range * step;
        if (min != null) {
          o = (Double) o + (Double) min;
        }
        break;
      case Types.INTEGER:
        o = (int) Math.random() * (int) range * step;
        if (min != null) {
          o = (int) o + (int) min;
        }
        break;
      case Types.NUMERIC:
        o = BigDecimal.valueOf(range.doubleValue()).multiply(BigDecimal.valueOf(Math.random())).multiply(BigDecimal.valueOf(step));
        if (min != null) {
          o = ((BigDecimal) o).add(((BigDecimal) min));
        }
        break;
      case Types.DATE:
        int i = (int) (Math.random()) * (int) range * step;
        LocalDate localValue = ((Date) min).toLocalDate();
        o = Date.valueOf(localValue.plusDays(i));
        break;
      case Types.TIMESTAMP:
        long iTimestamp = new Double(Math.random() * (long) range * step).longValue();
        LocalDateTime localValueTimestamp = ((Timestamp) min).toLocalDateTime();
        o = Timestamp.valueOf(localValueTimestamp.plusSeconds(iTimestamp));
        break;
      case Types.CHAR:
      case Types.VARCHAR:
        o = (char) ((int) min + (Math.random() * (int) range) * step);
        o = String.valueOf(o); // A CHAR or a VARCHAR is a string
        break;
      default:
        throw new RuntimeException("The data type with the type code (" + dataType.getTypeCode() + "," + dataType.getClazz().getSimpleName() + ") is not supported for the column " + columnDef.getFullyQualifiedName());

    }


    return clazz.cast(o);
  }

  /**
   * @return a generated value (used in case of derived data
   */
  @Override
  public T getActualValue() {
    return clazz.cast(o);
  }

  /**
   * @return the column attached to this generator
   * It permits to create parent relationship between generators
   * when asking a value for a column, we may need to ask the value for another column before
   */
  @Override
  public GenColumnDef getColumn() {

    return columnDef;

  }


  @Override
  public Long getMaxGeneratedValues() {
    return Long.MAX_VALUE;
  }

  @Override
  public <T> T getDomainMax() {
    return (T) max;
  }

  @Override
  public <T> T getDomainMin() {
    return (T) min;
  }

  @Override
  public UniformCollectionGenerator<T> step(Integer step) {
    this.step = step;
    return this;
  }


  @Override
  public String toString() {
    return "DistributionGenerator{" + columnDef + '}';
  }

  public UniformCollectionGenerator setMin(Object min) {
    this.min = min;
    return this;
  }

  public UniformCollectionGenerator setMax(Object max) {
    this.max = max;
    return this;
  }


}
