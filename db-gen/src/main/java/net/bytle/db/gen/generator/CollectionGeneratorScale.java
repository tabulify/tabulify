package net.bytle.db.gen.generator;

/**
 * To retrieve value on a scale, you can do it by sequence or random
 * This interface helps to create a {@link UniformCollectionGenerator} used in a foreign column
 * that will randomly return new value that can be created by a {@link SequenceGenerator} used on a primary key
 * @param <T>
 */
public interface CollectionGeneratorScale<T> {

  /**
   *
   * @param <T>
   * @return the max generated value of the domain
   * If the collection generator generates values from 0 to 10, it will return 10
   */
  <T> T getDomainMax();

  /**
   *
   * @param <T>
   * @return the min generated value of the domain
   * If the collection generator generates values from 0 to 10, it will return 0
   */
  <T> T getDomainMin();


  /**
   *
   * @param step - the value of a unit
   * @return
   */
  CollectionGenerator<T> step(Integer step);


}
