package com.tabulify.html;

import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class HtmlSelectStream extends SelectStreamAbs implements SelectStream {


  private final Elements allTrRowElements;
  private int index;
  private ArrayList<Object> actualRow;

  public HtmlSelectStream(HtmlDataPath htmlDataPath, Elements trElements) {

    super(htmlDataPath);
    this.allTrRowElements = trElements;
    this.index = -1;

  }

  @Override
  public HtmlDataPath getDataPath() {
    return (HtmlDataPath) super.getDataPath();
  }

  @Override
  public boolean next() {

    /**
     * Next
     */
    this.index++;
    this.actualRow = new ArrayList<>();


    /**
     * Processing
     */
    Element actualTrElement;
    try {
      actualTrElement = this.allTrRowElements.get(this.index);
    } catch (IndexOutOfBoundsException e) {
      return false;
    }


    String cellSelector = this.getDataPath().getCellSelectorOrDefault();
    for (Element element : actualTrElement.select(cellSelector)) {
      this.actualRow.add(element.text());
    }


    return true;

  }

  @Override
  public void close() {

  }

  @Override
  public long getRow() {
    return this.index + 1;
  }

  @Override
  public Object getObject(int columnIndex) {
    try {
      return this.actualRow.get(columnIndex-1);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public void beforeFirst() {
    this.index = -1;
  }

}
