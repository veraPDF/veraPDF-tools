package org.verapdf.tools.classification;

import org.verapdf.report.XsltTransformer;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.*;

/**
 * @author Maksim Bezrukov
 */
final class CliProcessor {
	private static final String xslExt = ".xsl"; //$NON-NLS-1$
	private static final String stylesheet = "classification" + xslExt; //$NON-NLS-1$

	private final CliArgParser args;

	private CliProcessor() throws IOException {
		this(new CliArgParser());
	}

	private CliProcessor(final CliArgParser args) throws IOException {
		this.args = args;
	}

	static CliProcessor createProcessorFromArgs(final CliArgParser args)
			throws IOException {
		return new CliProcessor(args);
	}

	void processPaths(final List<String> pdfPaths) {
		List<File> files = new ArrayList<>();
		for (String pdfPath : pdfPaths) {
			File file = new File(pdfPath);
			if (file.isDirectory()) {
				processDir(file, files);
			} else {
				processFile(file, files);
			}
		}
		try {
			processFiles(files);
		} catch (FileNotFoundException | TransformerException e) {
			e.printStackTrace();
		}
	}

	private void processDir(final File dir, final List<File> files) {
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				int extIndex = file.getName().lastIndexOf(".");
				String ext = file.getName().substring(extIndex + 1);
				if ("xml".equalsIgnoreCase(ext)) {
					processFile(file, files);
				}
			} else if (file.isDirectory()) {
				processDir(file, files);
			}
		}
	}

	private void processFile(final File pdfFile, final List<File> files) {
		if (checkFileCanBeProcessed(pdfFile)) {
			files.add(pdfFile);
		}
	}

	private static boolean checkFileCanBeProcessed(final File file) {
		if (!file.isFile()) {
			System.err.println("Path " + file.getAbsolutePath() + " is not an existing file.");
			return false;
		} else if (!file.canRead()) {
			System.err.println("Path " + file.getAbsolutePath() + " is not a readable file.");
			return false;
		}
		return true;
	}

	private void processFiles(final List<File> toProcess) throws FileNotFoundException, TransformerException {
		if (toProcess.isEmpty()) {
			return;
		}
		File file = toProcess.get(0);
		InputStream source = new FileInputStream(file);
		File xslt = new File(args.xslt());
		InputStream xsltIS = new FileInputStream(xslt);
		Map<String, String> arguments = new HashMap<>();
		String version = args.version();
		arguments.put("version", version);
		Calendar date = new GregorianCalendar();
		String dateString = String.format("%04d%02d%02d",
				date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
		arguments.put("date",
				dateString);
		File out = new File(dateString + "_veraPDF_" + version + ".txt");
		System.out.println(out.getAbsolutePath());
		OutputStream os = new FileOutputStream(out);
		XsltTransformer.transform(source, xsltIS,
				os, arguments);

	}
}
