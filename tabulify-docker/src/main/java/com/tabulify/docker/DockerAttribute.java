package com.tabulify.docker;

import com.tabulify.service.ServiceAttributeEnum;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * We use the Docker compose service schema
 * <a href="https://docs.docker.com/reference/compose-file/services/">...</a>
 */
public enum DockerAttribute implements ServiceAttributeEnum {

  // https://docs.docker.com/reference/compose-file/services/#image
  // [<registry>/][<project>/]<image>[:<tag>|@<digest>]
  IMAGE("The image", true, null, String.class, null, null),
  // in docker compose, it's a list of string. ie "443:8043"
  // https://docs.docker.com/reference/compose-file/services/#ports
  PORTS("The ports", true, new ArrayList<String>(), List.class, String.class, null),
  // https://docs.docker.com/reference/compose-file/services/#environment
  ENVIRONMENT("The environment attributes", true, new HashMap<String, String>(), Map.class, String.class, String.class),
  // In docker compose, it's a list
  // We support only the short syntax
  // https://docs.docker.com/reference/compose-file/services/#short-syntax-5
  // VOLUME:CONTAINER_PATH:ACCESS_MODE
  // https://docs.docker.com/reference/compose-file/services/#volumes
  VOLUMES("The volumes", true, new ArrayList<String>(), List.class, String.class, null),
  // Command in Exec form
  // https://docs.docker.com/reference/compose-file/services/#command
  // Docker Compose uses a string in the doc but accepts also the exec form syntax
  // https://docs.docker.com/reference/dockerfile/#exec-form
  // We use the exec format because
  // * Docker Api use this form in the function signature
  // * otherwise we would need to parse it, and it's not 1,2,3.
  // Example: /bin/sh -c 'echo "hello $$HOSTNAME"'
  // This command has 2 args
  COMMAND("The command", true, new ArrayList<>(), List.class, String.class, null);

  private final String description;
  private final Class<?> valueClazz;
  private final boolean isParameter;
  private final Object defaultValue;
  private final Class<?> collectionElementClazz;
  private final Class<?> collectionValueClazz;
  private final KeyNormalizer keyNormalizer;

  DockerAttribute(String description, boolean isParameter, Object defaultValue, Class<?> valueClazz, Class<?> collectionElementClazz, Class<?> collectionValueClass) {
    this.description = description;
    this.valueClazz = valueClazz;
    this.isParameter = isParameter;
    this.defaultValue = defaultValue;
    this.collectionElementClazz = collectionElementClazz;
    this.collectionValueClazz = collectionValueClass;
    this.keyNormalizer = KeyNormalizer.createSafe(this.name());
  }

  @Override
  public boolean isParameter() {
    return isParameter;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return valueClazz;
  }

  @Override
  public Class<?> getCollectionElementClazz() {
    return this.collectionElementClazz;
  }

  @Override
  public Class<?> getCollectionValueClazz() {
    return this.collectionValueClazz;
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return this.keyNormalizer;
  }
}
