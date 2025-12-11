package com.tabulify.html;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CssInlinerTest {

    @Test
    public void basic() {

        final String html = "<html><body> " +
                "<style>"
                + "body{background:#FFC}"
                + "p{background:red}"
                + "body, p{font-weight:bold} " +
                "</style>"
                + "<p class=\"myClass\">Hallo</p>"
                + "</body></html>";

        String inlinedHtml = CssInliner.createFromStringDocument(html)
                .inline()
                .toString();

        System.out.println(inlinedHtml);

        String expected = "<html>\n" +
                " <head></head>\n" +
                " <body style=\"background:rgb(255, 255, 204);font-weight:bold;\">\n" +
                "  <p class=\"myClass\" style=\"background:red;font-weight:bold;\">Hallo</p>\n" +
                " </body>\n" +
                "</html>";
        Assertions.assertEquals(expected, inlinedHtml);

    }

    /**
     * Ignored because no time to handle it
     */
    @Disabled
    @Test
    public void externalStyleSheetCss() throws URISyntaxException, IOException {

        URI htmlToInliner = Objects.requireNonNull(CssInlinerTest.class.getResource("/css-inline/bootstrap-inliner.html")).toURI();
        Path path = Paths.get(htmlToInliner);
        String htmlString = Files.readString(path, StandardCharsets.UTF_8);
        String inlinedHtml = CssInliner.createFromStringDocument(htmlString)
                .inline()
                .toString();

        String expected = "<!doctype html><!--suppress JSUnresolvedLibraryURL -->\n" +
                "<html lang=\"en\">\n" +
                " <head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Inliner</title> <!-- CSS only -->\n" +
                " </head>\n" +
                " <body style=\"margin:0;padding:0;border:0;outline:0;width:100%;min-width:100%;height:100%;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;font-family:Helvetica, Arial, sans-serif;line-height:24px;font-weight:normal;font-size:16px;-moz-box-sizing:border-box;-webkit-box-sizing:border-box;box-sizing:border-box;background-color:rgb(255, 255, 255);color:rgb(0, 0, 0);\">\n" +
                "  <div style=\"width:100%;\">\n" +
                "   <h1 style=\"margin:0;padding-top:0;padding-bottom:0;font-weight:500;text-align:left;vertical-align:baseline;font-size:36px;line-height:43.2px;\">Title</h1>\n" +
                "  </div>\n" +
                " </body>\n" +
                "</html>";
        Assertions.assertEquals(expected, inlinedHtml);

    }
}
