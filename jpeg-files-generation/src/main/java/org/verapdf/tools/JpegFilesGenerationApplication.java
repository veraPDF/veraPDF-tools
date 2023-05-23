package org.verapdf.tools;

import com.github.jaiimageio.jpeg2000.J2KImageWriteParam;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.io.*;

public class JpegFilesGenerationApplication {

	public static void main(String[] args) throws IOException {
		BufferedImage image = ImageIO.read(new File(args[0]));

		File myFile = new File(args[1]);
		PDDocument doc = PDDocument.load(myFile);

		PDPage page = doc.getPage(0);
		PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND,
		                                                            false);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		encodeImageToJPEGStream(image, 1, byteArrayOutputStream);

		PDImageXObject pdImage = new PDImageXObject(doc, new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
		                                            COSName.JPX_DECODE, image.getWidth(), image.getHeight(),
		                                            image.getColorModel().getComponentSize(0),
		                                            getColorSpaceFromAWT(image));
		contentStream.drawImage(pdImage, 0, 0);

		contentStream.close();
		doc.save("result.pdf");
		doc.close();
	}

	private static PDColorSpace getColorSpaceFromAWT(BufferedImage awtImage) { // returns a PDColorSpace for a given BufferedImage
		if (awtImage.getColorModel().getNumComponents() == 1) {
			return PDDeviceGray.INSTANCE; // 256 color (gray) JPEG
		}
		ColorSpace awtColorSpace = awtImage.getColorModel().getColorSpace();
		if (awtColorSpace instanceof ICC_ColorSpace && !awtColorSpace.isCS_sRGB()) {
			throw new UnsupportedOperationException("ICC color spaces not implemented");
		}

		switch (awtColorSpace.getType()) {
			case ColorSpace.TYPE_RGB:
				return PDDeviceRGB.INSTANCE;
			case ColorSpace.TYPE_GRAY:
				return PDDeviceGray.INSTANCE;
			case ColorSpace.TYPE_CMYK:
				return PDDeviceCMYK.INSTANCE;
			default:
				throw new UnsupportedOperationException("color space not implemented: " + awtColorSpace.getType());
		}
	}

	private static void encodeImageToJPEGStream(BufferedImage image, float quality, OutputStream out) throws IOException {
		ImageOutputStream ios = null; // encode to JPEG
		ImageWriter imageWriter = null;
		try {
			imageWriter = ImageIO.getImageWritersBySuffix("jp2").next(); // find JAI writer
			ios = ImageIO.createImageOutputStream(out);
			imageWriter.setOutput(ios);
			// add compression
			J2KImageWriteParam param = (J2KImageWriteParam) imageWriter.getDefaultWriteParam();
			param.setSOP(true);
			param.setEPH(true);
			param.setWriteCodeStreamOnly(true);
			if (quality == 1.0f) {
				param.setLossless(true);
				//param.setFilter(J2KImageWriteParam.FILTER_53);
			} else {
				param.setProgressionType("res");
				param.setCompressionMode(J2KImageWriteParam.MODE_EXPLICIT);
				param.setCompressionType("JPEG2000");
				param.setLossless(false);
				param.setCompressionQuality(quality);
				param.setEncodingRate(1.01);
				param.setFilter(J2KImageWriteParam.FILTER_97);
			}
			ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(image);
			IIOMetadata data = imageWriter.getDefaultImageMetadata(imageTypeSpecifier, param);
			imageWriter.write(data, new IIOImage(image, null, null), param); // write
		} finally {
			IOUtils.closeQuietly(out); // clean up
			if (ios != null) {
				ios.close();
			}
			if (imageWriter != null) {
				imageWriter.dispose();
			}
		}
	}
}
