package net.bytle.db.spi;

import net.bytle.db.model.RelationDef;

import java.util.List;

public abstract class DataPath implements Comparable<DataPath> {


    public abstract TableSystem getDataSystem();

    public abstract RelationDef getDataDef();

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
