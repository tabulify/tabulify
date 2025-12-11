package com.tabulify.json.library;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabulify.fs.Fs;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JacksonJsonTest {

  /**
   * A test on the jackson library to read a com.tabulify.db.json file
   *
   */
  @Test
  public void readSingleJsonTest() throws IOException {

    JsonFactory jsonFactory = new JsonFactory();
    Path jsonFile = Fs.getPathFromResources(JacksonJsonTest.class,"/json/file.json");
    JsonParser jsonParser = jsonFactory.createParser(jsonFile.toFile());

    // Sanity check: verify that we got "Json Object":
    if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
      throw new IOException("Expected data to start with an Object");
    }

    // Iterate over object fields:
    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      String fieldName = jsonParser.getCurrentName();
      // Let's move to value
      jsonParser.nextToken();
      switch (fieldName) {
        case "id":
          System.out.println("Id: " + jsonParser.getLongValue());
          break;
        case "text":
          System.out.println("Text: " + jsonParser.getText());
          break;
        case "fromUserId":
          System.out.println("From User: " + jsonParser.getIntValue());
          break;
        case "toUserId":
          System.out.println("To User: " + jsonParser.getIntValue());
          break;
        case "languageCode":
          System.out.println("Lang: " + jsonParser.getText());
          break;
        default:
          throw new IOException("Unrecognized field '" + fieldName + "'");
      }
    }

    jsonParser.close(); // important to close both parser and underlying File reader

  }

  /**
   * A test on the jackson library to write a jso
   *
   * @throws IOException
   */
  @Test
  public void writeWithGeneratorTest() throws IOException {

    JsonFactory jsonFactory = new JsonFactory();
    Path jsonFile = Files.createTempFile("write", ".com.tabulify.db.json");
    JsonGenerator jsonGenerator = jsonFactory.createGenerator(jsonFile.toFile(), JsonEncoding.UTF8);
    jsonGenerator.useDefaultPrettyPrinter(); // enable indentation

    jsonGenerator.writeStartObject();
    jsonGenerator.writeNumberField("id", 1223);
    jsonGenerator.writeStringField("text", "a text");
    jsonGenerator.writeEndObject();
    jsonGenerator.close();

    System.out.println(new String(Files.readAllBytes(jsonFile)));

  }

  /**
   * A test on the jackson library to read a jsonl
   */
  @Test
  public void readJsonlTest() throws IOException {

    JsonFactory jsonFactory = new JsonFactory();
    Path jsonFile = Fs.getPathFromResources(JacksonJsonTest.class,"/json/file.jsonl");
    Files.newBufferedReader(jsonFile).lines().forEach(
      s -> {
        try {
          JsonParser jsonParser = jsonFactory.createParser(s);
          // Sanity check: verify that we got "Json Object":
          if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
          }

          // Iterate over object fields:
          while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            // In the while, we moved to the field name
            System.out.println(jsonParser.getCurrentName());
            // Let's move to value
            jsonParser.nextToken();
            System.out.println(jsonParser.getText());
          }

          jsonParser.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    );
  }


  /**
   * https://www.twilio.com/blog/java-json-with-jackson
   * @throws JsonProcessingException
   */
  @Test
  public void writeWithJsonTree() throws JsonProcessingException {


    ObjectMapper mapper = new ObjectMapper();

    // create a JSON object
    ObjectNode user = mapper.createObjectNode();

    user.put("id", 1);
    user.put("name", "John Doe");
    user.put("email", "john.doe@example.com");
    user.put("salary", 3545.99);
    user.put("role", "QA Engineer");
    user.put("admin", false);

    // create a child JSON object
    ObjectNode address = mapper.createObjectNode();
    address.put("street", "2389  Radford Street");
    address.put("city", "Horton");
    address.put("state", "KS");
    address.put("zipCode", 66439);

    // append address to user
    user.set("address", address);

    // convert `ObjectNode` to pretty-print JSON
    // without pretty-print, use `user.toString()` method
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);

    // print json
    System.out.println(json);

    // Read it back
    JsonNode jsonNode = mapper.readTree(json);
    System.out.println(jsonNode.get("name"));

  }

}
