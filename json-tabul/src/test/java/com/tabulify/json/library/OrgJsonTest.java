package com.tabulify.json.library;

import org.json.JSONObject;
import org.junit.Test;

public class OrgJsonTest {

  @Test
  public void name() {
    JSONObject jo = new JSONObject("{ \"abc\" : \"def\" }");
    jo.put("fooooo","bar");
    System.out.println(jo.toString(2));
  }

}
