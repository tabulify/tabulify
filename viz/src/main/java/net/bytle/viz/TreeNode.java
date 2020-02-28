package net.bytle.viz;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
 */
public class TreeNode {

  final String name;
  final List<TreeNode> children;

  public TreeNode(String name, TreeNode... children) {
    this.name = name;
    this.children = Arrays.asList(children);
  }

  public String toString() {
    StringBuilder buffer = new StringBuilder(50);
    print(buffer, "", "");
    return buffer.toString();
  }

  private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
    buffer.append(prefix);
    buffer.append(name);
    buffer.append('\n');
    for (Iterator<TreeNode> it = children.iterator(); it.hasNext(); ) {
      TreeNode next = it.next();
      if (it.hasNext()) {
        next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
      } else {
        next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
      }
    }
  }


}
