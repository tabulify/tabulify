package com.tabulify.tpc;


import io.airlift.tpch.Customer;
import io.airlift.tpch.CustomerGenerator;
import io.airlift.tpch.GenerateUtils;
import io.airlift.tpch.TpchTable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.airlift.tpch.GenerateUtils.calculateRowCount;
import static io.airlift.tpch.GenerateUtils.calculateStartIndex;

public class tpch {


    public static void main(String[] args) throws IOException {


        printSchema();
    }

    private static void RowCountStartIndexPartitioninUtil() {
        int scaleBase = 10;
        double scaleFactor = 1;
        int part = 1;
        int partCount = 1;
        long rowCount = GenerateUtils.calculateRowCount(scaleBase, scaleFactor, part, partCount);


        long startIndex = GenerateUtils.calculateStartIndex(scaleBase, scaleFactor, part, partCount);
        System.out.printf("Row Count: %s\n", rowCount);
        System.out.printf("Start Index: %s\n", startIndex);
    }


    private static void generateCustomerData() throws IOException {
        int scaleFactor = 1; // 1GB
        int part = 1, partCount = 1;

        assert scaleFactor > 0.0D : "scaleFactor must be greater than 0";
        assert part >= 1 : "part must be at least 1";
        assert part <= partCount : "part must be less than or equal to part count";


        Path tempFile = Files.createTempFile("bytle", "_tpch.txt");
        Writer writer = new FileWriter(tempFile.toFile());
        for (Customer entity : new CustomerGenerator(scaleFactor, part, partCount)) {
            writer.write(entity.toLine());
            writer.write('\n');
        }
        System.out.println("Done. File generated at " + tempFile.toAbsolutePath().toString());
    }

    /**
     * Print the TPCH schema
     */
    private static void printSchema() {
        TpchTable.getTables().forEach(table -> {
            System.out.printf("\nTable %s\n", table.getTableName().toUpperCase());
            System.out.println("Name, Simplified Name, Type, Precision, Scale".toUpperCase());
            table.getColumns().forEach(column -> {
                System.out.printf("%s, %s, %s, %s, %s\n",
                        column,
                        column.getSimplifiedColumnName(),
                        column.getType().getBase(),
                        column.getType().getPrecision(),
                        column.getType().getScale()
                );
            });
            // table.getColumnOf(column.getSimplifiedColumnName()) = column = table.getColumnOf(column.getColumnName())
        });
    }


}
