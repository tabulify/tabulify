package com.tabulify.json.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class GsonTest {

  @Test
  public void base() {

    JsonObject responseObj = new JsonObject();
    responseObj.addProperty("userid", "User 1");
    responseObj.addProperty("amount", "24.23");
    responseObj.addProperty("success", "NO");


    JsonObject element = new JsonObject();
    element.addProperty("success", "NO");
    responseObj.add("sub",element);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(responseObj));
  }

  @Test
  public void gsonFromString() {

    String json = "{ \"name\":\"foo\" }";
    Gson gson = new Gson();
    Object jsonObject = gson.fromJson(json, Object.class);
    Assert.assertEquals(com.google.gson.internal.LinkedTreeMap.class, jsonObject.getClass());
    @SuppressWarnings("unchecked") Map.Entry<String, Object> entry = ((LinkedTreeMap<String, Object>) jsonObject).entrySet().iterator().next();
    Assert.assertEquals(entry.getKey(),"name");
    Assert.assertEquals(entry.getValue(),"foo");

  }
}
