package de.arvida.warcmock.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jwat.common.HeaderLine;

/**
 * An HTTP proxy for WARC files.
 * Main servlet.
 * 
 * @author aharth
 */
public class ProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	Logger _log = Logger.getLogger(WarcStore.class.getName());


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServletContext ctx = getServletContext();

		WarcStore ws = (WarcStore)ctx.getAttribute(WarcListener.WARC);

		// maybe strip out #
		URI uri;
		try {
			uri = new URI(req.getRequestURL().toString());
		} catch (URISyntaxException e) {
			throw new ServletException(e);
		}
		
		if (uri.toString().equals("http://toc/")) {
			resp.setContentType("application/rdf+xml");
			
			PrintWriter pw = resp.getWriter();
			
			pw.println("<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>");
			pw.println("<rdf:Description rdf:about=''>");

			for (URI u: ws.getKeys()) {
				pw.println("\t<rdfs:seeAlso rdf:resource=\"" + u.toString().replace("&", "&amp;") + "\"/>");
			}
			pw.println("</rdf:Description>");
			pw.println("</rdf:RDF>");
		} else if (ws.contains(uri)) {
			_log.log(Level.INFO, "Serving {0}", uri);
			for (HeaderLine hl : ws.getHeaders(uri)) {
				resp.setHeader(hl.name, hl.value);
			}

			resp.setStatus(ws.getStatus(uri));
			
			int time = ws.getResponseTime(uri);
			if (time > 0) {
				_log.log(Level.INFO, "Delaying {0} ms", time);
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					_log.log(Level.INFO, "{0}:{1}", new Object[] { e.getClass(), e.getMessage() } );
				}
			}

			OutputStream os = resp.getOutputStream();
			byte[] ba = ws.getContent(uri);
			
			ByteArrayInputStream buffer = new ByteArrayInputStream(ba);
			
			GZIPInputStream gzip = new GZIPInputStream(buffer);

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = gzip.read(data, 0, data.length)) != -1) {
				os.write(data, 0, nRead);
			}
			
			gzip.close();

			os.flush();
			os.close();
		} else {
			_log.log(Level.INFO, "Not found {0}", uri);
			resp.setStatus(404);			
		}
	}
}
