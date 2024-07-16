/*
 * StreamGobbler.java
 *
 * Created on February 21, 2006, 12:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gov.noaa.pmel.dashboard.server.process;

//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

/**
 *
 * @author cmoore
 */
public class StreamGobbler extends Thread implements Closeable {
    
	private static Logger logger = Logger.getLogger(StreamGobbler.class);
	
    PrintWriter pw = null;
    InputStream is;
    String type;
    OutputStream os;
    boolean outputOccurred = false;
    EventListenerList listenerList = new EventListenerList();
    String errorString=""; // outputString, 
    boolean stopRequested = false;
    boolean stillReading = true;
    
    /** Creates a new instance of StreamGobbler */
    public StreamGobbler(InputStream is, String type) {
        this(is, type, null);
    }
    
    public StreamGobbler(InputStream is, String type, OutputStream redirect) {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    public void run() {
        try ( InputStreamReader isr = new InputStreamReader(is);
              BufferedReader br = new BufferedReader(isr); ) {
            if (os != null)
                pw = new PrintWriter(os);
            
            String line=null;
            while ( (line = br.readLine()) != null && !stopRequested) {
                if (pw != null)
                    pw.println(line);
//                if (line != null)
//                    if (line.indexOf(outputString) >= 0)
//                        fireActionPerformed(line);
                errorString = line;
                logger.info(type + ">" + line);
                outputOccurred = true;
            }
            stillReading = false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.info(type + "> error in StreamGobbler (runtime process probably terminated)");
        } finally {
            if (pw != null) {
                pw.flush();
            }
        }
    }
    
    public void requestStop() {
        stopRequested = true;
    }
    
//    // to notify that a string occurs
//    public void addActionListener(ActionListener l, String listenString) {
//        listenerList.add(ActionListener.class, l);
//    }
//    
//    public void removeActionListener(ActionListener l) {
//        listenerList.remove(ActionListener.class, l);
//    }
//    
//    protected void fireActionPerformed(String s) {
//        // Guaranteed to return a non-null array
//        Object[] listeners = listenerList.getListenerList();
//        ActionEvent e = null;
//        // Process the listeners last to first, notifying
//        // those that are interested in this event
//        for (int i = listeners.length-2; i>=0; i-=2) {
//            if (listeners[i]==ActionListener.class) {
//                // Lazily create the event:
//                if (e == null) {
//                    String actionCommand = s;
//                    e = new ActionEvent(this,
//                            ActionEvent.ACTION_PERFORMED,
//                            actionCommand);
//                }
//                ((ActionListener)listeners[i+1]).actionPerformed(e);
//            }
//        }
//    }

	@Override
	public void close() throws IOException {
		if ( pw != null && stillReading ) { System.out.println("still reading"); pw.flush(); }
		if ( is != null ) is.close();
		if ( os != null ) os.close();
	}

	public boolean hasOutputOccurred() {
		return this.outputOccurred;
	}

//	public String getOutputString() {
//		return this.outputString;
//	}

	public String getErrorString() {
		return this.errorString;
	}
}
