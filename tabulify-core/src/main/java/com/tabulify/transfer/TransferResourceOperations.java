package com.tabulify.transfer;

import com.tabulify.conf.AttributeValue;

/**
 * Operation:
 * * on the existing target before load (replace, truncate)
 * * on the source after load (drop, truncate)
 */
public enum TransferResourceOperations implements AttributeValue {


  TRUNCATE( "Truncate the source or the existing target", "both"),
  DROP( "Drop the source after the transfer or the existing target before the transfer", "both"),
  CREATE( "Create a target resource if it does not exist. This is by default always on", "target"),
  REPLACE( "Replace the existing target (ie drop, create) - ie replace existing", "target"),
  KEEP("Does not replace the target if it exists", "target"),
  REPLACE_IF_NEWER( "Replace the target if the source is newer", "target"),
  ;


  private final String comment;
  private final String entity; // On which data resource entity can this option be applied (source, target or both)

  TransferResourceOperations( String comment, String entity) {
    this.comment = comment;
    this.entity = entity;
  }


  @Override
  public String getDescription() {
    return this.comment;
  }


  public String getEntity() {
    return entity;
  }

}


