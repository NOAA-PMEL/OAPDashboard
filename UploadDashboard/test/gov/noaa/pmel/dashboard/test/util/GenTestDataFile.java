package gov.noaa.pmel.dashboard.test.util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * #############################################################
 * use mkfile or truncate or dd (search SO for create data file)
 * #############################################################
 */
public class GenTestDataFile {

	static int K = 1024;
	enum MAGNITUDE {
		B(0),
		K(1),
		M(2),
		G(3);
		
		private int exp;
		private MAGNITUDE(int exponent) {
			exp = exponent;
		}
		int exp() {
			return exp;
		}
	}
	static void usage(Integer exit) {
		System.out.println("usage: GenTestDataFile <size> [output_filename]");
		System.out.println("\t- size in bytes (default) or <number>[bkmg] - bytes, KB, MB, GB");
		if ( exit != null ) {
			System.exit(exit.intValue());
		}
	}
	static String sizeRegEx = "(\\d+)([bkmgBKMG]?)";
	
	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			if ( args.length == 0 ) {
				usage(-1);
			}
			String sizeStr = args[0];
			Pattern p = Pattern.compile(sizeRegEx);
			Matcher  match = p.matcher(sizeStr);
			if ( !match.matches()) {
				System.out.println("Invalid size: " + sizeStr);
				usage(-1);
			}
			int sizeNum = Integer.parseInt(match.group(1));
			MAGNITUDE mag = match.group(2).isEmpty() ? 
								MAGNITUDE.valueOf("B") :
								MAGNITUDE.valueOf(match.group(2).toUpperCase());
			int multiplier = (int)Math.pow(K,mag.exp());
			long fileSize = (long)sizeNum * multiplier;
			
			String outputFile = args.length >= 2 ? args[1] : "sysout";
			
			try ( PrintStream out = args.length >= 2 ? 
									new PrintStream(new FileOutputStream(args[1])) :
									System.out; ) {
				
				Random rando = new Random();
				int asciiTop = '~' - '#';
				for (int i = 0; i < fileSize; i++) {
					char c = (char)('#' + rando.nextInt(asciiTop));
					out.write(c);
				}
			}
			long end = System.currentTimeMillis();
			long delta = end - start;
			System.out.println("Wrote " + fileSize + " bytes to " + outputFile + 
								" in " + delta + " ms," +
								" at " + (fileSize / ( delta + 1000000)) + " per sec.");
		} catch (Exception ex) {
			ex.printStackTrace();
			// TODO: handle exception
		}

	}

}
