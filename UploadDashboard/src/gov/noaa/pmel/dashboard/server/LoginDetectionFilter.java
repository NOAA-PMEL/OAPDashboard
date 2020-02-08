package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.security.Principal;

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


/**
 * Authenticates a user for a session
 */
public class LoginDetectionFilter implements Filter {

	private static Logger logger = LogManager.getLogger(LoginDetectionFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("LoginDetectionFilter init:" + filterConfig);
		logger.debug("LoginDetectionFilter:" + filterConfig);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, 
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		logger.debug(request);
		
	    Principal user = request.getUserPrincipal();
	    HttpSession session = request.getSession(false);

	    if (user != null && (session == null || session.getAttribute("user") == null)) {
	        request.getSession().setAttribute("user", user);
	        // First-time login. You can do your intercepting thing here.
            logger.info("User " + user + " new login");
	    }

	    chain.doFilter(request, response);	
	}

	@Override
	public void destroy() { 
        // do nothing
	}
}
