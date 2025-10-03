package com.tabulify.gen.flow.enrich;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.connection.Connection;
import com.tabulify.gen.GenRelationDef;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAbs;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.NoParentException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.List;
import java.util.Objects;

public class EnrichDataPath extends DataPathAbs {


  static final String POSTFIX_ENRICHED = "_enriched";
  public static final MediaType MEDIA_TYPE_ENRICHED = MediaTypes.createFromMediaTypeNonNullString("application/vnd.tabulify.enrich");
  final DataPath wrappedDataPath;



  public EnrichDataPath(Connection connection, String path, DataPath wrappedDataPath) {
    super(connection, path, null, MEDIA_TYPE_ENRICHED);

    /**
     * First the wrapped path has all function depends on it.
     */
    Objects.requireNonNull(wrappedDataPath, "The wrapped data path should be not null");
    this.wrappedDataPath = wrappedDataPath;
    /**
     * Variable are build when creating the path
     * We can't overwrite {@link DataPathAbs#getOrCreateVariable(AttributeEnum)}
     * We overwrite the variables below with the variables of the wrapped data path
     */
    for (Attribute attribute : this.wrappedDataPath.getAttributes()) {
      this.addAttribute(attribute);
    }

    /**
     * Second, merge
     */
    this.relationDef = this.createRelationDef();


  }


  public static EnrichDataPath create(DataPath dataPath) {
    String path = dataPath.getLogicalName() + POSTFIX_ENRICHED;
    return new EnrichDataPath(dataPath.getConnection().getTabular().getNoOpConnection(), path, dataPath);
  }


  @Override
  public String getName() {

    return this.wrappedDataPath.getName() + POSTFIX_ENRICHED;

  }

  @Override
  public List<String> getNames() {
    List<String> names = this.wrappedDataPath.getNames();
    names.set(names.size() - 1, getName());
    return names;
  }

  @Override
  public String getCompactPath() {
    return this.wrappedDataPath.getCompactPath();
  }

  @Override
  public DataPath getSibling(String name) {
    return this.wrappedDataPath.getSibling(name);
  }

  @Override
  public DataPath resolve(String name, MediaType mediaType) {
    return this.wrappedDataPath.resolve(name, mediaType);
  }

  @Override
  public DataPath resolve(String stringPath) {
    return this.wrappedDataPath.resolve(stringPath);
  }


  @Override
  public String getAbsolutePath() {
    String absolutePath = this.wrappedDataPath.getAbsolutePath();
    String absoluteWithoutName = absolutePath.substring(0, absolutePath.length() - this.wrappedDataPath.getName().length());
    return absoluteWithoutName + getName();
  }

  @Override
  public Long getSize() {
    return this.wrappedDataPath.getSize();
  }

  @Override
  public Long getCount() {
    return this.wrappedDataPath.getCount();
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    throw new RuntimeException("You can't insert in an enriched data path. This is an enriched data path of (" + this.wrappedDataPath + ")");
  }

  @Override
  public SelectStream getSelectStream() {
    return new EnrichDataPathSelectStream(this);
  }

  @Override
  public DataPath getParent() throws NoParentException {
    return this.wrappedDataPath.getParent();
  }

  @Override
  public boolean hasHeaderInContent() {
    return this.wrappedDataPath.hasHeaderInContent();
  }

  @Override
  public SchemaType getSchemaType() {
    return this.wrappedDataPath.getSchemaType();
  }



  /**
   * It makes no sense to show the data resource uri (ie enrich@noop) to the user
   *
   * @return the data uri
   */
  @Override
  public DataUriNode toDataUri() {
    return this.wrappedDataPath.toDataUri();
  }

  @Override
  public GenRelationDef createRelationDef() {
    GenRelationDef genRelationDef = new GenRelationDef(this);
    // constraints
    for (ColumnDef<?> sourceColumnDef : this.wrappedDataPath.getOrCreateRelationDef().getColumnDefs()) {
      genRelationDef
        .getOrCreateColumn(sourceColumnDef.getColumnName(), sourceColumnDef.getDataType())
        .addDataPathStreamGenerator()
        .getColumnDef()
        .setPrecision(sourceColumnDef.getPrecision())
        .setScale(sourceColumnDef.getScale())
        .setComment(sourceColumnDef.getComment())
        .setAllVariablesFrom(sourceColumnDef);
    }
    // constraints
    genRelationDef.mergeLocalConstraints(this.wrappedDataPath.getOrCreateRelationDef());

    return genRelationDef;
  }

  @Override
  public GenRelationDef getRelationDef() {
    return (GenRelationDef) super.getRelationDef();
  }

  @Override
  public GenRelationDef getOrCreateRelationDef() {
    return (GenRelationDef) super.getOrCreateRelationDef();
  }


}
