package de.arvida.warcmock;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import de.arvida.warcmock.webapp.WarcStore;

public class WarcStoreTest extends TestCase {
	public void testRead() throws IOException, URISyntaxException {
		WarcStore ws = new WarcStore();
		ws.readWarc("src/test/resources/foaf.warc.gz");

		Set<URI> uris = ws.getKeys();

		// the warc file should contain the uri
		Assert.assertTrue(uris.contains(new URI("http://harth.org/andreas/foaf")));
	}
}