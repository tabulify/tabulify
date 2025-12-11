package com.tabulify.java;

import com.tabulify.exception.NoManifestException;
import com.tabulify.type.MapKeyIndependent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;


public class JarManifest {


  private final MapKeyIndependent<String> attributes = new MapKeyIndependent<>();

  /**
   * @param aClazz the class from which the manifest should be found
   * @throws NoManifestException if the run is outside a jar
   */
  public JarManifest(Class<?> aClazz) throws NoManifestException {

    String name = aClazz.getSimpleName() + ".class";
    URL resourceUrl = aClazz.getResource(name);
    if (resourceUrl == null) {
      throw new NoManifestException();
    }
    String resourceUrlAsString = resourceUrl.toString();
    if (!resourceUrlAsString.startsWith("jar")) {
      // Class not from JAR
      throw new NoManifestException();
    }

    String manifestPath = resourceUrlAsString.substring(0, resourceUrlAsString.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
    URL url;
    try {
      url = new URL(manifestPath);
    } catch (IOException e) {
      throw new InternalError("Error, the manifest path (" + manifestPath + ") is not an URL", e);
    }

    try (InputStream is = url.openStream()) {
      java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
      for (Map.Entry<Object, Object> attribute : manifest.getMainAttributes().entrySet()) {
        attributes.put(String.valueOf(attribute.getKey()), String.valueOf(attribute.getValue()));
      }
      for (Map.Entry<String, Attributes> manifestEntry : manifest.getEntries().entrySet()) {
        Attributes localAttributes = manifestEntry.getValue();
        for (Map.Entry<Object, Object> attribute : localAttributes.entrySet()) {
          attributes.put(String.valueOf(attribute.getKey()), String.valueOf(attribute.getValue()));
        }
      }
    } catch (IOException e) {
      throw new NoManifestException(e.getMessage(), e);
    }

  }

  public static JarManifest createFor(Class<?> aClazz) throws NoManifestException {
    return new JarManifest(aClazz);
  }


  public String getAttribute(JarManifestAttribute manifestAttribute) {
    return attributes.get(manifestAttribute.toString());
  }

  public Map<String, String> getMap() {
    return this.attributes;
  }

  public String getAttributeValue(String key) {
    return this.attributes.get(key);
  }


}
