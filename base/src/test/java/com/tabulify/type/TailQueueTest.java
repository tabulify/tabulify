package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

public class TailQueueTest {

  @Test
  public void baseTest() {
    int maxSize = 10;
    TailQueue<Integer> q =  new TailQueue<>(maxSize);
    int maxInsert = 20;
    IntStream.range(0, maxInsert+1).forEach(q::add);
    Assert.assertEquals("The size of the queue is equal to max size", maxSize, q.size());
    Assert.assertEquals("The head element should be the good one", (Integer) (maxInsert - maxSize + 1), q.peek());
    Assert.assertEquals("The last element should be the good one", (Integer) maxInsert, q.getLast());
  }

}
