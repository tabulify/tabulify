package com.tabulify.flow.stream;

import com.tabulify.spi.DataPath;

import java.util.function.Consumer;

/**
 * A function that can be used in a peek operation
 */
public interface PipelinePeekOperation extends Consumer<DataPath> {
}
