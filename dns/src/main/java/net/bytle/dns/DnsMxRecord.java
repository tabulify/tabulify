package net.bytle.dns;

import net.bytle.type.DnsName;

public interface DnsMxRecord {

  /**
   * The priority of the MX record.
   */
  int getPriority();

  /**
   * The target name of the MX record
   */
  DnsName getTarget();

}
