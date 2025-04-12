package com.tabulify.flow.engine;

import java.util.Set;
import java.util.function.Function;

/**
 * A flow operation is a pipeline operation
 * that gets a set of message and send back a set of message
 */
public abstract class FlowStep extends StepAbs implements Function<Set<Message>,Set<Message>> {

}
