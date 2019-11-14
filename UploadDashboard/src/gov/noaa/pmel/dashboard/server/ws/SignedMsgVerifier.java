/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import gov.noaa.pmel.tws.auth.HttpRequestValidator;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.ConfigurationException;
import gov.noaa.pmel.tws.util.Logging;
import gov.noaa.pmel.tws.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * @author kamb
 *
 */
public class SignedMsgVerifier implements Filter {

	private static Logger logger;
	
	@Override
	public void destroy() { // nothing to do
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
		HttpServletRequest hReq = (HttpServletRequest)request;
		HttpServletResponse hResp = (HttpServletResponse)response;
		logger.debug("verify message: " + hReq.getRequestURL().toString());
		try {
			HttpRequestValidator.authenticateRequest(hReq);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			logger.warn(e, e);
			hResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn(e, e);
			hResp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		chain.doFilter(hReq, response);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
        System.out.println("OAPDashboardWS: verifier init");
        String envConfig = System.getProperty("oap.config.dir");
        System.out.println("ArgoWS: Environment-specified config dir: " + envConfig);
        try {
            if ( StringUtils.emptyOrNull(envConfig)) {
                System.out.println("OAPDashboardWS: using default config dir.");
                ApplicationConfiguration.Initialize("oap");
            } else {
                File configDir = new File(envConfig);
                System.out.println("OAPDashboardWS: using specified configuration dir: " + configDir.getAbsolutePath());
                if ( !configDir.exists()) { throw new IllegalStateException("OAPDashboardWS: configuration dir does not exist! Exiting."); }
                if ( !configDir.canRead()) { throw new IllegalStateException("OAPDashboardWS: unable to read configuration dir! Exiting."); }
                ApplicationConfiguration.Initialize(configDir, "oap");
            }
            logger = Logging.getLogger(SignedMsgVerifier.class);
    		logger.debug("init");
            Logging.showLogFiles(logger);
        } catch (ConfigurationException ex) {
            ex.printStackTrace();
        }
        if ( !HttpRequestValidator.checkConfiguration()) {
            System.err.println("HttpRequestValidator configuration problem.");
            throw new ServletException("Problem with HttpRequestValidator");
        }
	}

}
