/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

/**
 * @author kamb
 *
 */
public class MyWindow {

    protected MyWindow() {
    }

    public static native int screenX() /*-{
        return $wnd.screenX;
    }-*/;
    
    public static native MyWindow open(String url, String name) /*-{
      return $wnd.open(url, name);
    }-*/;

    public static native int screenY() /*-{
        return $wnd.screenY;
    }-*/;
    
    public static native MyWindow open(String url, String name, String options) /*-{
      var w = $wnd.open(url, name, options);
      w.focus();
      return w;
    }-*/;

    public native void setHref(String href) /*-{
      if (this.location) {
        this.location.href = href;
      }
    }-*/;
    
    public native void setFocus() /*-{
        this.focus();
    }-*/;
}
