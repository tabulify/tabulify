package net.bytle.vertx.db;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The inputs properties for the pagination of a list of objects
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JdbcPagination {


  /**
   * The search term to use against a unique key
   */
  private String searchTerm;
  /**
   * The page id starting at 1 (not zero)
   */
  private Long pageId;

  /**
   * The page size (ie the numbers of row returned)
   */
  private Long pageSize;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public JdbcPagination() {
  }

  @JsonProperty("searchTerm")
  public String getSearchTerm() {
    return searchTerm;
  }

  @SuppressWarnings("unused")
  public void setSearchTerm(String searchTerm) {
    this.searchTerm = searchTerm;
  }

  public void setPageId(Long pageId) {
    this.pageId = pageId;
  }

  @JsonProperty("pageId")
  public Long getPageId() {
    return pageId;
  }

  public void setPageSize(Long pageSize) {
    this.pageSize = pageSize;
  }

  @JsonProperty("pageSize")
  public Long getPageSize() {
    return pageSize;
  }

}
