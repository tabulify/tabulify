package net.bytle.db.model;

import java.util.*;

public class UniqueKeyDef {

    private final RelationDef relationDef;
    private String name;
    private Map<Integer, ColumnDef> columnDefs = new HashMap<>();

    public static UniqueKeyDef of(RelationDef relationDef) {
        return new UniqueKeyDef(relationDef);
    }

    public String getName() {
        return name;
    }


    private UniqueKeyDef(){

        relationDef = null;
    }

    UniqueKeyDef(RelationDef relationDef) {
        this.relationDef = relationDef;
    }

    public RelationDef getRelationDef() {
        return relationDef;
    }

    public UniqueKeyDef name(String name) {
        this.name = name;
        return this;
    }

    public UniqueKeyDef addColumn(ColumnDef columnDef) {

        return addColumn(columnDef, this.columnDefs.keySet().size()+1);
    }

    public UniqueKeyDef addColumn(ColumnDef columnDef, int colSeq) {

        if(!this.columnDefs.values().contains(columnDef)){

            this.columnDefs.put(colSeq, columnDef);

        }

        return this;

    }

    /**
     * Return the columns sorted by position
     * @return
     */
    public List<ColumnDef> getColumns() {

        List<Integer> positions = new ArrayList<>(columnDefs.keySet());
        Collections.sort(positions);
        List<ColumnDef> columnDefsToReturn = new ArrayList<>();
        for (Integer position:positions){
            columnDefsToReturn.add(columnDefs.get(position));
        }
        return columnDefsToReturn;
    }


    public UniqueKeyDef addColumns(List<ColumnDef>  columnDefs) {

        for (ColumnDef columnDef:columnDefs) {
            addColumn(columnDef);
        }

        return this;

    }
}
