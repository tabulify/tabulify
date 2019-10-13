package net.bytle.db.spi;

import net.bytle.db.model.TableDef;

import java.util.List;

/**
 *
 * An object that may be used to locate a data container (such as a file or a table) in a data system (file system, relational database).
 * It will typically represent a system dependent data path.
 *
 */
public abstract class DataPath implements Comparable<DataPath> {


    public abstract TableSystem getDataSystem();

    public abstract TableDef getDataDef();

    public abstract String getName();

    public abstract List<String> getPathSegments();

    private String getId(){
        return "@/"+getDataSystem().getDatabase().getDatabaseName()+"/"+String.join("/",getPathSegments());
    }

    public int compareTo(DataPath o) {
        return this.getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return getId();
    }


}
