package net.bytle.vertx.analytics.sink;

public class AnalyticsSqlSink {

  /**
   * Event Table
   * Example: MixPanel:
   * https://docs.mixpanel.com/docs/tracking-methods/data-warehouse/sending-events
   * The following columns should be mapped from your warehouse table:
   * Name	Type	Description
   * Event Name	STRING	The name of the event that occurred. This can be specified as a table column that contains the event name, or as a static value (one event name for the whole table).
   * Event Time	TIMESTAMP	The time when an event occurred.
   * Distinct ID	STRING, INT	The identifier of the user that performed the event. Learn more about identifying users.
   * JSON Properties Optional	JSON	If your table stores data in a JSON column, you may chose to ingest that data as event properties. If there is a conflict between the name of a table column and one of the keys in the JSON column, the table column will take precedence.
   */

}
