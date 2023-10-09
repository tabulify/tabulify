package net.bytle.dmarc;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.Gzip;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DmarcManager {


  private static final String ZIP_CONTENT_TYPE = "application/zip";
  private static final String GZIP_CONTENT_TYPE = "application/gzip";

  /**
   * Parse the <a href="https://forwardemail.net/en/faq#do-you-support-webhooks">forwardEmail json data</a> into
   * {@link DmarcReport}
   *
   * @param forwardEmailJsonObject - A Json Object with the forwardEmail structure
   * @return the Dmarc Report
   * @throws IllegalStructure if any error
   */
  public static DmarcReport getDmarcReportFromEmail(JsonObject forwardEmailJsonObject) throws IllegalStructure {

    JsonArray jsonArrayAttachements = forwardEmailJsonObject.getJsonArray("attachments");
    if (jsonArrayAttachements == null) {
      throw new IllegalStructure("No attachements");
    }

    for (int i = 0; i < jsonArrayAttachements.size(); i++) {

      JsonObject attachement = jsonArrayAttachements.getJsonObject(i);
      String contentType = attachement.getString("contentType");
      if (!(contentType.equals(ZIP_CONTENT_TYPE) || contentType.equals(GZIP_CONTENT_TYPE))) {
        throw new IllegalStructure("Attachement is not a zip file");
      }
      JsonObject contentObject = attachement.getJsonObject("content");
      String contentObjectType = contentObject.getString("type");
      if (!contentObjectType.equals("Buffer")) {
        throw new IllegalStructure("Attachement type is not buffer");
      }

      JsonArray data = contentObject.getJsonArray("data");
      byte[] bytes = new byte[data.size()];
      for (int j = 0; j < data.size(); j++) {
        bytes[j] = data.getInteger(j).byteValue();
      }

      String xmlString = null;
      String xmlFileName = null;
      switch (contentType) {
        case ZIP_CONTENT_TYPE:
          try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry localFileHeader;
            while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
              if (localFileHeader.isDirectory()) {
                continue;
              }

              ArrayList<Byte> fileAttachementBytes = new ArrayList<>();
              int readLen;
              while ((readLen = zipInputStream.read()) != -1) {
                fileAttachementBytes.add((byte) readLen);
              }
              byte[] fileAttachementBytesArray = new byte[fileAttachementBytes.size()];
              for (int k = 0; k < fileAttachementBytes.size(); k++) {
                fileAttachementBytesArray[k] = fileAttachementBytes.get(k);
              }

              xmlString = new String(fileAttachementBytesArray, StandardCharsets.UTF_8);
              xmlFileName = localFileHeader.getName();

            }
          } catch (IOException e) {
            throw new IllegalStructure("Unable to read the zip data attachment", e);
          }
          break;
        case GZIP_CONTENT_TYPE:
          try {
            xmlFileName = attachement.getString("filename");
            xmlString = Gzip.decompress(bytes);
          } catch (IOException e) {
            throw new IllegalStructure("Unable to read the gzip data attachment", e);
          }
          break;
        default:
          throw new IllegalStructure("Attachement Content Type (" + contentType + ") not supported");
      }

      try {
        return DmarcReport.create(xmlFileName, xmlString);
      } catch (XMLStreamException | IOException e) {
        throw new IllegalStructure("Unable to create the Dmarc Report", e);
      }


    }

    throw new IllegalStructure("No file found in the zip attachement.");

  }

}
