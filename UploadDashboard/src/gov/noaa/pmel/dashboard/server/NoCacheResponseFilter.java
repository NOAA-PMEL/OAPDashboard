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
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoCacheResponseFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse);

		logger.debug("NoCacheFilter:" + httpRequest.getRequestURL().toString());
		
		wrapper.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		wrapper.addHeader("Pragma", "no-cache");
		wrapper.addHeader("Expires", "0");
		chain.doFilter(request, wrapper);
	}

	private static Logger logger = LogManager.getLogger(AuthenticateFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("NoCacheFilter init:" + filterConfig);
		logger.debug("NoCacheFilter init:" + filterConfig);
	}

}
