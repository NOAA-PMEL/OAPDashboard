/**
 * 
 */
package gov.noaa.pmel.dashboard.test.datatype;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class DependentValueConverterTest {

	private static DataFileHandler dataFileHandler;
	private static KnownDataTypes knownDataTypes;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			DashboardConfigStore dcfg = DashboardConfigStore.get(false);
			dataFileHandler = dcfg.getDataFileHandler();
			knownDataTypes = dcfg.getKnownUserDataTypes();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link gov.noaa.pmel.dashboard.datatype.DependentValueConverter#convertValueOf(java.lang.String, int)}.
	 */
	@Test
	public void testConvertValueOfStringInt() {
		String datasetId = "PRISM102011"; // "HOGREEF64W32N";
		DashboardDatasetData ddd = dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
		StdUserDataArray stda = new StdUserDataArray(ddd, knownDataTypes);
		assertNotNull(stda);
	}

}
