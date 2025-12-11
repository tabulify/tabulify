package com.tabulify.service;

import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.conf.*;
import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.service.ServiceAttributeBase.IS_STARTED;

/**
 * A service is an abstraction to start and stop a service
 * It was created at first to start and stop docker container
 */
public abstract class Service implements Comparable<Service> {


  private final Tabular tabular;
  private final KeyNormalizer name;


  Map<ServiceAttributeEnum, Attribute> attributes = new HashMap<>();

  public Service(Tabular tabular, Attribute name) {
    this.tabular = tabular;
    this.addAttribute(name);
    this.name = (KeyNormalizer) name.getValueOrDefault();
    this.addAttributesFromEnumAttributeClass(ServiceAttributeBase.class);
    this.getAttribute(IS_STARTED).setValueProvider(this::isStarted);
  }

  public Service addAttribute(ServiceAttributeEnum serviceConnectionAttribute, Origin origin, Object value) {
    attributes.put(serviceConnectionAttribute, this.tabular.getVault().createAttribute(serviceConnectionAttribute, value, origin));
    return this;
  }

  /**
   * Add an attribute from a {@link KeyNormalizer} that comes normally from a file
   * System implementer would override this method to handle the value first
   * and pass it on to the super class if the key is unknown
   */
  public Service addAttribute(KeyNormalizer keyNormalizer, Origin origin, Object value) {
    ServiceAttributeBase connectionAttributeBase;
    try {
      connectionAttributeBase = Casts.cast(keyNormalizer, ServiceAttributeBase.class);
    } catch (CastException e) {
      List<Class<? extends AttributeEnumParameter>> systemConnectionEnumParameters = this.getAttributeEnums().stream().map(enumClass -> (Class<? extends AttributeEnumParameter>) enumClass).collect(Collectors.toList());
      throw new RuntimeException("The service attribute (" + keyNormalizer + ") is unknown for the service " + this + ". We were expecting one of the following: " + tabular.toPublicListOfParameters(systemConnectionEnumParameters), e);
    }
    this.addAttribute(connectionAttributeBase, origin, value);
    return this;
  }

  /**
   * @return the list of enum class
   * Typically, a connection would add its own class
   * This is used to give feedback when an attribute is not recognized when reading a {@link ConfVault config file}
   */
  public List<Class<? extends ServiceAttributeEnum>> getAttributeEnums() {
    return List.of(ServiceAttributeBase.class);
  }

  protected Tabular getTabular() {
    return this.tabular;
  }

  /**
   * A utility class to add the default attribute when a service is build
   *
   * @param enumClass - the class that holds all enum attribute
   * @return the path for chaining
   */
  public Service addAttributesFromEnumAttributeClass(Class<? extends ServiceAttributeEnum> enumClass) {


    Vault vault = this.tabular.getVault();
    for (ServiceAttributeEnum attribute : enumClass.getEnumConstants()) {


      Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

      // Name is constructor variable
      if (attribute == ServiceAttributeBase.NAME) {
        continue;
      }

      // Env
      // We don't look up without the tabli prefix because it can cause clashes
      // for instance, name in os is the name of the computer
      TabularEnvs tabularEnvs = this.tabular.getTabularEnvs();
      KeyNormalizer envName = KeyNormalizer.createSafe(Tabular.TABUL_NAME + "_" + OsEnvType.SERVICE + "_" + this.getName() + "_" + attribute);
      String envValue = tabularEnvs.getOsEnvValue(envName);
      if (envValue != null) {
        com.tabulify.conf.Attribute variable = variableBuilder
          .setOrigin(com.tabulify.conf.Origin.OS)
          .buildSafe(envValue);
        this.addAttribute(variable);
        continue;
      }

      // None
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(Origin.DEFAULT)
        .buildSafe(null);
      this.addAttribute(variable);

    }
    return this;
  }

  public Service addAttribute(Attribute attribute) {
    attributes.put((ServiceAttributeEnum) attribute.getAttributeMetadata(), attribute);
    return this;
  }

  public KeyNormalizer getName() {
    return this.name;
  }

  public String getType() {
    return getAttribute(ServiceAttributeBase.TYPE).getValueOrDefaultAsStringNotNull();
  }

  public Attribute getAttribute(ServiceAttributeEnum serviceAttribute) {
    return this.attributes.get(serviceAttribute);
  }

  /**
   * Start the service
   */
  public abstract void start();

  /**
   * Drop a service
   */
  public abstract void drop();

  /**
   * Check if the service is started
   */
  public abstract boolean isStarted();

  /**
   * Stop the service
   */
  public abstract void stop();

  @Override
  public int compareTo(Service o) {
    return this.getName().compareTo(o.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Service that = (Service) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  public HashSet<Attribute> getAttributes() {
    return new HashSet<>(this.attributes.values());
  }

  public Service addAttribute(String key, Origin origin, Object value) {
    return addAttribute(KeyNormalizer.createSafe(key), origin, value);
  }

  @Override
  public String toString() {
    return name.toString();
  }

  /**
   * @return if the service exists
   */
  public abstract boolean exists();

}
