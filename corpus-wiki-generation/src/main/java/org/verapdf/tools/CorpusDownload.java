package org.verapdf.tools;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

//code from integration-tests
public class CorpusDownload {

	public static File createTempFileFromCorpus(final URL downloadLoc, final String prefix) throws IOException {
		File tempFile = File.createTempFile(prefix, ".zip");
		System.out.println("Downloading: " + downloadLoc + ", to temp:" + tempFile);
		int totalBytes = 0;
		try (OutputStream output = new FileOutputStream(tempFile);
			 InputStream corpusInput = handleRedirects(downloadLoc)) {
			byte[] buffer = new byte[8 * 1024];
			int bytesRead;
			while ((bytesRead = corpusInput.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
				totalBytes += bytesRead;
			}
		}
		System.out.println("Downloaded: " + totalBytes + " bytes");
		tempFile.deleteOnExit();
		return tempFile;
	}

	static InputStream handleRedirects(URL url) throws IOException {
		if (!url.getProtocol().startsWith("http")) {
			return url.openStream();
		}
		System.err.println("Prot:" + url.getProtocol());
		URL resourceUrl;
		URL base;
		URL next;
		Map<String, Integer> visited;
		HttpURLConnection conn;
		String location;
		String urlString = url.toExternalForm();
		int times;

		visited = new HashMap<>();

		while (true) {
			times = visited.compute(urlString, (key, count) -> count == null ? 1 : count + 1);

			if (times > 3)
				throw new IOException("Stuck in redirect loop");

			resourceUrl = new URL(urlString);
			conn = (HttpURLConnection) resourceUrl.openConnection();

			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);
			conn.setInstanceFollowRedirects(false); // Make the logic below easier to detect redirections
			conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

			switch (conn.getResponseCode()) {
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
					location = conn.getHeaderField("Location");
					location = URLDecoder.decode(location, "UTF-8");
					base = new URL(urlString);
					next = new URL(base, location); // Deal with relative URLs
					urlString = next.toExternalForm();
					continue;
			}

			break;
		}

		return conn.getInputStream();
	}
}
