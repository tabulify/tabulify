package com.tabulify.model;

/**
 * The name and the code are the unique identifier of a type
 * But for database where the name is the identifier,
 * and they don't return deterministically the same jdbc code,
 * you can switch it to NAME
 */
public enum SqlTypeKeyUniqueIdentifier {

  /**
   * The name is sufficient to have a unique type
   */
  NAME_ONLY,
  /**
   * The name and code is the unique key identifier
   */
  NAME_AND_CODE,
  /**
   * Only the code
   */
  CODE_ONLY

}
