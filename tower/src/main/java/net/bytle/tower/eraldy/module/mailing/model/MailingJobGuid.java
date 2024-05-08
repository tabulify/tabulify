package net.bytle.tower.eraldy.module.mailing.model;

import net.bytle.vertx.guid.Guid;

public class MailingJobGuid extends Guid {


  /**
   * The database realm id
   */
  private final long realmId;
  /**
   * The database local id
   */
  private final long jobId;

  public MailingJobGuid(Builder builder) {
    assert builder.jobId != null : "The job id should be not null";
    assert builder.realmId != null : "The realm id should be not null";
    this.realmId = builder.realmId;
    this.jobId = builder.jobId;
  }

  public Long getRealmId() {
    return this.realmId;
  }

  /**
   * @return localId The mailing identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public long getJobId() {

    return this.jobId;
  }


  @Override
  public String toStringLocalIds() {
    return "realmId=" + realmId +
      ", mailingJobId=" + jobId;
  }

  public static class Builder {

    private Long jobId;
    protected Long realmId;

    public Builder setRealmId(Long realmId) {

      this.realmId = realmId;
      return this;

    }

    /**
     * @param jobId The job id in the realm in the database (ie local to the realm)
     */
    public Builder setJobId(Long jobId) {

      this.jobId = jobId;
      return this;
    }

    public MailingJobGuid build() {

      return new MailingJobGuid(this);
    }

  }

}
