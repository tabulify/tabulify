# AdHoc Tabulify Run

## About

You can create a class `com.tabulify.tabul.TabulTmpTest`
to run adhoc Tabulify or DocRun command within the test framework.

This file is in the [gitignore](../../.gitignore) file.

## Example

```java
package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.doc.DocExec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TabulTmpTest {

  private Tabular tabular;

  @BeforeEach
  void setUp() {
    tabular = Tabular.tabularWithCleanEnvironment();
  }

  @AfterEach
  void tearDown() {
    tabular.close();
  }


  /**
   * An entry to be able to test whatever
   * command from the doc or from anywhere
   */
  @Test
  public void dynamicTest() {

    Tabul.main(new String[]{
      TabulWords.CONNECTION_COMMAND,
      TabulWords.TYPE_COMMAND,
      "sqlserver",
    });

  }

  /**
   * Doc run
   */
  @Test
  public void docTest() {

    String[] args = {"--ide", "--no-cache", "docs/data_type/data_type"};
    DocExec.main(args);

  }


}
```
