package com.tabulify.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.StringWriter;

public class JsonToYamlToXmlAndViceVersaTest {

  @Test
  public void YamlToJsonTest() throws IOException {

    // Jackson is best known for its ability to serialize POJOs into structural object and back
    // https://stackoverflow.com/questions/23744216/how-do-i-convert-from-yaml-to-json-in-java

    // Jackson use org.yaml.snakeyaml
    String yaml = "name: nico";
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    Object yamlObject = yamlMapper.readValue(yaml, Object.class);

    ObjectMapper jsonMapper = new ObjectMapper();
    String json = jsonMapper.writeValueAsString(yamlObject);
    System.out.println(json);

  }

  @Test
  public void yamlToXml() throws IOException, XMLStreamException {


    // Jackson is best known for its ability to serialize POJOs into structural object and back
    // https://stackoverflow.com/questions/23744216/how-do-i-convert-from-yaml-to-json-in-java

    // Jackson use org.yaml.snakeyaml
    String yaml = "name: nico";
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    Object yamlObject = yamlMapper.readValue(yaml, Object.class);

    // https://github.com/FasterXML/jackson-dataformat-xml#usage
    XmlMapper xmlMapper = XmlMapper.builder()
      .defaultUseWrapper(false)
      // enable/disable Features, change AnnotationIntrospector
      .build();

    StringWriter stringWriter = new StringWriter();
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
    xmlStreamWriter.writeStartDocument();
    xmlStreamWriter.writeStartElement("root");
    xmlMapper.writeValue(xmlStreamWriter, yamlObject);
    xmlStreamWriter.writeComment("Some insightful commentary here");
    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndDocument();
    System.out.println(stringWriter);

  }

}
