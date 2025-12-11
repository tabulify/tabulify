/**
 * A Java stream api interface
 * <p>
 * The stream creation can be started with {@link com.tabulify.flow.stream.PipelineStream#createFrom(com.tabulify.Tabular, PipelineStepSupplierDataPath)}
 * by passing one {@link com.tabulify.flow.engine.PipelineStepSupplierDataPath}
 * <p>
 * <p>
 * Java Stream Api is not really what we need in our flow engine because
 * * we can't group (there is no inverse of flatMap, you need to collect, but it's a terminal operation)
 * * we can't optimize (ie map into a transfer)
 * but it's handy to create Java inline Stream for test
 */
package com.tabulify.flow.stream;

import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
