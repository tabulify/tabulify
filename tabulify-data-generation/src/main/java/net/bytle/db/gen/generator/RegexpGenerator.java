package net.bytle.db.gen.generator;

import com.github.curiousoddman.rgxgen.RgxGen;
import net.bytle.db.gen.GenColumnDef;
import net.bytle.type.BigIntegers;
import net.bytle.type.Casts;

import java.util.HashSet;
import java.util.Set;

public class RegexpGenerator<T> extends CollectionGeneratorAbs<T> {


  private final RgxGen rgxGen;
  private String actualValue;

  public RegexpGenerator(Class<T> clazz, String regexp) {
    super(clazz);
    rgxGen = new RgxGen(regexp);
  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   *
   * @param genColumnDef
   * @param <T>
   * @return
   */
  public static <T> RegexpGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {
    String regexp = genColumnDef.getGeneratorProperty(String.class, "regexp");
    return (RegexpGenerator<T>) (new RegexpGenerator<>(clazz, regexp))
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
    this.actualValue = rgxGen.generate();
    return getActualValue();
  }

}
