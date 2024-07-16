/**
 * 
 */
package gov.noaa.pmel.dashboard.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.process.CommandRunner;
import gov.noaa.pmel.oads.util.StringUtils;

//import gov.noaa.pmel.dashboard.server.process.StreamGobbler;
//import gov.noaa.pmel.tws.util.process.CommandRunner;

/**
 * 
 */
public class VScanner {
	
	private static final Logger logger = LogManager.getLogger(VScanner.class);

	private static VScanner _instance = null;
	
	private static String CLAM_COMMAND = "clamdscan";
	private static final String MAC_COMMAND = "/opt/homebrew/bin/clamdscan";
	
	public static synchronized VScanner Instance() {
		if ( _instance == null ) {
			_instance = new VScanner();
		}
		return _instance;
	}
	
	private CommandRunner runner = null;
	private String outputString = null;
	private String errorString = null;
	
	public VScanner() {
		String os = System.getProperty("os.name");
		if ( os.contains("Mac")) {
			CLAM_COMMAND = MAC_COMMAND;
		}
		
	}
//	public static boolean ScanFile(File file) throws Exception {
//		return Instance().scanFile(file);
//	}
	
	public boolean scanFilePB(File file) throws Exception {
		if ( ! file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}
//		OutputStream out = new ByteArrayOutputStream();
//		OutputStream err = new ByteArrayOutputStream();
//		String command = CLAM_COMMAND + " --quiet " + file.getAbsolutePath();
		ProcessBuilder pb = new ProcessBuilder();
//		Map<String, String> env = pb.environment();
//		System.out.println(env);
//		String path = env.get("PATH");
//		path += ":/opt/homebrew/bin";
//		env.put("PATH", path);
//		System.out.println(pb.environment());
//		pb.directory(new File(System.getProperty("user.home")));
//		System.out.println("dir: " + pb.directory());
		pb.command(CLAM_COMMAND, "--no-summary", "--fdpass", file.getAbsolutePath());
		Process process = pb.start();
		int exit = process.waitFor();
		return exit != 0;
	}
	private String QUIET_FLAG = " --quiet ";
	private String BE_QUIET = "";
	
	public boolean scanFile(File file) throws Exception {
		return scanFile(file, "");
	}
	public boolean scanFile(File file, String quarantine) throws Exception {
//		return scanFile(file, quarantine, "");
//	}
//	public boolean scanFile(File file, String quarantine, String fileName) throws Exception {
		String quarantineFlag = "";
		if ( ! StringUtils.emptyOrNull(quarantine)) {
			File quarantineDir = new File(quarantine);
			if ( !quarantineDir.exists()) {
				if ( !quarantineDir.mkdirs()) {
					logger.warn("Unable to create non-exising quarantine directory: " + quarantineDir);
				} else {
					quarantineFlag = " --move=" + quarantine + " ";
				}
			} else if ( !quarantineDir.isDirectory() || !quarantineDir.canWrite()) {
				logger.warn("Unable to write to specified quarantine directory: " + quarantineDir);
			} else {
				quarantineFlag = " --move=" + quarantine + " ";
			}
//			if ( !StringUtils.emptyOrNull(fileName)) {
//				File renamedFile = new File(file.getParentFile(), fileName);
//				file.renameTo(renamedFile);
//				file = renamedFile;
//			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		String command = CLAM_COMMAND + BE_QUIET + quarantineFlag + " --fdpass --no-summary " + file.getAbsolutePath();
		runner = new CommandRunner(command, out, err);
		int exit;
		try {
			exit = runner.runCommand();
			outputString = new String(out.toByteArray(), "UTF-8");
			errorString = new String(err.toByteArray(), "UTF-8");
		} catch (Exception ex) {
			throw ex;
		}
		return exit != 0;
	}
		
	private static void usage(Integer exit) {
		System.out.println("usage: VScanner <file>");
		if ( exit != null ) {
			System.exit(exit);
		}
	}
	
	public String getError() {
		return errorString;
	}
	
	public String getVirus() {
		if ( outputString != null ) {
			// logger.warn("No virus scanner output for file: " + file);
			int cindex = outputString.indexOf(':');
			if ( cindex >= 0 ) { 
				String virus = outputString.substring(cindex + 2,
													  outputString.indexOf(" FOUND"));
				return virus;
			}
		}
		return "N/A";
	}
	public String getOutput() {
		return outputString;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ( args.length != 1 ) {
			usage(-1);
		}
		String filename = args[0];
		File file = new File(filename);
		try {
			VScanner scanner = new VScanner();
			long t0 = System.nanoTime() / 100000;
			boolean hasVirus = scanner.scanFile(file);
			long t1 = System.nanoTime() / 100000;
			if ( hasVirus ) {
				String scannerOutput = scanner.getOutput();
				System.err.println("A virus was detected in file : "+ scannerOutput);
			} else {
				System.err.println("No virus detected in file : "+ file.getAbsolutePath());
			}
			long t2 = System.nanoTime() / 100000;
//			System.out.println("t1: " + (t1 - t0) + ", t2: " + (t2-t0));
		} catch (Exception ex) {
			System.err.println("An exception occurred scanning for viruses in file : " + file.getAbsolutePath());
			ex.printStackTrace();
			System.exit(-1);
		}
	}

}
