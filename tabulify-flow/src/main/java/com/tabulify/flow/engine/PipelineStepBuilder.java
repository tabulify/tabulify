package com.tabulify.flow.engine;


import com.tabulify.Tabular;
import net.bytle.type.KeyNormalizer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * A pipeline step builder
 */
public abstract class PipelineStepBuilder extends ExecutionNodeBuilder {


  private static final List<PipelineStepBuilder> operationProviders = new ArrayList<>();


  static {

    // Load operations provider
    ServiceLoader<PipelineStepBuilder> loadedTableSystemProviders =
      ServiceLoader
        .load(PipelineStepBuilder.class, ClassLoader.getSystemClassLoader());

    // ServiceConfigurationError may be thrown here
    for (PipelineStepBuilder provider : loadedTableSystemProviders) {
      operationProviders.add(provider);
    }

  }

  private PipelineBuilder pipelineBuilder;

  @Override
  public PipelineStepBuilder setNodeName(String nodeName) {
    return (PipelineStepBuilder) super.setNodeName(nodeName);
  }


  // lock using when loading providers
  private static final Object lock = new Object();

  // installed providers
  private static volatile List<PipelineStepBuilder> installedStructProviders;

  // Used to avoid recursive loading of installed providers
  private static boolean loadingProviders = false;
  private Pipeline pipeline;

  // A step may accept many operations
  // This is the operation accepted (ie written in the yaml file)
  private KeyNormalizer acceptedOperation;
  /**
   * The position in the pipeline list
   */
  private int pipelineStepId;


  private static Void checkPermission() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkPermission(new RuntimePermission(PipelineStepBuilder.class.getSimpleName()));
    return null;
  }

  private PipelineStepBuilder(Void ignore) {
  }


  /**
   * Initializes a new instance of this class.
   * <p/>
   * <p> During construction a provider may safely access files associated
   * with the default provider but care needs to be taken to avoid circular
   * loading of other installed providers. If circular loading of installed
   * providers is detected then an unspecified error is thrown.
   *
   * @throws SecurityException If a security manager has been installed and it denies
   *                           {@link RuntimePermission}<tt>("fileSystemProvider")</tt>
   */
  public PipelineStepBuilder() {
    this(checkPermission());
    this.buildArgumentAttributesFromClass();
  }


  // loads all installed providers
  private static List<PipelineStepBuilder> loadInstalledProviders() {

    return operationProviders;
  }

  /**
   * Returns a list of the installed work providers.
   * <p/>
   * <p> The first invocation of this method loads any installed
   * providers that will be used by the provider Factory class.
   *
   * @return An unmodifiable list of the installed service providers.
   * @throws ServiceConfigurationError When an error occurs while loading a service provider
   */
  public static List<PipelineStepBuilder> installedProviders() {
    if (installedStructProviders == null) {

      synchronized (lock) {
        if (installedStructProviders == null) {
          if (loadingProviders) {
            throw new Error("Circular loading of installed providers detected");
          }
          loadingProviders = true;

          List<PipelineStepBuilder> list = AccessController
            .doPrivileged((PrivilegedAction<List<PipelineStepBuilder>>) PipelineStepBuilder::loadInstalledProviders);

          installedStructProviders = Collections.unmodifiableList(list);
        }
      }
    }
    return installedStructProviders;
  }

  /**
   * This is used by the {@link PipelineStepBuilder}
   * to match an operation name to an object
   *
   * @return true if the step accepts the operation name
   */
  public Boolean acceptOperation(KeyNormalizer operationName) {
    return getAcceptedCommandNames().contains(operationName);
  }

  /**
   * @return the accepted operation names
   */
  public Set<KeyNormalizer> getAcceptedCommandNames() {
    return Set.of(getOperationName());
  }

  /**
   * This is used by the {@link PipelineStepBuilder}
   * to create an operation object when {@link #acceptOperation(KeyNormalizer)} match
   * <p>
   * Create a new instance of the operation
   */
  public abstract PipelineStepBuilder createStepBuilder();


  /**
   * The final build function
   */
  public abstract PipelineStep build();


  public PipelineStepBuilder setPipeline(Pipeline pipeline) {
    this.pipeline = pipeline;
    return this;
  }

  /**
   * The name of the node
   * must be unique in the set of operation
   * May be called by log as it's part of the toString and should be not null
   *
   * @return the operation name
   */
  public abstract KeyNormalizer getOperationName();


  @Override
  public String toString() {
    return this.getOperationName().toCliLongOptionName() + " (" + super.toString() + ")";
  }

  public PipelineStepBuilder setAcceptedOperation(KeyNormalizer actualOperation) {
    this.acceptedOperation = actualOperation;
    return this;
  }

  public Pipeline getPipeline() {
    return this.pipeline;
  }

  /**
   * @param stepSequenceId - the position in the pipeline list
   */
  public PipelineStepBuilder setPipelineStepId(int stepSequenceId) {
    this.pipelineStepId = stepSequenceId;
    return this;
  }

  /**
   * @return the position in the pipeline list
   */
  public int getPipelineStepId() {
    return this.pipelineStepId;
  }

  @Override
  public Tabular getTabular() {
    Tabular tabular = super.getTabular();
    if (tabular == null) {
      return this.pipeline.getTabular();
    }
    return tabular;
  }

  public PipelineStepBuilder setPipelineBuilder(PipelineBuilder pipelineBuilder) {
    this.pipelineBuilder = pipelineBuilder;
    this.setTabular(pipelineBuilder.getTabular());
    return this;
  }

  protected PipelineBuilder getPipelineBuilder() {
    return this.pipelineBuilder;
  }
}
