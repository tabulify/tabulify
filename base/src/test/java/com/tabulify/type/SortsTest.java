package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SortsTest {

  @Test
  void baselineNaturalSort() {

    String[] toSort = {"img12.png", "img10.png", "img2.png", "img1.png"};
    List<String> expectedNaturalOrder = Arrays.asList("img1.png", "img2.png", "img10.png", "img12.png");

    Assertions.assertNotEquals(
      expectedNaturalOrder,
      Stream.of(toSort)
        .sorted()
        .collect(Collectors.toList()),
      "Not a natural sort");

    Assertions.assertEquals(expectedNaturalOrder, Stream.of(toSort)
        .sorted(Sorts::naturalSortComparator)
        .collect(Collectors.toList()),
      "Natural Sorted"
    );



  }
}
