package de.arvida.warcmock.webapp;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Provide access to the WarcStore.
 * 
 * @author aharth
 *
 */
public class WarcListener implements ServletContextListener {
	WarcStore _warc;
	
	public static final String WARC = "warc";

	public WarcListener() {
		_warc = new WarcStore();
	}

	public WarcListener(WarcStore warc) {
		_warc = warc;
	}

	public void contextInitialized(ServletContextEvent event) {
		ServletContext ctx = event.getServletContext();

		String fname = ctx.getInitParameter("warcfile");
		if (fname != null) {
			try {
				_warc.readWarc(fname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ctx.setAttribute(WARC, _warc);
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		;
	}
}