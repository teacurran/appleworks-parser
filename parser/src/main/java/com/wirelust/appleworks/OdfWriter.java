package com.wirelust.appleworks;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	// Style name constants
	private static final String STYLE_BOLD = "AWBold";
	private static final String STYLE_ITALIC = "AWItalic";
	private static final String STYLE_UNDERLINE = "AWUnderline";
	private static final String STYLE_BOLD_ITALIC = "AWBoldItalic";
	private static final String STYLE_BOLD_UNDERLINE = "AWBoldUnderline";
	private static final String STYLE_ITALIC_UNDERLINE = "AWItalicUnderline";
	private static final String STYLE_ALL = "AWBoldItalicUnderline";

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
	 * Creates ODF styles for fonts and text formatting found in the AppleWorks document.
	 */
	private void setupFontStyles(OdfTextDocument odfDoc, Document doc) throws Exception {
		OdfOfficeStyles styles = odfDoc.getOrCreateDocumentStyles();

		// Create font styles if fonts are available
		if (!doc.getFonts().isEmpty()) {
			for (int i = 0; i < doc.getFonts().size(); i++) {
				String fontName = doc.getFonts().get(i);
				String styleName = "Font" + i;

				OdfStyle fontStyle = styles.newStyle(styleName, OdfStyleFamily.Text);
				fontStyle.setProperty(OdfTextProperties.FontName, fontName);
			}
		}

		// Create text formatting styles for bold, italic, underline
		OdfStyle boldStyle = styles.newStyle(STYLE_BOLD, OdfStyleFamily.Text);
		boldStyle.setProperty(OdfTextProperties.FontWeight, "bold");

		OdfStyle italicStyle = styles.newStyle(STYLE_ITALIC, OdfStyleFamily.Text);
		italicStyle.setProperty(OdfTextProperties.FontStyle, "italic");

		OdfStyle underlineStyle = styles.newStyle(STYLE_UNDERLINE, OdfStyleFamily.Text);
		underlineStyle.setProperty(OdfTextProperties.TextUnderlineStyle, "solid");
		underlineStyle.setProperty(OdfTextProperties.TextUnderlineWidth, "auto");
		underlineStyle.setProperty(OdfTextProperties.TextUnderlineColor, "font-color");

		// Combination styles
		OdfStyle boldItalicStyle = styles.newStyle(STYLE_BOLD_ITALIC, OdfStyleFamily.Text);
		boldItalicStyle.setProperty(OdfTextProperties.FontWeight, "bold");
		boldItalicStyle.setProperty(OdfTextProperties.FontStyle, "italic");

		OdfStyle boldUnderlineStyle = styles.newStyle(STYLE_BOLD_UNDERLINE, OdfStyleFamily.Text);
		boldUnderlineStyle.setProperty(OdfTextProperties.FontWeight, "bold");
		boldUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineStyle, "solid");
		boldUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineWidth, "auto");
		boldUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineColor, "font-color");

		OdfStyle italicUnderlineStyle = styles.newStyle(STYLE_ITALIC_UNDERLINE, OdfStyleFamily.Text);
		italicUnderlineStyle.setProperty(OdfTextProperties.FontStyle, "italic");
		italicUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineStyle, "solid");
		italicUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineWidth, "auto");
		italicUnderlineStyle.setProperty(OdfTextProperties.TextUnderlineColor, "font-color");

		OdfStyle allStyle = styles.newStyle(STYLE_ALL, OdfStyleFamily.Text);
		allStyle.setProperty(OdfTextProperties.FontWeight, "bold");
		allStyle.setProperty(OdfTextProperties.FontStyle, "italic");
		allStyle.setProperty(OdfTextProperties.TextUnderlineStyle, "solid");
		allStyle.setProperty(OdfTextProperties.TextUnderlineWidth, "auto");
		allStyle.setProperty(OdfTextProperties.TextUnderlineColor, "font-color");
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

		List<StyleRun> styleRuns = doc.getStyleRuns();

		// If no style runs, just add plain text
		if (styleRuns.isEmpty()) {
			String[] paragraphs = content.split("[\r\n]+");
			for (String paragraphText : paragraphs) {
				if (!paragraphText.isEmpty()) {
					OdfTextParagraph paragraph = odfDoc.newParagraph();
					paragraph.addContent(paragraphText);
				}
			}
			return;
		}

		// With style runs, we need to track global position
		// Split content into paragraphs while preserving position information
		int globalPos = 0;
		int contentLen = content.length();

		// Process content character by character, grouping into paragraphs
		StringBuilder currentPara = new StringBuilder();
		int paraStartPos = 0;

		for (int i = 0; i <= contentLen; i++) {
			boolean isEnd = (i == contentLen);
			boolean isNewline = !isEnd && (content.charAt(i) == '\n' || content.charAt(i) == '\r');

			if (isEnd || isNewline) {
				// End of paragraph - output it with styles
				if (currentPara.length() > 0) {
					addStyledParagraph(odfDoc, currentPara.toString(), paraStartPos, styleRuns);
				}
				currentPara = new StringBuilder();
				paraStartPos = i + 1;

				// Skip \r\n as single newline
				if (!isEnd && i + 1 < contentLen) {
					char c = content.charAt(i);
					char next = content.charAt(i + 1);
					if (c == '\r' && next == '\n') {
						i++;
						paraStartPos = i + 1;
					}
				}
			} else {
				currentPara.append(content.charAt(i));
			}
		}
	}

	/**
	 * Adds a paragraph with style runs applied based on global character positions.
	 *
	 * @param odfDoc The ODF document
	 * @param text The paragraph text
	 * @param paragraphStartPos The global character position where this paragraph starts
	 * @param styleRuns All style runs from the document
	 */
	private void addStyledParagraph(OdfTextDocument odfDoc, String text, int paragraphStartPos,
									List<StyleRun> styleRuns) throws Exception {
		OdfTextParagraph paragraph = odfDoc.newParagraph();

		int paraEnd = paragraphStartPos + text.length();
		int currentPos = 0;

		// Find all style runs that overlap with this paragraph
		for (StyleRun run : styleRuns) {
			int runStart = run.getStartOffset();
			int runEnd = run.getEndOffset();

			// Check if this run overlaps with our paragraph
			if (runEnd <= paragraphStartPos || runStart >= paraEnd) {
				continue; // No overlap
			}

			// Calculate the local positions within this paragraph
			int localStart = Math.max(0, runStart - paragraphStartPos);
			int localEnd = Math.min(text.length(), runEnd - paragraphStartPos);

			// Add any unstyled text before this run
			if (localStart > currentPos) {
				String plainText = text.substring(currentPos, localStart);
				paragraph.addContent(plainText);
			}

			// Add the styled text
			if (localEnd > localStart) {
				String styledText = text.substring(localStart, localEnd);
				String styleName = getStyleNameForRun(run);

				if (styleName != null) {
					TextSpanElement span = paragraph.newTextSpanElement();
					span.setStyleName(styleName);
					span.setTextContent(styledText);
				} else {
					paragraph.addContent(styledText);
				}
				currentPos = localEnd;
			}
		}

		// Add any remaining unstyled text after all runs
		if (currentPos < text.length()) {
			paragraph.addContent(text.substring(currentPos));
		}
	}

	/**
	 * Returns the ODF style name for a given style run.
	 */
	private String getStyleNameForRun(StyleRun run) {
		boolean bold = run.isBold();
		boolean italic = run.isItalic();
		boolean underline = run.isUnderline();

		// Return appropriate combination style
		if (bold && italic && underline) {
			return STYLE_ALL;
		} else if (bold && italic) {
			return STYLE_BOLD_ITALIC;
		} else if (bold && underline) {
			return STYLE_BOLD_UNDERLINE;
		} else if (italic && underline) {
			return STYLE_ITALIC_UNDERLINE;
		} else if (bold) {
			return STYLE_BOLD;
		} else if (italic) {
			return STYLE_ITALIC;
		} else if (underline) {
			return STYLE_UNDERLINE;
		}

		// Normal text - no style needed
		return null;
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
