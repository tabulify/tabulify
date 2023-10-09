package net.bytle.db.gen.generator;

import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.gen.GenRelationDef;
import net.bytle.exception.InternalException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.function.Supplier;

public abstract class CollectionGeneratorAbs<T> implements CollectionGenerator<T>, Supplier<T> {

  public static MediaType MEDIA_TYPE = MediaTypes.createFromMediaTypeNonNullString("tabli/col-gen");

  /**
   * The related column def
   */
  private GenColumnDef genColumnDef;

  /**
   * A shortcut to get the clazz
   */
  protected final Class<T> clazz;


  public CollectionGeneratorAbs(Class<T> clazz) {
    this.clazz = clazz;
  }

  public CollectionGeneratorAbs<T> setColumnDef(GenColumnDef columnDef) {
    this.genColumnDef = columnDef;
    if (this.clazz != columnDef.getClazz()) {
      throw new InternalException("The generator (" + this + ") is coupled to a column (" + columnDef + ") that expects data from the class (" + columnDef.getClazz().getSimpleName() + ") but it generates value from the " + this.clazz.getSimpleName() + " type.");
    }
    return this;
  }

  /**
   * The {@link java.util.function.Supplier supplier interface} has the returned function
   * called {@link Supplier#get()}.
   * An alias method
   */
  @Override
  public T get() {
    return getNewValue();
  }

  @Override
  public String toString() {
    String columnName = this.getColumnDef() == null ? "unknown" : this.getColumnDef().getColumnName();
    return this.getClass().getSimpleName() + " for the column " + columnName;
  }

  @Override
  public GenRelationDef getRelationDef() {
    return this.genColumnDef.getRelationDef();
  }


  @Override
  public GenColumnDef getColumnDef() {
    return this.genColumnDef;
  }

  @Override
  public String getGeneratorType() {
    return this.getClass().getSimpleName().toLowerCase().replace("generator", "");
  }

  @Override
  public String getId() {
    return toString();
  }


  @Override
  public MediaType getMediaType() {
    return MEDIA_TYPE;
  }

}
