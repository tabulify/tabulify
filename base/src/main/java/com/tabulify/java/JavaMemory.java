package com.tabulify.java;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Date;

@SuppressWarnings("unused")
public class JavaMemory {


  static int MB = 1024 * 1024;


  @SuppressWarnings("unused")
  public static void garbageCollect(){
    System.gc();
  }

  public static void printMemoryUsage() {


    Runtime runtime = Runtime.getRuntime();

    System.out.println(new Date() + ": Runtime Memory Mb -> " + runtime.totalMemory() / MB + " - " + runtime.freeMemory() / MB + " = " + (runtime.totalMemory() - runtime.freeMemory()) / MB);

    MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapMemoryUsage = memBean.getHeapMemoryUsage();

    System.out.println("Heap Max:" + heapMemoryUsage.getMax()); // max memory allowed for jvm -Xmx flag (-1 if isn't specified)
    System.out.println("Heap Committed:" + heapMemoryUsage.getCommitted()); // given memory to JVM by OS ( may fail to reach getMax, if there isn't more memory)
    System.out.println("Heap Used:" +heapMemoryUsage.getUsed()); // used now by your heap
    System.out.println("Heap Init:" +heapMemoryUsage.getInit()); // -Xms flag

    // |------------------ max ------------------------| allowed to be occupied by you from OS (less than xmX due to empty survival space)
    // |------------------ committed -------|          | now taken from OS
    // |------------------ used --|                    | used by your heap

  }


}
