package com.wirelust.appleworks;

import java.io.File;

import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.element.text.TextPElement;
import org.odftoolkit.odfdom.dom.element.text.TextSpanElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.OdfPageLayoutProperties;
import org.odftoolkit.odfdom.dom.style.props.OdfTextProperties;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeMasterStyles;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes AppleWorks Document objects to ODF (Open Document Format) files.
 *
 * ODF files can then be converted to other formats (PDF, DOCX, HTML) using
 * LibreOffice headless mode:
 *   soffice --headless --convert-to pdf document.odt
 *   soffice --headless --convert-to docx document.odt
 */
public class OdfWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OdfWriter.class);

	// Points per inch (ODF uses inches/cm, AppleWorks uses points)
	private static final double POINTS_PER_INCH = 72.0;

	/**
	 * Converts an AppleWorks Document to ODF format and saves it.
	 *
	 * @param doc The parsed AppleWorks document
	 * @param outputPath Path for the output .odt file
	 * @return true if successful, false otherwise
	 */
	public boolean write(Document doc, String outputPath) {
		if (doc == null) {
			LOGGER.error("Document is null");
			return false;
		}

		if (doc.getType() != Document.TYPE_TEXT) {
			LOGGER.warn("Document type {} ({}) may not convert properly to ODF text document",
					doc.getTypeName(), String.format("0x%04X", doc.getType()));
		}

		try {
			OdfTextDocument odfDoc = OdfTextDocument.newTextDocument();

			// Set page layout (dimensions and margins)
			setupPageLayout(odfDoc, doc);

			// Create font styles if fonts are available
			setupFontStyles(odfDoc, doc);

			// Add content with style runs
			addContent(odfDoc, doc);

			// Save the document
			File outputFile = new File(outputPath);
			if (outputFile.exists()) {
				LOGGER.error("Output file already exists: {}", outputPath);
				odfDoc.close();
				return false;
			}

			odfDoc.save(outputPath);
			odfDoc.close();

			LOGGER.info("Successfully wrote ODF document: {}", outputPath);
			return true;

		} catch (Exception e) {
			LOGGER.error("Error writing ODF document", e);
			return false;
		}
	}

	/**
	 * Sets up page layout including dimensions, margins, and orientation.
	 */
	private void setupPageLayout(OdfTextDocument odfDoc, Document doc) throws Exception {
		OdfOfficeMasterStyles masterStyles = odfDoc.getOfficeMasterStyles();
		StyleMasterPageElement masterStyle = masterStyles.getMasterPage("Standard");

		if (masterStyle == null) {
			LOGGER.warn("Could not find Standard master page style");
			return;
		}

		String layoutName = masterStyle.getStylePageLayoutNameAttribute();
		OdfStylePageLayout layoutStyle = masterStyle.getAutomaticStyles().getPageLayout(layoutName);

		if (layoutStyle == null) {
			LOGGER.warn("Could not find page layout: {}", layoutName);
			return;
		}

		// Set page dimensions (convert points to inches for ODF)
		if (doc.getPageWidth() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.PageWidth,
					String.format("%.4fin", doc.getPageWidth() / POINTS_PER_INCH));
		}

		if (doc.getPageHeight() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.PageHeight,
					String.format("%.4fin", doc.getPageHeight() / POINTS_PER_INCH));
		}

		// Set margins (convert points to inches)
		if (doc.getMarginTop() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.MarginTop,
					String.format("%.4fin", doc.getMarginTop() / POINTS_PER_INCH));
		}

		if (doc.getMarginBottom() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.MarginBottom,
					String.format("%.4fin", doc.getMarginBottom() / POINTS_PER_INCH));
		}

		if (doc.getMarginLeft() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.MarginLeft,
					String.format("%.4fin", doc.getMarginLeft() / POINTS_PER_INCH));
		}

		if (doc.getMarginRight() > 0) {
			layoutStyle.setProperty(OdfPageLayoutProperties.MarginRight,
					String.format("%.4fin", doc.getMarginRight() / POINTS_PER_INCH));
		}

		// Set orientation
		if (doc.isLandscape()) {
			layoutStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "landscape");
		} else {
			layoutStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "portrait");
		}
	}

	/**
	 * Creates ODF styles for fonts found in the AppleWorks document.
	 */
	private void setupFontStyles(OdfTextDocument odfDoc, Document doc) throws Exception {
		if (doc.getFonts().isEmpty()) {
			return;
		}

		OdfOfficeStyles styles = odfDoc.getOrCreateDocumentStyles();

		for (int i = 0; i < doc.getFonts().size(); i++) {
			String fontName = doc.getFonts().get(i);
			String styleName = "Font" + i;

			OdfStyle fontStyle = styles.newStyle(styleName, OdfStyleFamily.Text);
			fontStyle.setProperty(OdfTextProperties.FontName, fontName);
		}
	}

	/**
	 * Adds document content to the ODF document, applying style runs if available.
	 */
	private void addContent(OdfTextDocument odfDoc, Document doc) throws Exception {
		String content = doc.getContent();
		if (content == null || content.isEmpty()) {
			LOGGER.warn("Document has no content");
			return;
		}

		// Split content into paragraphs (AppleWorks uses \r or \n for line breaks)
		String[] paragraphs = content.split("[\r\n]+");

		for (String paragraphText : paragraphs) {
			if (paragraphText.isEmpty()) {
				continue;
			}

			// Check if we have style runs to apply
			if (!doc.getStyleRuns().isEmpty()) {
				addStyledParagraph(odfDoc, doc, paragraphText);
			} else {
				// Simple text without styling
				OdfTextParagraph paragraph = odfDoc.newParagraph();
				paragraph.addContent(paragraphText);
			}
		}
	}

	/**
	 * Adds a paragraph with style runs applied.
	 * Note: Style run parsing is still experimental and may not work correctly.
	 */
	private void addStyledParagraph(OdfTextDocument odfDoc, Document doc, String text) throws Exception {
		OdfTextParagraph paragraph = odfDoc.newParagraph();

		// For now, just add plain text
		// TODO: Implement proper style run application once CHAR block parsing is complete
		paragraph.addContent(text);

		// Future implementation would iterate through style runs and create spans:
		// for (StyleRun run : doc.getStyleRuns()) {
		//     if (run overlaps with this paragraph) {
		//         TextSpanElement span = paragraph.newTextSpanElement();
		//         span.setStyleName(getStyleForRun(run));
		//         span.setTextContent(substring);
		//     }
		// }
	}

	/**
	 * Convenience method to convert a CWK file to ODF.
	 *
	 * @param cwkPath Path to the input .cwk file
	 * @param odfPath Path for the output .odt file (optional, defaults to same name with .odt extension)
	 * @return true if successful
	 */
	public static boolean convert(String cwkPath, String odfPath) {
		if (odfPath == null || odfPath.isEmpty()) {
			odfPath = cwkPath.replaceAll("\\.[cC][wW][kK]$", ".odt");
		}

		// TODO: Integrate with Parser to parse and write in one step
		LOGGER.info("Converting {} to {}", cwkPath, odfPath);

		// This would be called after parsing:
		// Parser parser = new Parser();
		// Document doc = parser.parse(cwkPath);
		// OdfWriter writer = new OdfWriter();
		// return writer.write(doc, odfPath);

		return false;
	}
}
