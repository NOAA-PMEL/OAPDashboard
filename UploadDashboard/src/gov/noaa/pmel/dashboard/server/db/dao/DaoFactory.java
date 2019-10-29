/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.dao;

import gov.noaa.pmel.dashboard.server.db.myb.dao.MybSubmissionsDao;
import gov.noaa.pmel.dashboard.server.db.myb.dao.MybUsersDao;

/**
 * @author kamb
 *
 */
public class DaoFactory {
	
	public static UsersDao UsersDao() { return new MybUsersDao(); }
	public static SubmissionsDao SubmissionsDao() { return new MybSubmissionsDao(); }
}
