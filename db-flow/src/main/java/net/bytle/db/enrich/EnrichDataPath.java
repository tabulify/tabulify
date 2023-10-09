package net.bytle.db.enrich;

import net.bytle.db.connection.Connection;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.NoParentException;
import net.bytle.type.Attribute;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnrichDataPath extends DataPathAbs {


  static final String POSTFIX_ENRICHED = "_enriched";
  public static final MediaType MEDIA_TYPE_ENRICHED = MediaTypes.createFromMediaTypeNonNullString("tabli/enriched");
  final DataPath wrappedDataPath;

  /**
   * Map the column name to the data path attribute
   */
  Map<String, DataPathAttribute> mapColumnNameToDataPathAttribute = new HashMap<>();


  public EnrichDataPath(Connection connection, String path, DataPath wrappedDataPath) {
    super(connection, path, MEDIA_TYPE_ENRICHED);

    /**
     * First the wrapped path has all function depends on it.
     */
    Objects.requireNonNull(wrappedDataPath, "The wrapped data path should be not null");
    this.wrappedDataPath = wrappedDataPath;
    /**
     * Variable are build when creating the path
     * We can't overwrite {@link DataPathAbs#getOrCreateVariable(Attribute)}
     * We overwrite the variables below with the variables of the wrapped data path
     */
    for (Variable variable : this.wrappedDataPath.getVariables()) {
      this.addVariable(variable);
    }

    /**
     * Second, merge
     */
    this.relationDef = this.createRelationDef().mergeStructWithoutConstraints(wrappedDataPath);





  }


  public static EnrichDataPath create(DataPath dataPath) {
    String path = dataPath.getLogicalName() + POSTFIX_ENRICHED;
    return new EnrichDataPath(dataPath.getConnection().getTabular().getNoOpConnection(), path, dataPath);
  }


  @Override
  public String getName() {

    return this.wrappedDataPath.getLogicalName() + POSTFIX_ENRICHED;

  }

  @Override
  public List<String> getNames() {
    List<String> names = this.wrappedDataPath.getNames();
    names.set(names.size() - 1, getName());
    return names;
  }

  @Override
  public String getRelativePath() {
    return this.wrappedDataPath.getRelativePath();
  }

  @Override
  public DataPath getSibling(String name) {
    return this.wrappedDataPath.getSibling(name);
  }

  @Override
  public DataPath getChild(String name) {
    return this.wrappedDataPath.getChild(name);
  }

  @Override
  public DataPath resolve(String stringPath) {
    return this.wrappedDataPath.resolve(stringPath);
  }

  @Override
  public DataPath getChildAsTabular(String name) {
    return null;
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
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new RuntimeException("You can't insert in an enriched data path. This is an enriched data path of (" + this.wrappedDataPath + ")");
  }

  @Override
  public SelectStream getSelectStream() {
    return new EnrichSelectStream(this);
  }

  @Override
  public DataPath getParent() throws NoParentException {
    return this.wrappedDataPath.getParent();
  }

  public EnrichDataPath addVirtualColumn(String columnName, DataPathAttribute dataPathAttribute) {


    if (this.relationDef.hasColumn(columnName)) {
      throw new IllegalStateException("The column data path attribute (" + columnName + ") exists already in the data resources (" + this.wrappedDataPath + ") and can't be added");
    }
    relationDef.getOrCreateColumn(columnName, String.class)
      .setComment("Enriched column with the data path attribute (" + dataPathAttribute + ") value");
    this.mapColumnNameToDataPathAttribute.put(columnName, dataPathAttribute);
    return this;
  }


  /**
   * It makes no sense to show the data resource uri (ie enrich@noop) to the user
   *
   * @return the data uri
   */
  @Override
  public DataUri toDataUri() {
    return this.wrappedDataPath.toDataUri();
  }

}
