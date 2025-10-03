package com.tabulify.gen.generator;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.type.BigIntegers;
import net.bytle.type.Casts;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RegexpGenerator<T> extends CollectionGeneratorAbs<T> {


  private final RgxGen rgxGen;
  private final String expression;
  private final Random random;
  private String actualValue;

  /**
   * @param clazz  - the class of data to return
   * @param regexp - the regular expression
   * @param seed   - the seed for the random function (used to be deterministic in the documentation)
   */
  public RegexpGenerator(Class<T> clazz, String regexp, Long seed) {
    super(clazz);
    expression = regexp;
    rgxGen = new RgxGen(regexp);
    if (seed == null) {
      random = new Random();
    } else {
      random = new Random(seed);
    }

  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> RegexpGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {
    Map<RegexpArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(RegexpArgument.class);
    String regexp = (String) argumentMap.get(RegexpArgument.EXPRESSION);
    if (regexp == null || regexp.isEmpty()) {
      throw new IllegalArgumentException("The expression attribute is mandatory for the regexp generator");
    }
    Object seedObject = argumentMap.get(RegexpArgument.SEED);
    Long seed;
    try {
      seed = Casts.cast(seedObject, Long.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The seed attribute is not a long. Value: " + seedObject, e);
    }

    return (RegexpGenerator<T>) (new RegexpGenerator<>(clazz, regexp, seed))
      .setColumnDef(genColumnDef);
  }

  @Override
  public long getCount() {
    return BigIntegers.createFromBigInteger(rgxGen.numUnique()).toLong();
  }

  @Override
  public void reset() {
    // Nothing to do here
  }

  @Override
  public T getActualValue() {
    return Casts.castSafe(this.actualValue, this.clazz);
  }

  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    return new HashSet<>();
  }

  @Override
  public T getNewValue() {
    this.actualValue = rgxGen.generate(random);
    return getActualValue();
  }

  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.REGEXP;
  }

  @Override
  public Boolean isNullable() {
    return false;
  }

  public String getExpression() {
    return expression;
  }
}
