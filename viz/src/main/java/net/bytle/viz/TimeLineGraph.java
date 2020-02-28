package net.bytle.viz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimeLineGraph {


  /**
   * Time in
   * https://github.com/chubin/wttr.in/tree/master#data-rich-output-format
   * is implemented here
   * https://github.com/chubin/wttr.in/blob/aa3736bb4cc7160b58202fd6d28621a46229dbe2/lib/spark.py#L207
   */
  public static void drawTimeGraph() {


    int heightY = 10; // number of data line
    double cycleX = 4; // number of cycle
    int dayNumbersX = 3;
    int numberOfUnitOnX = 24 * dayNumbersX;
    String dateLine = IntStream.range(0, dayNumbersX)
      .mapToObj(i ->"       "+LocalDate.now().plusDays(i).toString()+"       ")
      .collect(Collectors.joining(""));
    List<Integer> data = IntStream.range(0, numberOfUnitOnX)
      .mapToDouble(i -> heightY/2+ heightY/2 * Math.sin(i * ((Math.PI * cycleX) / numberOfUnitOnX)))
      .mapToObj(i->Double.valueOf(i).intValue())
      .collect(Collectors.toList());

    // Cross join
    Map<Integer, String> dataLines = new HashMap<>();
    // X
    data.forEach(d->{
      // Y
      IntStream.rangeClosed(0,heightY)
        .forEach(line->{
          String value = " ";
          if (line == d){
            value = "x";
          }
          dataLines.merge(line,value,String::concat);
        });
    });


    String tickLine = IntStream.range(0, dayNumbersX)
      .mapToObj(i -> "                       ╷")
      .collect(Collectors.joining(""));
    String scaleLine = IntStream.range(0, dayNumbersX)
      .mapToObj(i -> "─────┴─────┼─────┴─────╂")
      .collect(Collectors.joining(""));
    String scaleNumberLine = IntStream.range(0, dayNumbersX)
      .mapToObj(i -> "     6    12     18     ")
      .collect(Collectors.joining(""));
    System.out.println(dateLine);
    System.out.println(tickLine);
    // Print
    new ArrayList<>(dataLines.keySet())
      .stream()
      .forEachOrdered(e-> System.out.println(dataLines.get(e)));
    System.out.println(scaleLine);
    System.out.println(scaleNumberLine);


  }


}

