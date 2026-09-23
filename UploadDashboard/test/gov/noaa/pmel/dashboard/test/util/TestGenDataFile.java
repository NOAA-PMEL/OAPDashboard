package gov.noaa.pmel.dashboard.test.util;


/*
 * use mkfile or truncate or dd (search SO for create data file)
 */
public class TestGenDataFile {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		GenTestDataFile.main(new String[] { "123" });
		GenTestDataFile.main(new String[] { "123k", "123k.out" });
		GenTestDataFile.main(new String[] { "12M", "12M.out" });
		GenTestDataFile.main(new String[] { "2g", "2g.out" });
		GenTestDataFile.main(new String[] { "123T", "123T.out" });
		long end = System.currentTimeMillis();
		System.out.println("Completed in " + ( end - start ) + " millis.");
	}

}
