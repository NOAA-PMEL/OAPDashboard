package gov.noaa.pmel.dashboard.server;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.shared.DashboardUtils;


/**
 * Authenticates a user for a session
 */
public class AuthenticateFilter implements Filter {

	private static Logger logger = LogManager.getLogger(AuthenticateFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("AuthFilter init:" + filterConfig);
		logger.warn("AuthFilter:" + filterConfig);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, 
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		System.out.println("AuthFilter filter:" + request);
		logger.warn("AuthFilter:" + request);
		
		HttpSession session = request.getSession(false);
		if ( session == null ) {
			// Session has been invalidated -
			// unlikely to be seen here since a new session will have been created
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
					"invalid session - refresh page to login again");
			return;
		}
		if ( session.isNew() ) {
			// New session - temporary redirect to the login page
			response.sendRedirect("dashboardlogin.html");
			return;
		}

		// Check for the username used by the dashboard service methods
		String username;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch ( Exception ex ) {
			// No user principal or name in old session - unexpected
			username = "";
		}
		if ( username.isEmpty() ) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
					"unexpected missing user name - refresh page to login again");
			return;
		}

		// All is well - continue on
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() { 
	}

}
