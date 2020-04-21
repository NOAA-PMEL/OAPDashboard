/**
 * 
 */
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
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class LoginDetectionFilter implements Filter {

    private Logger logger = Logging.getLogger(LoginDetectionFilter.class);

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) 
        throws IOException, ServletException 
    {
        HttpServletRequest request = (HttpServletRequest) req;
        Principal user = request.getUserPrincipal();
        HttpSession session = request.getSession(false);

        if (user != null && (session == null || session.getAttribute("user") == null)) {
            request.getSession().setAttribute("user", user);

            logger.info("Login by " + user);
        }

        chain.doFilter(req, resp);
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }

}
