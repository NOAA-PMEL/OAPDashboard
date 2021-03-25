/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.ui;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

import gov.noaa.pmel.dashboard.client.MyHandler;

import java.util.Iterator;

/**
 * <p>
 * A {@link PopupPanel} that wraps its content in a 3x3 grid, which allows users
 * to add rounded corners.
 * </p>
 * 
 * <h3>Setting the Size:</h3>
 * <p>
 * If you set the width or height of the {@link MyDecoratedPopupPanel}, you need
 * to set the height and width of the middleCenter cell to 100% so that the
 * middleCenter cell takes up all of the available space. If you do not set the
 * width and height of the {@link MyDecoratedPopupPanel}, it will wrap its
 * contents tightly.
 * </p>
 * 
 * <pre>
 * .gwt-DecoratedPopupPanel .popupMiddleCenter {
 *   height: 100%;
 *   width: 100%;
 * }
 * </pre>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedPopupPanel { the outside of the popup }</li>
 * <li>.gwt-DecoratedPopupPanel .popupContent { the wrapper around the content }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeft { the top left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeftInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenter { the top center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenterInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRight { the top right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRightInner { the inner element of the
 * cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeft { the middle left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeftInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenter { the middle center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenterInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRight { the middle right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRightInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeft { the bottom left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeftInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenter { the bottom center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenterInner { the inner element of
 * the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRight { the bottom right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRightInner { the inner element of
 * the cell }</li>
 * </ul>
 */
public class MyDecoratedPopupPanel extends PopupPanel {
  private static final String DEFAULT_STYLENAME = "gwt-DecoratedPopupPanel";

  /**
   * The panel used to nine box the contents.
   */
  private DecoratorPanel decPanel;

  /**
   * Creates an empty decorated popup panel. A child widget must be added to it
   * before it is shown.
   */
  public MyDecoratedPopupPanel() {
    this(false);
  }

  /**
   * Creates an empty decorated popup panel, specifying its "auto-hide"
   * property.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   */
  public MyDecoratedPopupPanel(boolean autoHide) {
    this(autoHide, false);
  }

  /**
   * Creates an empty decorated popup panel, specifying its "auto-hide" and
   * "modal" properties.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard or mouse events that do not
   *          target the PopupPanel or its children should be ignored
   */
  public MyDecoratedPopupPanel(boolean autoHide, boolean modal) {
    this(autoHide, modal, "suggestPopup");
  }

  /**
   * Creates an empty decorated popup panel using the specified style names.
   * 
   * @param autoHide <code>true</code> if the popup should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard or mouse events that do not
   *          target the PopupPanel or its children should be ignored
   * @param prefix the prefix applied to child style names
   */
  MyDecoratedPopupPanel(boolean autoHide, boolean modal, String prefix) {
    super(autoHide, modal);
    String[] rowStyles = new String[] {
        prefix + "Top", prefix + "Middle", prefix + "Bottom"};
    decPanel = new DecoratorPanel(rowStyles, 1);
    MyHandler handler = new MyHandler("decPanel domHandler");
    decPanel.addDomHandler(handler, MouseOverEvent.getType());
    decPanel.addDomHandler(handler, MouseMoveEvent.getType());
    decPanel.addAttachHandler(new MyHandler("decPanel attachHandler"));
    decPanel.setStyleName("");
    setStylePrimaryName(DEFAULT_STYLENAME);
    super.setWidget(decPanel);
    setStyleName(getContainerElement(), "popupContent", false);
    setStyleName(decPanel.getContainerElement(), prefix + "Content", true);
  }

  @Override
  public void clear() {
    decPanel.clear();
  }

  @Override
  public Widget getWidget() {
    return decPanel.getWidget();
  }

  @Override
  public Iterator<Widget> iterator() {
    return decPanel.iterator();
  }

  @Override
  public boolean remove(Widget w) {
    return decPanel.remove(w);
  }

  @Override
  public void setWidget(Widget w) {
    decPanel.setWidget(w);
    maybeUpdateSize();
  }
//  /**
//   * We control size by setting our child widget's size. However, if we don't
//   * currently have a child, we record the size the user wanted so that when we
//   * do get a child, we can set it correctly. Until size is explicitly cleared,
//   * any child put into the popup will be given that size.
//   */
//  void maybeUpdateSize() {
//    // For subclasses of PopupPanel, we want the default behavior of setWidth
//    // and setHeight to change the dimensions of PopupPanel's child widget.
//    // We do this because PopupPanel's child widget is the first widget in
//    // the hierarchy which provides structure to the panel. DialogBox is
//    // an example of this. We want to set the dimensions on DialogBox's
//    // FlexTable, which is PopupPanel's child widget. However, it is not
//    // DialogBox's child widget. To make sure that we are actually getting
//    // PopupPanel's child widget, we have to use super.getWidget().
//    Widget w = super.getWidget();
//    if (w != null) {
//      if (desiredHeight != null) {
//        w.setHeight(desiredHeight);
//      }
//      if (desiredWidth != null) {
//        w.setWidth(desiredWidth);
//      }
//    }
//  }

  @Override
  protected void doAttachChildren() {
    // See comment in doDetachChildren for an explanation of this call
    decPanel.onAttach();
  }

  @Override
  protected void doDetachChildren() {
    // We need to detach the decPanel because it is not part of the iterator of
    // Widgets that this class returns (see the iterator() method override).
    // Detaching the decPanel detaches both itself and its children. We do not
    // call super.onDetachChildren() because that would detach the decPanel's
    // children (redundantly) without detaching the decPanel itself.
    // This is similar to a {@link ComplexPanel}, but we do not want to expose
    // the decPanel widget, as its just an internal implementation.
    decPanel.onDetach();
  }

  /**
   * Get a specific Element from the panel.
   * 
   * @param row the row index
   * @param cell the cell index
   * @return the Element at the given row and cell
   */
  protected com.google.gwt.user.client.Element getCellElement(int row, int cell) {
    return DOM.asOld(decPanel.getCellElement(row, cell));
  }
}
