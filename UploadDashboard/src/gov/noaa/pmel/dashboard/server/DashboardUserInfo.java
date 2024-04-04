/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.util.HashSet;
import java.util.Set;

import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * User authentication and privileges
 * 
 * @author Karl Smith
 */
public class DashboardUserInfo {

	private String username;
    private Set<String> userRoles;
    
	// Is this user an admin?
	private boolean admin;

	/**
	 * Creates a user with no group memberships; thus,
	 * can only work with cruises this user uploaded.
	 * 
	 * @param username
	 * 		username to use
	 * @throws IllegalArgumentException
	 * 		if username is invalid (null or too short)
	 */
	public DashboardUserInfo(String username) throws IllegalArgumentException {
		if ( (username == null) || (username.trim().length() < 3) ) // XXX TODO:  Why?!? // Changed to 3- LK
			throw new IllegalArgumentException("User name too short");
		this.username = DashboardUtils.cleanUsername(username);
        userRoles = new HashSet<>();
		admin = false;
	}

	/**
	 * Add privileges to this user from the roles specified
	 * @param rolesString
	 * 		comma/semicolon/space separated list of user roles
	 * @throws IllegalArgumentException
	 * 		if a role cannot be interpreted
	 */
	public void addUserRoles(String rolesString) throws IllegalArgumentException {
        if ( StringUtils.emptyOrNull(rolesString)) { 
            throw new IllegalArgumentException("Empty or null roles string.");
        }
		String[] roles = rolesString.split("[,;\\s]+", -1);
        for (String role : roles) {
            role = role.trim();
            // in case there are eg commas and spaces.
            if ( ! StringUtils.emptyOrNull(role)) {
                userRoles.add(role);
            }
        }
        admin = userRoles.contains("dashboardadmin");
	}

	/**
	 * @return
     * Currently not supporting groups.
	 * 		true is this user is an admin or a manager of a group
	 * 		(regardless of whether there is anyone else in the group)
	 */
	public boolean isManager() {
		if ( admin ) // || (managerNums.size() > 0) )
			return true;
		return false;
	}

	/**
	 * @return
	 * 		true is this user is an admin
	 */
	public boolean isAdmin() {
		return admin;
	}

	/**
	 * Determines if this user has manager privilege over a user. 
	 * This can be from this user being an administrator, a manager
	 * of a group the other user belongs to, or actually being the
	 * same user (same username) as the other user.
	 * 
	 * @param other
	 * 		the other user info; can be null
	 * @return
	 * 		true if this user has manager privilege over the other user;
	 * 		if other is null, returns true.
	 */
	public boolean managesOver(DashboardUserInfo other) {
		// Admin manages over everyone
		if ( admin )
			return true;
		if ( other == null )
			return true;
		// User manages over himself
		if ( username.equals(other.username) )
			return true;
		// Check groups this user manages  // XXX Currently not supporting groups
		// Not a manager over other
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = Boolean.valueOf(admin).hashCode();
		result = result * prime + username.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;

		if ( ! (obj instanceof DashboardUserInfo) )
			return false;
		DashboardUserInfo other = (DashboardUserInfo) obj;

		if ( admin != other.admin )
			return false;
		if ( ! username.equals(other.username) )
			return false;

		return true;
	}

	@Override
	public String toString() {
		return "DashboardUserInfo" +
				"[ username=" + username + 
				", admin=" + Boolean.toString(admin) + 
				"]";
	}

}
