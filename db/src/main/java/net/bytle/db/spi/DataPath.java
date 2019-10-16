package net.bytle.db.spi;

import net.bytle.db.model.DataDefs;
import net.bytle.db.model.TableDef;

import java.util.List;

/**
 *
 * An object that may be used to locate a data container (such as a file or a table) in a data system (file system, relational database).
 * It will typically represent a system dependent data path.
 *
 */
public abstract class DataPath implements Comparable<DataPath> {

    private TableDef dataDef;

    public abstract TableSystem getDataSystem();


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


    public TableDef getDataDef() {
        if (this.dataDef == null){
            this.dataDef = TableDef.of(this);
        }
        return dataDef;
    }


}
