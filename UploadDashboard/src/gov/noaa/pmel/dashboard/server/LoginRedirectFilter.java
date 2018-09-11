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
		logger.warn("LoginRedirectFilter:" + filterConfig);
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
		logger.debug("target:" + target + ", referer: " + referer + ", content: " + contentType);
        if (target.indexOf("DashboardServices") > 0 &&
           ( "GET".equals(method) || contentType == null || referer.indexOf("dashboardlogin") > 0 )) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
//                String loadPage = referer.indexOf("Popup") > 0 ? "/dashboardloginPopupClose.html" : "/OAPUploadDashboard.html";
//                String mainPage = request.getContextPath()+loadPage;
//                response.sendRedirect(mainPage);
                return;
//            } else if ( contentType == null || referer.indexOf("dashboardlogin") > 0 ) {
//                response.setStatus(HttpServletResponse.SC_CONFLICT);
//                response.getOutputStream().write("TRY AGAIN".getBytes());
//                response.flushBuffer();
//                return;
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
