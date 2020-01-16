package gov.noaa.pmel.dashboard.test.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DashboardServerUtilsTest.class, DashboardUserInfoTest.class, RowNumSetTest.class })
public class AllTests {

}
