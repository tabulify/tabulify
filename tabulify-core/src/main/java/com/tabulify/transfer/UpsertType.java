package com.tabulify.transfer;

import net.bytle.type.KeyInterface;

public enum UpsertType implements KeyInterface {

  /**
   * Upsert is done via one statement (Generally a merge,
   * but it can be an insert on conflict
   */
  MERGE,
  /**
   * When the target has no unique constraint
   */
  INSERT,
  /**
   * Upsert is done via 2 statement (Insert if error, update)
   */
  INSERT_UPDATE,
  /**
   * Upsert is done via 2 statement (Update if error, insert)
   */
  UPDATE_INSERT;

  @Override
  public String toString() {
    return toKeyNormalizer().toCliLongOptionName();
  }

}
