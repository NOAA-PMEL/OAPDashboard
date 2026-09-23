package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.util.Collection;

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

	private static Logger logger = LogManager.getLogger(NoCacheResponseFilter.class);

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	/*
	 * from older SO post (focussed on IE)
	 <meta http-equiv="Cache-Control" content="no-store,no-cache,must-revalidate"> 
	 <meta http-equiv="Pragma" content="no-cache"> 
	 <meta http-equiv="Expires" content="-1"> 
	 
	    Alternatively these can be added as headers directly to the response.
	
	 response.addHeader("Cache-Control", "no-store,no-cache,must-revalidate");
	 response.addHeader("Pragma", "no-cache");
	 response.addHeader("Expires", "-1");
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		if ( dontCache(httpRequest)) {
			HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse);
	
			logger.debug("NoCacheFilter:" + httpRequest.getRequestURL().toString());
			
			wrapper.addHeader("Cache-Control", "no-cache,no-store,must-revalidate,max-age=0");
			wrapper.addHeader("Pragma", "no-cache");
			wrapper.addHeader("Expires", "0");
			httpResponse = wrapper;
		}
		chain.doFilter(request, httpResponse);
//        logger.debug("response headers after all:\n"+dumpHeadersAsString(httpResponse));
//        httpResponse.setHeader("Cache-Control", "no-cache,no-store,must-revalidate,max-age=0");
//        logger.debug("response headers after alles:\n"+dumpHeadersAsString(httpResponse));
	}
    
    private static String dumpHeadersAsString(HttpServletResponse response) {
    	StringBuilder b = new StringBuilder();
        Collection<String>headers = response.getHeaderNames();
        for (String name : headers) {
            String value = response.getHeader(name);
            b.append(name).append(":").append(value).append("\n");
        }
    	return b.toString();
    }


	private static boolean dontCache(HttpServletRequest httpRequest) {
		String something = httpRequest.getRequestURI().toLowerCase();
		String contentType = httpRequest.getHeader("Accept") != null ? httpRequest.getHeader("Accept").toLowerCase() : "";
		boolean dontCache = something.endsWith("nocache.js") || something.endsWith("html");
		logger.debug("dontCache: " + something + " : " + contentType + " : "+dontCache);
		return dontCache;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("NoCacheFilter init:" + filterConfig);
		logger.debug("NoCacheFilter init:" + filterConfig);
	}

}
