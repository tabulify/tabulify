package net.bytle.dag;

import net.bytle.type.MediaType;

import java.util.Set;

public interface Dependency {


  Set<? extends Dependency> getDependencies();

  /**
   * The id that will be used to see if the object are the same
   */
  String getId();


  /**
   *
   * @return the dependency type (table, generator, ...)
   */
  MediaType getMediaType();

}
