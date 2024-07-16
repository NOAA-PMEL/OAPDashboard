/**
 * 
 */
package gov.noaa.pmel.dashboard.server.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class CommandRunner {

    private static Logger logger = Logging.getLogger(CommandRunner.class);
    
    private String command;
	private int exitValue;
	private Process process;
	private StreamGobbler outputGobbler;
	private StreamGobbler errorGobbler;
	private OutputStream outputStream;
	private OutputStream errorStream;
	private File outputFile;
	private File errorFile;

	private static File DEV_NULL = new File("/dev/null");
	
	@Override
	public String toString() {
		return "\""+command + "\", " + outputFile + ", " + errorFile;
	}
	
	public CommandRunner(String command) {
		this.command = command;
	}
	
	public CommandRunner(String command, File output, File error) {
		this.command = command;
		setOutputFile(output);
		setErrorFile(error);
	}
		
	public CommandRunner(String command, OutputStream output) {
		this.command = command;
		setOutputStream(output);
	}
		
	public CommandRunner(String command, OutputStream output, OutputStream error) {
		this.command = command;
		setOutputStream(output);
		setErrorStream(error);
	}
		
	public void setOutputFile(File file) {
//		logger.debug("output file: "+ file);
		outputFile = file != null ? file : DEV_NULL;
	}
	
	public void setOutputStream(OutputStream os) {
		outputStream = os ;
	}
	
	public void setErrorFile(File file) {
//		logger.debug("error file: "+ file);
		errorFile = file != null ? file : DEV_NULL;
	}
	
	public void setErrorStream(OutputStream os) {
		errorStream = os ;
	}
	
	public int exitValue() {
		return exitValue;
	}
	
	public int runCommand() throws Exception {
        return runCommand((String[])null);
	}
    public int runCommand(String commandString) throws Exception {
        return runCommand(new String[] { commandString });
    }
	public int runCommand(String[] args) throws Exception {
        String[] passedArgs = args != null ? args : new String[0];
		logger.debug("running command \"" + command + "\" with args " + Arrays.asList(passedArgs));
		InputStream pOut = null;
		InputStream pErr = null;
		try {
            String[] preambleCommands = new String[] {
        		"bash",
        		"-c",
        		command
            };
            int nArgs = passedArgs.length;
    		String[] commandArgs = new String[nArgs+preambleCommands.length];
            int i = 0;
            for (String preCmd : preambleCommands) {
                commandArgs[i] = preambleCommands[i];
                i++;
            }
            
    		System.arraycopy(passedArgs, 0, commandArgs, i, passedArgs.length);
    		Runtime rt = Runtime.getRuntime();
    		process = rt.exec(commandArgs); 
    		pOut = process.getInputStream();
    		try ( OutputStream out = outputStream != null ? outputStream : new FileOutputStream(outputFile != null ? outputFile : DEV_NULL);
    		      OutputStream err = errorStream != null ? errorStream : new FileOutputStream(errorFile != null ? errorFile : DEV_NULL);) {
        		outputGobbler = new StreamGobbler(pOut, "stdout", out);
        		outputGobbler.start();
        		pErr = process.getErrorStream();
        		errorGobbler = new StreamGobbler(pErr, "stderr", err);
        		errorGobbler.start();
        		exitValue = process.waitFor();
    		}
		} catch (Throwable t) {
		    t.printStackTrace();
		} finally {
			try { 
				outputGobbler.requestStop();
				errorGobbler.requestStop();
				Thread.sleep(250);
				outputGobbler.close();
				errorGobbler.close();
				if ( pOut != null ) pOut.close(); 
				if ( pErr != null ) pErr.close(); 
			}
			catch (Exception ex) { System.out.println(ex);/* ignore */ }
		}
		return exitValue;
	}
		
	/**
     * @param envp
     * @return
     */
    private static String[] asProperties(Map<String, String> envp) {
        String[] props = new String[envp.size()];
        int idx = 0;
        for (String key : envp.keySet()) {
            String val = envp.get(key);
            props[idx++] = key+"="+val ;
        }
        return props;
    }

    public File getErrorFile() {
		return errorFile;
	}
	
	public File getOutputFile() {
		return outputFile;
	}
	
	public StreamGobbler getOutputStream() {
		return outputGobbler;
	}
	
	public StreamGobbler getErrorStream() {
		return errorGobbler;
	}
	
	public Process getProcess() {
		return process;
	}
	
	public int getExitValue() {
		return exitValue;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
            PropertyConfigurator.configure("/Users/kamb/oxy-work/tws_util_oad/log4j.properties");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String[] cargs = new String[] { "-jar", "notFound.jar" };
			CommandRunner sr = new CommandRunner("java" /* | grep ssh" */, baos);
			int result = sr.runCommand(cargs);
            String output = new String(baos.toByteArray());
			System.out.println("result: " + result);
            System.out.println("Output:\n" + output);
		} catch (Exception e) {
//			logger.warn(e, e);
            e.printStackTrace();
		}
	}
}
