package net.bytle.db.gen.generator;

/**
 * To retrieve value on a scale, you can do it by sequence or random
 * This interface helps to create a {@link RandomGenerator} used in a foreign column
 * that will randomly return new value that can be created by a {@link SequenceGenerator} used on a primary key
 * @param <T>
 */
public interface CollectionGeneratorScale<T> {

  /**
   *
   * @return the max generated value for the given size
   * This is used to get the max for the domain on the whole data resource.
   *
   * The {@link GenFsDataDef#getSize()} is generally injected to obtain the max for the domain
   * according to this size.
   *
   *
   * This is used to create a {@link RandomGenerator} on a {@link ForeignColumnGenerator foreign column}
   *
   */
  T getDomainMax(long size);

  /**
   *
   * @return the max generated value of the domain capped by the {@link GenFsDataDef#setMaxRecordCount(Long) max size}
   * on the data resources (included)
   *
   * It's not capped by the size of the other generators of the same data resource
   * otherwise there will be a stackoverflow error (every generator would need the value of the other)
   *
   * See {@link #getDomainMax(long)} etDomainMax}
   */
  T getDomainMax();

  /**
   *
   * @return same as {@link #getDomainMax(long)} but for the minimum
   */
  T getDomainMin(long size);

  /**
   *
   * @return same as {@link #getDomainMax()} but for the minimum
   */
  T getDomainMin();


  /**
   *
   * @param step - the value of a step
   * @return
   */
  CollectionGenerator<T> setStep(Number step);


}
