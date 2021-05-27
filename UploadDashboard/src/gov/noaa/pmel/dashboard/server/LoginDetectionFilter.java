/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.UsersDao;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class LoginDetectionFilter implements Filter {

    private Logger logger = Logging.getLogger(LoginDetectionFilter.class);

    private static Set<String> requiredPasswordChange = new HashSet<>();
    
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
        HttpServletResponse response = (HttpServletResponse) resp;
        Principal principal = request.getUserPrincipal();
        HttpSession session = request.getSession(false);

        logger.trace(request.getRequestURL().toString());
        if (principal != null) {
            String username = principal.getName();
            if (session == null || session.getAttribute("user") == null) {
                request.getSession().setAttribute("user", principal);
                String browser = request.getHeader("User-Agent");
                String msg = "Login by " + principal + " on " + request.getLocalName() + " using " + browser;
                logger.info(msg);
                if ( ApplicationConfiguration.getProperty("oap.login.notify", false)) {
                    sendNotification(msg);
                }
                try {
                    UsersDao udao = DaoFactory.UsersDao();
                    User user = udao.retrieveUser(username);
                    udao.userLogin(user);
                    if ( user.requiresPasswordChange()) {
                        logger.info("change password required for request by " + username + ": "+ request.getRequestURI());
                        synchronized (requiredPasswordChange) {
                            requiredPasswordChange.add(user.username());
                        }
                        response.addCookie(new Cookie("sdisuid", user.requiresPwChange()));
                        response.sendRedirect(getPasswordChangeUrl(request));
                        return;
                    }
                } catch (SQLException ex) {
                    logger.warn(ex,ex);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving user information.");
                    return;
                }
            } else if ( requiredPasswordChange.contains(username) &&
                        shouldBeRedirected(request)) {
                try {
                    logger.info("Redirecting attempted bypass by " + username + 
                                " to " + request.getRequestURL().toString());
                    UsersDao udao = DaoFactory.UsersDao();
                    User user = udao.retrieveUser(username);
                    if ( user.requiresPasswordChange()) {
                        logger.debug("redirecting for request: "+ request);
                        response.addCookie(new Cookie("sdisuid", user.requiresPwChange()));
                        response.sendRedirect(getPasswordChangeUrl(request));
                        return;
                    } else {
                        synchronized (requiredPasswordChange) {
                            logger.info("removeing " + username + " from changePassword cache.");
                            requiredPasswordChange.remove(username);
                        }
                    }
                } catch (SQLException ex) {
                    logger.warn(ex,ex);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving user information.");
                    return;
                }
            } 
        } else if ( request.getRequestURL().toString().contains("DashboardServices")) {
            logger.warn("Null user principle!");
            Notifications.SendEmail("Null user principle", "Null user principle at\n"+String.valueOf(request), "linus.kamb@noaa.gov");
            // We'll come back to this. Want to see if this is common or normal.
//            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No User Principle");
//            return;
        }

        chain.doFilter(req, resp);
    }

    /**
     * @param msg
     */
    private void sendNotification(String msg) {
        Runnable notifier = new Runnable() {
            @Override
            public void run() {
                logger.debug("running login notifier");
                Thread.yield();
                Notifications.SendEmail("SDIS Login", msg, "linus.kamb@noaa.gov,linus.kamb@gmail.com");
            }
        };
        Executors.newSingleThreadExecutor().execute(notifier);
    }

    /**
     * @param request
     * @return
     */
    private boolean shouldBeRedirected(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String remainder = uri.substring(context.length());
        logger.debug("redirect remainder:"+remainder);
        return remainder.isEmpty() || remainder.equals("/") || remainder.contains("Dashboard");
    }

    /**
     * @param request
     * @return
     */
    private String getPasswordChangeUrl(HttpServletRequest request) {
        String root = request.getContextPath(); // request.getServletPath();
        logger.debug("setpass context path:" + root);
        return root + "/setpassword.html";
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }

}
