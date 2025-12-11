package com.tabulify.type.env;

import com.tabulify.exception.BadExitStatusException;
import com.tabulify.text.envrc.Envrc;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class EnvrcTest {


    @Test
    public void base() throws BadExitStatusException {

        Path env = Paths.get("src", "test", "resources", "type", ".envrc");

        Map<String, String> all = Envrc.exec(env, Map.of());
        Assert.assertEquals(1, all.size());
        Assert.assertEquals("bar", all.get("FOO"));

    }

}
