package com.tabulify.type;

import junit.framework.TestCase;
import org.assertj.core.api.Assertions;

import java.io.IOException;

public class GzipTest extends TestCase {


    public void testBasic() throws IOException {
        String halloWorld = "HalloWorld";
        byte[] compress = Gzip.compress(halloWorld);
        String base64 = Base64Utility.bytesToBase64String(compress);
        Assertions.assertThat(base64).isEqualTo("H4sIAAAAAAAA//NIzMnJD88vykkBAHVdmyoKAAAA");
        String result = Gzip.decompress(compress);
        Assertions.assertThat(result).isEqualTo(halloWorld);
    }

}
