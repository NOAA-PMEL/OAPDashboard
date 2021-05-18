/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author kamb
 *
 */
public class MyWindow {

    static int lastX, lastY;
    
//    protected MyWindow() {
//    }
//
    public static native int screenX() /*-{
        return $wnd.screenX;
    }-*/;
    public static native int screenY() /*-{
        return $wnd.screenY;
    }-*/;
    public static native int screenX(JavaScriptObject win) /*-{
        return win.screenX;
    }-*/;
    public static native int screenY(JavaScriptObject win) /*-{
        return win.screenY;
    }-*/;
    
    public static void updatePosition(int winX, int winY) {
        GWT.log("update window to " + winX + ", " + winY);
        lastX = winX;
        lastY = winY;
    }
    
    public static native JavaScriptObject open(String url, String name, String options) /*-{
      var w = $wnd.open(url, name, options);
      w.onbeforeunload = function(e) {
          console.log("onbeforeunload " + w.screenX + ", " + w.screenY);
          @gov.noaa.pmel.dashboard.client.MyWindow::updatePosition(II)(w.screenX, w.screenY);
      };
      w.onunload = function(e) {
          console.log("onunload " + w.screenX + ", " + w.screenY);
          @gov.noaa.pmel.dashboard.client.MyWindow::updatePosition(II)(w.screenX, w.screenY);
      };
      w.focus();
      return w;
    }-*/;

    public native static void setHref(JavaScriptObject win, String href) /*-{
        win.location.href = href;
    }-*/;
    
    public native static void setFocus(JavaScriptObject win) /*-{
        win.focus();
    }-*/;
    
    public native static boolean isOpen(JavaScriptObject win) /*-{
        return ! win.closed;
    }-*/;
    
    public native static void close(JavaScriptObject win) /*-{
        win.close();
    }-*/;
}
