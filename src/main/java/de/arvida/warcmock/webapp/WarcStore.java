package de.arvida.warcmock.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jwat.common.HeaderLine;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 * Main class to access a WARC archive.
 * 
 * @author aharth
 *
 */
public class WarcStore {
	Logger _log = Logger.getLogger(WarcStore.class.getName());

	Map<URI, List<HeaderLine>> _headers;
	Map<URI, Integer> _status;
	Map<URI, byte[]> _content;
	Map<URI, Integer> _time = null;

	public WarcStore() {
		_headers = new HashMap<URI, List<HeaderLine>>();
		_status = new HashMap<URI, Integer>();
		_content = new HashMap<URI, byte[]>();
	}

	public Set<URI> getKeys() {
		return _content.keySet();
	}

	public boolean contains(URI u) {
		return (_headers.containsKey(u) && _content.containsKey(u) && _status.containsKey(u));
	}

	public List<HeaderLine> getHeaders(URI u) {
		return _headers.get(u);
	}

	public int getStatus(URI u) {
		return _status.get(u);
	}
	
	public int getResponseTime(URI u) {
		if (_time != null) {
			return _time.get(u);
		}
		
		return -1;
	}

	public byte[] getContent(URI u) {
		return _content.get(u);
	}
	

	public void readAccessLog(String fname) throws IOException {
		_time = new HashMap<URI, Integer>();

		String pattern = "^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+GET\\s+(\\S+)\\s+.*";
		Pattern p = Pattern.compile(pattern);

		Scanner sc = new Scanner(new File(fname));

		while (sc.hasNext()) {
			String line = sc.nextLine();

			Matcher matcher = p.matcher(line);
			if (!matcher.matches()) {
				_log.log(Level.INFO, "Bad line {0}", line);
				continue;
			}

			try {
				int time = Integer.parseInt(matcher.group(2));
				URI u = new URI(matcher.group(6));
				
				_time.put(u, time);
			} catch (Exception e) {
				_log.log(Level.INFO, "Bad line {0}", line);
			}
		}
	}

	public void readWarc(String fname) throws IOException {
		InputStream in = new GZIPInputStream(new FileInputStream(fname));

		WarcReader warcReader = WarcReaderFactory.getReaderUncompressed(in);
		WarcRecord record = null;
		while ((record = warcReader.getNextRecord()) != null) {
			HeaderLine type = record.getHeader("WARC-Type");
			if (type == null || !type.value.equals("response")) {
				continue;
			}

			URI u = null;
			HeaderLine uri = record.getHeader("WARC-Target-URI");
			if (uri != null) {
				try {
					u = new URI(uri.value);
				} catch (URISyntaxException e) {
					e.printStackTrace();
					continue;
				}
				_log.log(Level.INFO, "Target URI: {0}", u);
			} else {
				continue;
			}

			//		for (HeaderLine hl : record.getHeaderList()) {
			//			System.out.println(hl.name + ": " + hl.value);
			//		}

			HttpHeader header = record.getHttpHeader();
			
			if (header != null) {
				_status.put(u, header.statusCode);
				_headers.put(u, header.getHeaderList());
			} else {
				_log.log(Level.INFO, "HTTP header is null?");
			}

			Payload payload = record.getPayload();

			InputStream pin = payload.getInputStream();
			byte[] content = toGzipByteArray(pin);
			pin.close();

			_content.put(u, content);
		}
		warcReader.close();

	}

	public static byte[] toGzipByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		GZIPOutputStream gzip = new GZIPOutputStream(buffer);
		
		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			gzip.write(data, 0, nRead);
		}

		gzip.flush();
		
		gzip.close();

		return buffer.toByteArray();
	}
}