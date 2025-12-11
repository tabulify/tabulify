package com.tabulify.type.env;

import com.tabulify.text.dotenv.DotEnv;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DotEnvTest {


  @Test
  public void base() {

    // not called .env because they are ignored
    Path env = Paths.get("src", "test", "resources", "type", "env");
    DotEnv dotEnv = DotEnv.createFromPath(env);
    Map<String, String> all = dotEnv.getAll();
    Assert.assertEquals(2, all.size());
    Assert.assertEquals("true", dotEnv.get("SMTP_TLS"));

  }
}
