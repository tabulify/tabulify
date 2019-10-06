package net.bytle.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by gerard on 05-04-2017.
 */
public class XmlStructure {

    // Level, NodePath, ChildNode
    HashMap<Integer, Set<String>> nodeNamesTree = new HashMap<Integer, Set<String>>();

    // NodePath, childeNode
    HashMap<String, Set<String>> childNodeName = new HashMap<String, Set<String>>();

    // NodePath, Attribute
    HashMap<String, Set<String>> attributeNode = new HashMap<String, Set<String>>();

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    private HierarchicalStreamReader streamReader;

    private XmlStructure(Reader reader)  {

        StaxDriver staxDriver = new StaxDriver();
        streamReader = staxDriver.createReader(reader);

    }

    public static XmlStructure of(Reader reader) {
        return new XmlStructure(reader);
    }


    /**
     * An XML analyze function.
     * Take an XML as input and will reveal all node name within their position in the XML tree
     */
    public void printNodeNames() {
        nodeNamesTree = new HashMap();
        parseNode(0, "/"+streamReader.getNodeName());
        for (Map.Entry<Integer, Set<String>> entry : nodeNamesTree.entrySet()) {
            System.out.println("Level = " + entry.getKey());
            for (String father : entry.getValue()) {
                System.out.print("    - XPath = " + father);
                System.out.print(", ChildNodeNames = " + childNodeName.get(father));
                System.out.println(", AttributeNodeNames = " + attributeNode.get(father));
            }
        }
    }

    /**
     * A recursive function that fill the variable nodeNamesTree
     *
     * @param level      - The level of the XML tree
     * @param nodePath - The node name path of the father
     */
    private void parseNode(Integer level, String nodePath) {


        // The root processing
        if (level == 0) {

            // Get the root attributes
            setAttributes(nodePath);

            // Get the node names by level
            Set<String> nodeNamesByLevel = nodeNamesTree.get(level);
            if (nodeNamesByLevel == null) {
                nodeNamesByLevel = new HashSet<String>();
                nodeNamesTree.put(level, nodeNamesByLevel);
            }
            nodeNamesByLevel.add(nodePath);

        }

        // The child variable
        Set<String> childNodesName = childNodeName.get(nodePath);
        if (childNodesName == null) {
            childNodesName = new HashSet<String>();
            childNodeName.put(nodePath, childNodesName);
        }


        // Get the node names by level
        int childLevel = level + 1;
        Set<String> nodeNamesForChildLevel = nodeNamesTree.get(childLevel);
        if (nodeNamesForChildLevel == null) {
            nodeNamesForChildLevel = new HashSet<String>();
            nodeNamesTree.put(childLevel, nodeNamesForChildLevel);
        }

        // Child processing
        while (streamReader.hasMoreChildren()) {

            streamReader.moveDown();

            // Add the node name in the child list
            String nodeName = streamReader.getNodeName();
            childNodesName.add(nodeName);

            // Fully qualified name
            String fullyQualifiedChildNodePath = nodePath + "/" + nodeName;

            // Add the child to the tree
            nodeNamesForChildLevel.add(fullyQualifiedChildNodePath);

            // Get the attributes
            setAttributes(fullyQualifiedChildNodePath);

            // More Children
            if (streamReader.hasMoreChildren()) {
                parseNode(childLevel, fullyQualifiedChildNodePath);
            } else {
                // Set an instantiated set of string to avoid null
                childNodesName = new HashSet<String>();
                childNodeName.put(fullyQualifiedChildNodePath, childNodesName);
            }

            streamReader.moveUp();
        }






    }

    private void setAttributes(String nodePath) {

        Set<String> attributes = attributeNode.get(nodePath);
        if (attributes == null) {
            attributes = new HashSet<String>();
            attributeNode.put(nodePath, attributes);
        }
        for (int i=0;i<streamReader.getAttributeCount();i++) {
            attributes.add(streamReader.getAttributeName(i));
        }
    }


}

