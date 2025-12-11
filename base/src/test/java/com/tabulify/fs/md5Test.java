package com.tabulify.fs;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class md5Test {

    @Test
    public void md5Test() {
        String md5 = Fs.getMd5(Paths.get("src/test/resources/fs/md5TestFile.txt"));
        Assert.assertEquals("md5 are the same",md5,"1032b2a0ce70cc468cdf0f32939a44ff");
    }
}
