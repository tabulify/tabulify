package net.bytle.db.fs;

import net.bytle.db.csv.CsvDataDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;


import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FsDataPath extends DataPath {


    private final Path path;
    private final FsTableSystem tableSystem;

    public FsDataPath(FsTableSystem fsTableSystem, Path path) {

        this.tableSystem = fsTableSystem;
        this.path = path;

    }

    public static FsDataPath of(FsTableSystem fsTableSystem, Path path) {

        return new FsDataPath(fsTableSystem, path);

    }

    @Override
    public TableSystem getDataSystem() {
        return tableSystem;
    }

    @Override
    public TableDef getDataDef() {

        // The data def of query is build at runtime
        if (super.getDataDef().getColumnDefs().size() == 0) {
            CsvDataDef.build(super.getDataDef());
        }
        return super.getDataDef();

    }

    @Override
    public String getName() {
        return this.path.getFileName().toString();
    }

    @Override
    public List<String> getPathParts() {
        return IntStream.range(0, path.getNameCount() - 1)
                .mapToObj(i->path.getName(i).toString())
                .collect(Collectors.toList());
    }

    public Path getNioPath() {
        return this.path;
    }

    public String getPath() {
        return this.path.toString();
    }
}
