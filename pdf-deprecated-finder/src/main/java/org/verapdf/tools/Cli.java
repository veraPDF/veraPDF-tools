package org.verapdf.tools;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.verapdf.tools.Utils.nameCheck;

public class Cli {
	private static final String HELP = "Arguments: inputFile";

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println(HELP);
			return;
		}
		int counter = 0;
		Path path = Paths.get(args[0]);
		Path finalPath = Paths.get(System.getProperty("user.dir") + "\\fixed_files");
		if (!Files.exists(finalPath)) {
			Files.createDirectory(Paths.get(String.valueOf(finalPath)));
		}

		List<String> pathes = null;
		try (Stream<Path> subPaths = Files.walk(path)) {
			pathes = subPaths.filter(Files::isRegularFile)
			                 .map(Path::toString)
			                 .collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<File> filesList = new ArrayList<>();
		assert pathes != null;
		for (String str : pathes) {
			if (str.endsWith(".pdf")) {
				filesList.add(new File(str));
			}
		}
		for (File file : filesList) {
			try (PDDocument pdDocument = PDDocument.load(file)) {
				if (pdDocument.isEncrypted()) {
					Utils.removeAllSecurity(pdDocument);
				}
				DeprecatedFeatures deprecatedFeatures = new DeprecatedFeatures();
				deprecatedFinder(pdDocument, deprecatedFeatures);
				if (!deprecatedFeatures.procSet.isEmpty() || !deprecatedFeatures.CIDSet.isEmpty() || !deprecatedFeatures.charSet.isEmpty()
				    || !deprecatedFeatures.name.isEmpty()) {
					System.out.println(file.getPath());
					List<Long> procSet = deprecatedFeatures.procSet;
					if (!procSet.isEmpty()) {
						System.out.println("ProcSet is in these objects: " + procSet);
						removeProcSet(pdDocument, procSet);
					}
					List<Long> CIDSet = deprecatedFeatures.CIDSet;
					if (!CIDSet.isEmpty()) {
						System.out.println("CIDSet is in these objects: " + deprecatedFeatures.CIDSet);
						removeCIDSet(pdDocument, CIDSet);
					}
					List<Long> charSet = deprecatedFeatures.charSet;
					if (!charSet.isEmpty()) {
						System.out.println("CharSet is in these objects: " + charSet);
						removeCharSet(pdDocument, charSet);
					}
					List<Long> name = deprecatedFeatures.name;
					if (!name.isEmpty()) {
						System.out.println("Name is in these objects: " + name);
						removeName(pdDocument, name);

					}
					counter++;
					System.out.println("");
					pdDocument.save(new File(String.valueOf(finalPath), "fix_" + file.getName()));

				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("There is " + counter + " files with deprecated features");
	}


	public static void deprecatedFinder(PDDocument document, DeprecatedFeatures deprecatedFeatures) {
		COSDocument cosDocument = document.getDocument();
		List<COSObject> cosObjects = cosDocument.getObjects();
		for (COSObject object : cosObjects) {
			COSBase baseObject = object.getObject();

			procSetSearch(deprecatedFeatures.getProcSet(), baseObject);
			CIDSetSearch(deprecatedFeatures.getCIDSet(), baseObject);
			charSetSearch(deprecatedFeatures.getCharSet(), baseObject);
			nameSearch(deprecatedFeatures.getName(), baseObject);

		}
	}

	private static void procSetSearch(List<Long> list, COSBase baseObject) {
		if (baseObject instanceof COSStream) {
			COSStream stream = (COSStream) baseObject;
			if (stream.containsKey(COSName.PROC_SET) && !list.contains(stream.getKey().getNumber())) {
				list.add(stream.getKey().getNumber());
			}
			COSDictionary dict = (COSDictionary) stream.getDictionaryObject(COSName.RESOURCES);
			if (dict != null) {
				if (dict.containsKey(COSName.PROC_SET) && !list.contains(stream.getKey().getNumber())) {
					list.add(stream.getKey().getNumber());
				}
			}
		}
		if (baseObject instanceof COSDictionary) {
			COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
			if (dict != null) {
				if (dict.containsKey(COSName.PROC_SET) && !list.contains(baseObject.getKey().getNumber())) {
					list.add(baseObject.getKey().getNumber());
				}
			}
			if (((COSDictionary) baseObject).containsKey(COSName.PROC_SET) &&
			    !list.contains(baseObject.getKey().getNumber())) {
				list.add(baseObject.getKey().getNumber());
			}
		}
	}

	private static void CIDSetSearch(List<Long> list, COSBase baseObject) {
		if (baseObject instanceof COSStream) {
			COSStream stream = (COSStream) baseObject;
			if (stream.containsKey(COSName.CID_SET)) {
				if (!list.contains(stream.getKey().getNumber())) {
					list.add(stream.getKey().getNumber());
				}
			}
			COSDictionary cosDictionary = (COSDictionary) (stream.getDictionaryObject(COSName.RESOURCES));
			if (cosDictionary != null) {
				if (cosDictionary.containsKey(COSName.CID_SET) && !list.contains(stream.getKey().getNumber())) {
					list.add(stream.getKey().getNumber());
				}
			}
		}
		if (baseObject instanceof COSDictionary) {
			COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
			if (dict != null) {
				if (dict.containsKey(COSName.CID_SET) && !list.contains(baseObject.getKey().getNumber())) {
					list.add(baseObject.getKey().getNumber());
				}
			}
			if (((COSDictionary) baseObject).containsKey(COSName.CID_SET)) {
				if (!list.contains(baseObject.getKey().getNumber())) {
					list.add(baseObject.getKey().getNumber());
				}
			}
		}
	}

	private static void charSetSearch(List<Long> list, COSBase baseObject) {
		if (baseObject instanceof COSStream) {
			COSStream stream = (COSStream) baseObject;
			if (stream.containsKey(COSName.CHAR_SET)) {
				if (!list.contains(stream.getKey().getNumber())) {
					list.add(stream.getKey().getNumber());
				}
			}
			COSDictionary cosDictionary = (COSDictionary) (stream.getDictionaryObject(COSName.RESOURCES));
			if (cosDictionary != null) {
				if (cosDictionary.containsKey(COSName.CHAR_SET)) {
					if (!list.contains(stream.getKey().getNumber())) {
						list.add(stream.getKey().getNumber());
					}
				}
			}
		}
		if (baseObject instanceof COSDictionary) {
			COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
			if (dict != null) {
				if (dict.containsKey(COSName.CHAR_SET) && !list.contains(baseObject.getKey().getNumber())) {
					list.add(baseObject.getKey().getNumber());
				}
			}
			COSDictionary cosDictionary = (COSDictionary) baseObject;
			if (cosDictionary.containsKey(COSName.CHAR_SET)) {
				if (!list.contains(baseObject.getKey().getNumber())) {
					list.add(baseObject.getKey().getNumber());
				}
			}
		}
	}

	private static void nameSearch(List<Long> list, COSBase baseObject) {
		if (baseObject instanceof COSStream) {
			COSStream stream = (COSStream) baseObject;
			nameCheck(list, stream);
			COSDictionary cosDictionary = (COSDictionary) (stream.getDictionaryObject(COSName.RESOURCES));
			if (cosDictionary != null) {
				nameCheck(list, cosDictionary);
			}
		}
		if (baseObject instanceof COSDictionary) {
			COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
			if (dict != null) {
				nameCheck(list, dict);
			}
			dict = (COSDictionary) baseObject;
			nameCheck(list, dict);
		}
	}

	public static void removeProcSet(PDDocument document, List<Long> list) {
		COSDocument cosDocument = document.getDocument();
		for (COSObject object : cosDocument.getObjects()) {
			if (list.contains(object.getObjectNumber())) {
				COSBase baseObject = object.getObject();
				if (baseObject instanceof COSStream) {
					COSStream stream = (COSStream) baseObject;
					stream.removeItem(COSName.PROC_SET);
					COSDictionary dict = (COSDictionary) stream.getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						stream.removeItem(COSName.PROC_SET);
					}
				}
				if (baseObject instanceof COSDictionary) {
					((COSDictionary) baseObject).removeItem(COSName.PROC_SET);
					COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						dict.removeItem(COSName.PROC_SET
						               );
					}
				}
			}
		}
	}

	public static void removeCIDSet(PDDocument document, List<Long> list) throws IOException {
		for (PDPage page : document.getPages()) {
			PDResources resources = page.getResources();
			PDFont font;
			for (COSName fontName : resources.getFontNames()) {
				font = resources.getFont(fontName);
				PDFontDescriptor fontDescriptor = font.getFontDescriptor();
				fontDescriptor.getCOSObject().removeItem(COSName.CID_SET);
			}
		}
	}

	public static void removeCharSet(PDDocument document, List<Long> list) throws IOException {
		COSDocument cosDocument = document.getDocument();
		for (COSObject object : cosDocument.getObjects()) {
			if (list.contains(object.getObjectNumber())) {
				COSBase baseObject = object.getObject();
				if (baseObject instanceof COSStream) {
					COSStream stream = (COSStream) baseObject;
					stream.removeItem(COSName.CHAR_SET);
					COSDictionary dict = (COSDictionary) stream.getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						stream.removeItem(COSName.CHAR_SET);
					}
				}
				if (baseObject instanceof COSDictionary) {
					((COSDictionary) baseObject).removeItem(COSName.CHAR_SET);
					COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						dict.removeItem(COSName.CHAR_SET);
					}
				}
			}
		}
	}

	public static void removeName(PDDocument document, List<Long> list) {
		COSDocument cosDocument = document.getDocument();
		for (COSObject object : cosDocument.getObjects()) {
			if (list.contains(object.getObjectNumber())) {
				COSBase baseObject = object.getObject();
				if (baseObject instanceof COSStream) {
					COSStream stream = (COSStream) baseObject;
					stream.removeItem(COSName.NAME);
					COSDictionary dict = (COSDictionary) stream.getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						stream.removeItem(COSName.NAME);
					}
				}
				if (baseObject instanceof COSDictionary) {
					((COSDictionary) baseObject).removeItem(COSName.NAME);
					COSDictionary dict = (COSDictionary) ((COSDictionary) baseObject).getDictionaryObject(COSName.RESOURCES);
					if (dict != null) {
						dict.removeItem(COSName.NAME);
					}
				}
			}
		}
	}
}


