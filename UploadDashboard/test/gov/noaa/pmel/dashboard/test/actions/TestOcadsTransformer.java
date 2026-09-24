package gov.noaa.pmel.dashboard.test.actions;

import java.io.File;

import gov.noaa.pmel.oads.xml.a0_2_2.Transform;

public class TestOcadsTransformer {

	void testTransform(String infilePath, String outfilePath, String obsType) {
		testTransform(new File(infilePath), new File(outfilePath), obsType);
	}
	void testTransform(File infile, File outfile, String obsType) {
		Transform.main(new String[] { "-o", obsType, infile.getPath(), outfile.getPath() });
	}
	public static void main(String[] args) {
		try {
			String contextDir = "content/OAPUploadDashboard/MetadataDocs/";
			String infilePath = contextDir + "BHTE/BHTEFGTK8/BHTEFGTK8_metadata.xml";
			String outfilePath = "BHTEFGTK8_machineUse.xml";
			new TestOcadsTransformer().testTransform(infilePath, outfilePath, "Laboratory experiment");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
