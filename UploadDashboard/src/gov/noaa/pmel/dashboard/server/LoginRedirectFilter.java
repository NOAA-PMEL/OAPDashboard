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
public class LoginRedirectFilter implements Filter {

	private static Logger logger = LogManager.getLogger(LoginRedirectFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("LoginRedirectFilter init:" + filterConfig);
		logger.debug("LoginRedirectFilter:" + filterConfig);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, 
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		logger.debug(request);
		
        String target = request.getRequestURL().toString();
        String referer = request.getHeader("Referer");
        String contentType = request.getHeader("Content-Type");
        String method = request.getMethod();
		logger.debug(method + ": target:" + target + ", referer: " + referer + ", content: " + contentType);
        if (( target.indexOf("DashboardServices") > 0 || 
              target.indexOf("DataUploadService") > 0 ||
              target.indexOf("SessionServices") > 0 ) &&
            ( "GET".equals(method) || contentType == null || referer.indexOf("dashboardlogin") > 0 )) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            logger.debug("j_security_check bogus GET request.  Sending NO_CONTENT");
            return;
        } else {
    		// All is well - continue on
    		chain.doFilter(request, response);
        }
	}

	@Override
	public void destroy() { 
        // do nothing
	}
}
