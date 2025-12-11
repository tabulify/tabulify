package com.tabulify.flow.engine;


import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
/**
 * Utility class over the graph
 *
 */
public class FlowPrint {


  /**
   * See also for example
   * <a href="https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram">...</a>
   * <a href="https://gitlab.com/barfuin/text-tree/-/blob/master/src/main/java/org/barfuin/texttree/internal/TextTreeImpl.java">...</a>
   */
  public static <V> void print(Graph<V, DefaultEdge> g, Function<V, String> formatter) {
    DOTExporter<V, DefaultEdge> exporter =
      new DOTExporter<>(formatter);
    exporter.setVertexAttributeProvider((v) -> {
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put("label", DefaultAttribute.createAttribute(v.toString()));
      return map;
    });
    Writer writer = new StringWriter();
    exporter.exportGraph(g, writer);
    System.out.println(writer);
  }

}
