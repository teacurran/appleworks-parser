package com.wirelust.appleworks;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.style.props.OdfPageLayoutProperties;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeMasterStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 1/15/13
 *
 * @author T. Curran
 *
 * AppleWorks / ClarisWorks parser, as described here:
 *
 * http://wiki.wirelust.com/x/index.php/AppleWorks_/_ClarisWorks#Document_Header
 *
 * Supports versions 4.x, 5.x, and 6.x of ClarisWorks/AppleWorks files (.cwk)
 */
public class Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

	// Block keyword constants
	static final byte[] KEYWORD_DSET = {0x44, 0x53, 0x45, 0x54}; // DSET - Dataset
	static final byte[] KEYWORD_ETBL = {0x45, 0x54, 0x42, 0x4C}; // ETBL - End Table (TOC)
	static final byte[] KEYWORD_FNTM = {0x46, 0x4E, 0x54, 0x4D}; // FNTM - Font Map
	static final byte[] KEYWORD_CHAR = {0x43, 0x48, 0x41, 0x52}; // CHAR - Character Styles
	static final byte[] KEYWORD_STYL = {0x53, 0x54, 0x59, 0x4C}; // STYL - Styles
	static final byte[] KEYWORD_SNAP = {0x53, 0x4E, 0x41, 0x50}; // SNAP - Snapshot/Thumbnail
	static final byte[] KEYWORD_CPRT = {0x43, 0x50, 0x52, 0x54}; // CPRT - Print Settings
	static final byte[] KEYWORD_DSUM = {0x44, 0x53, 0x55, 0x4D}; // DSUM - Document Summary
	static final byte[] KEYWORD_HDNI = {0x48, 0x44, 0x4E, 0x49}; // HDNI - Header Info
	static final byte[] KEYWORD_MCRO = {0x4D, 0x43, 0x52, 0x4F}; // MCRO - Macros
	static final byte[] KEYWORD_MARK = {0x4D, 0x41, 0x52, 0x4B}; // MARK - Markers
	static final byte[] KEYWORD_WMBT = {0x57, 0x4D, 0x42, 0x54}; // WMBT - Unknown
	static final byte[] KEYWORD_TNAM = {0x54, 0x4E, 0x41, 0x4D}; // TNAM - Template Name
	static final byte[] KEYWORD_CTAB = {0x43, 0x54, 0x41, 0x42}; // CTAB - Cell Table (spreadsheets)

	// BOBO signature
	static final byte[] SIGNATURE_BOBO = {0x42, 0x4F, 0x42, 0x4F};

	// File end marker (always present at end of file)
	static final byte[] FILE_END_MARKER = {
		(byte) 0xFF, (byte) 0xFE, (byte) 0xFD, (byte) 0xFC,
		(byte) 0xFB, (byte) 0xFA, (byte) 0xF9, (byte) 0xF8,
		(byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3,
		(byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7
	};

	// Document type position offset (corrected: at 0x80, not 268/278)
	static final int DOCTYPE_OFFSET = 0x80;

	// Font entry size in FNTM block
	static final int FONT_ENTRY_SIZE = 0x45; // 69 bytes per font

	private String opt_file;
	private boolean opt_output = false;
	private boolean opt_print_content = false;

	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		Parser app = new Parser();
		CommandLine cl = app.getCommandLine(args);
		app.processCommandLine(cl);
		app.run();
	}

	public void run() {
		println("file=" + opt_file);

		FileInputStream fileInputStream;
		BufferedInputStream bufferedInputStream;
		CountingInputStream countingInputStream;
		DataInputStream dataInputStream = null;

		FileInputStream fileInputStream2;
		BufferedInputStream bufferedInputStream2 = null;

		byte[] allBytes = null;

		Charset macRomanCharset = Charset.forName("MacRoman");
		Charset utf8Charset = Charset.forName("UTF-8");

		Document doc = new Document();

		try {
			File file = new File(opt_file);

			fileInputStream = new FileInputStream(file);
			bufferedInputStream = new BufferedInputStream(fileInputStream);
			countingInputStream = new CountingInputStream(bufferedInputStream);
			dataInputStream = new DataInputStream(countingInputStream);

			// Version bytes 0-3
			// Format: [major] [minor] [build_high] [build_low]
			int majorVersion = dataInputStream.read();
			int minorVersion = dataInputStream.read();
			int buildHigh = dataInputStream.read();
			int buildLow = dataInputStream.read();
			int buildVersion = (buildHigh << 8) | buildLow;

			doc.setMajorVersion(majorVersion);
			doc.setMinorVersion(minorVersion);
			doc.setBuildVersion(buildVersion);

			println("version=%d.%d (build 0x%02X%02X)", majorVersion, minorVersion, buildHigh, buildLow);

			// BOBO signature (bytes 4-7)
			byte[] bobo = new byte[4];
			dataInputStream.read(bobo);

			if (!Arrays.equals(bobo, SIGNATURE_BOBO)) {
				exit("file appears to be invalid. BOBO header not found");
			}

			// Previous version (bytes 8-11) - if file was converted
			int prevMajorVersion = dataInputStream.read();
			int prevMinorVersion = dataInputStream.read();
			int prevBuildHigh = dataInputStream.read();
			int prevBuildLow = dataInputStream.read();
			int prevBuildVersion = (prevBuildHigh << 8) | prevBuildLow;

			doc.setPreviousMajorVersion(prevMajorVersion);
			doc.setPreviousMinorVersion(prevMinorVersion);
			doc.setPreviousBuildVersion(prevBuildVersion);

			if (doc.wasConverted()) {
				println("previous version=%d.%d (build 0x%02X%02X) - file was converted",
						prevMajorVersion, prevMinorVersion, prevBuildHigh, prevBuildLow);
			}

			// Unknown (bytes 12-19) - usually zeros
			dataInputStream.skipBytes(8);

			// Unknown (bytes 20-21) - usually 0x0001
			dataInputStream.skipBytes(2);

			// Marker (bytes 22-23)
			dataInputStream.skipBytes(2);

			// Unknown (bytes 24-25)
			dataInputStream.skipBytes(2);

			// Unknown (bytes 26-29)
			dataInputStream.skipBytes(4);

			// Page height (bytes 30-31)
			short pageHeight = dataInputStream.readShort();
			doc.setPageHeight(pageHeight);
			println("page height=%d pts (%.2f in)", pageHeight, pageHeight / 72.0);

			// Page width (bytes 32-33)
			short pageWidth = dataInputStream.readShort();
			doc.setPageWidth(pageWidth);
			println("page width=%d pts (%.2f in)", pageWidth, pageWidth / 72.0);

			// Margins (bytes 34-45) - 6 x 2-byte values
			short marginTop = dataInputStream.readShort();
			short marginLeft = dataInputStream.readShort();
			short marginBottom = dataInputStream.readShort();
			short marginRight = dataInputStream.readShort();
			short margin5 = dataInputStream.readShort();
			short margin6 = dataInputStream.readShort();

			doc.setMarginTop(marginTop);
			doc.setMarginLeft(marginLeft);
			doc.setMarginBottom(marginBottom);
			doc.setMarginRight(marginRight);
			doc.setMargin5(margin5);
			doc.setMargin6(margin6);

			println("margins: top=%d, left=%d, bottom=%d, right=%d, m5=%d, m6=%d",
					marginTop, marginLeft, marginBottom, marginRight, margin5, margin6);

			// Inner page height (bytes 46-47)
			short innerHeight = dataInputStream.readShort();
			doc.setInnerHeight(innerHeight);

			// Inner page width (bytes 48-49)
			short innerWidth = dataInputStream.readShort();
			doc.setInnerWidth(innerWidth);

			println("inner dimensions: %d x %d pts", innerWidth, innerHeight);

			// Skip to document type position (0x80)
			int currentPos = countingInputStream.getCount();
			if (currentPos < DOCTYPE_OFFSET) {
				dataInputStream.skipBytes(DOCTYPE_OFFSET - currentPos);
			}

			// Document type (2 bytes at offset 0x80)
			short docType = dataInputStream.readShort();
			doc.setType(docType);
			println("document type=0x%04X (%s)", docType, doc.getTypeName());

			if (doc.isLandscape()) {
				println("orientation=landscape");
			} else {
				println("orientation=portrait");
			}

			// Read entire file for block parsing
			fileInputStream2 = new FileInputStream(file);
			bufferedInputStream2 = new BufferedInputStream(fileInputStream2);
			allBytes = IOUtils.toByteArray(bufferedInputStream2);

		} catch (IOException e) {
			LOGGER.error("ioexception", e);
		} finally {
			if (dataInputStream != null) {
				try {
					dataInputStream.close();
				} catch (IOException e2) {
					LOGGER.error("ioexception", e2);
				}
			}

			if (bufferedInputStream2 != null) {
				try {
					bufferedInputStream2.close();
				} catch (IOException e2) {
					LOGGER.error("ioexception", e2);
				}
			}
		}

		if (allBytes != null && allBytes.length > 0) {

			// Parse ETBL (End Table of Contents) for block index
			Map<String, Integer> blockIndex = parseETBL(allBytes);
			doc.setBlockIndex(blockIndex);
			if (!blockIndex.isEmpty()) {
				println("ETBL block index found with %d entries", blockIndex.size());
				for (Map.Entry<String, Integer> entry : blockIndex.entrySet()) {
					println("\t%s -> offset 0x%08X (%d)", entry.getKey(), entry.getValue(), entry.getValue());
				}
			}

			// Parse FNTM (Font Map)
			parseFonts(allBytes, doc);
			if (!doc.getFonts().isEmpty()) {
				println("Fonts found: %d", doc.getFonts().size());
				for (int i = 0; i < doc.getFonts().size(); i++) {
					println("\t[%d] %s", i, doc.getFonts().get(i));
				}
			}

			// Parse content using DSET blocks
			int content_start_position = 0;
			int contentEndPosition = 0;
			boolean content_start_found = false;

			int dsetBlocks = 5; // all DSETs have been observed to have 5 blocks
			int dsetIndex = 0;
			byte[] keyword = new byte[4];

			for (int i = 0; i < allBytes.length - 4; i++) {
				keyword = Arrays.copyOfRange(allBytes, i, i + keyword.length);

				int blockPosition = i + keyword.length;
				if (Arrays.equals(keyword, KEYWORD_DSET)) {
					println("DSET found at position: 0x%08X (%d)", i, i);
					dsetIndex++;

					for (int blockIndex2 = 0; blockIndex2 < dsetBlocks; blockIndex2++) {
						int blockLen = getArrayInt(allBytes, blockPosition);
						println("\tdset:%d, block:%d, length:%d", dsetIndex, blockIndex2, blockLen);
						blockPosition = blockPosition + 4 + blockLen;
					}

					if (dsetIndex == 1 && !content_start_found) {
						content_start_found = true;
						content_start_position = blockPosition;
						processFirstDSet(allBytes, i + keyword.length);
					}
				}
			}

			OdfTextDocument convertedDoc = null;

			if (opt_output) {
				try {
					convertedDoc = OdfTextDocument.newTextDocument();
				} catch (Exception e) {
					LOGGER.error("error creating output document");
					opt_output = false;
				}

				if (convertedDoc != null) {
					OdfOfficeMasterStyles masterStyles = convertedDoc.getOfficeMasterStyles();
					StyleMasterPageElement masterStyle = masterStyles.getMasterPage("Standard");
					String layoutName = masterStyle.getStylePageLayoutNameAttribute();

					OdfStylePageLayout layoutStyle = masterStyle.getAutomaticStyles().getPageLayout(layoutName);
					layoutStyle.setProperty(OdfPageLayoutProperties.PageWidth,
							String.format("%dpt", doc.getPageWidth()));
					layoutStyle.setProperty(OdfPageLayoutProperties.PageHeight,
							String.format("%dpt", doc.getPageHeight()));

					if (doc.isLandscape()) {
						masterStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "landscape");
					} else {
						masterStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "portrait");
					}
				}
			}

			if (content_start_found) {
				println("content starts at position=0x%08X (%d)", content_start_position, content_start_position);

				StringBuilder contentBuilder = new StringBuilder();
				boolean contentEndFound = false;
				int blockPosition = content_start_position;
				int contentBlockIndex = 0;

				while (!contentEndFound && blockPosition < allBytes.length - 4) {
					int blockLen = getArrayInt(allBytes, blockPosition);

					if (blockLen <= 0 || blockLen > allBytes.length - blockPosition - 4) {
						break;
					}

					String block = new String(
							Arrays.copyOfRange(allBytes, blockPosition + 4, blockPosition + 4 + blockLen),
							macRomanCharset);

					if (block.length() > 0 && block.charAt(block.length() - 1) == 0x00) {
						contentEndFound = true;
						contentEndPosition = blockPosition;
						block = block.substring(0, block.length() - 1);
					}

					println("\tcontent block:%d, length:%d", contentBlockIndex, blockLen);

					String blockUtf8 = new String(block.getBytes(utf8Charset)).replaceAll("\r", "\n");
					contentBuilder.append(blockUtf8);

					if (opt_print_content) {
						System.out.println(blockUtf8);
					}

					if (opt_output && convertedDoc != null) {
						try {
							convertedDoc.addText(blockUtf8);
						} catch (Exception e) {
							LOGGER.error("error adding text to converted document", e);
						}
					}

					blockPosition = blockPosition + 4 + blockLen;
					contentBlockIndex++;
				}

				doc.setContent(contentBuilder.toString());
				println("content ends at position=0x%08X (%d)", contentEndPosition, contentEndPosition);

				if (opt_output && convertedDoc != null) {
					String convertedFileName = opt_file.replace(".cwk", ".odt");

					if (new File(convertedFileName).exists()) {
						LOGGER.error("unable to save converted document. '{}' exists.", convertedFileName);
					} else {
						try {
							convertedDoc.save(convertedFileName);
							convertedDoc.close();
							println("Saved converted document: %s", convertedFileName);
						} catch (Exception e) {
							LOGGER.error("error saving converted document", e);
						}
					}
				}
			}

			// Verify file end marker
			int endMarkerPos = findPattern(allBytes, FILE_END_MARKER, allBytes.length - 100);
			if (endMarkerPos > 0) {
				println("File end marker found at: 0x%08X (%d)", endMarkerPos, endMarkerPos);
			}
		}
	}

	/**
	 * Parses the ETBL block to build an index of all blocks and their offsets.
	 * The ETBL block is located near the end of the file and contains a table of contents.
	 *
	 * Format:
	 *   ETBL (4 bytes)
	 *   Total size (4 bytes, big-endian)
	 *   Entries:
	 *     Block name (4 bytes, ASCII)
	 *     Block offset (4 bytes, big-endian)
	 *     ... repeating ...
	 */
	public Map<String, Integer> parseETBL(byte[] allBytes) {
		Map<String, Integer> blockIndex = new HashMap<>();

		// Find the last occurrence of ETBL (the TOC is near the end)
		int etblPos = findLastPattern(allBytes, KEYWORD_ETBL);

		if (etblPos < 0) {
			return blockIndex;
		}

		println("Parsing ETBL at position: 0x%08X", etblPos);

		int size = getArrayInt(allBytes, etblPos + 4);
		println("ETBL size: %d bytes", size);

		int pos = etblPos + 8;
		int endPos = etblPos + 4 + size;

		while (pos + 8 <= endPos && pos + 8 < allBytes.length) {
			String blockName = new String(Arrays.copyOfRange(allBytes, pos, pos + 4),
					Charset.forName("US-ASCII"));
			int offset = getArrayInt(allBytes, pos + 4);

			// Stop if we hit the file end marker or invalid data
			if (blockName.charAt(0) == (char) 0xFF) {
				break;
			}

			blockIndex.put(blockName, offset);
			pos += 8;
		}

		return blockIndex;
	}

	/**
	 * Parses the FNTM (Font Map) block to extract font names.
	 *
	 * Format:
	 *   FNTM (4 bytes)
	 *   ... header bytes ...
	 *   For each font (69 bytes each):
	 *     Header (2 bytes)
	 *     Index (2 bytes)
	 *     Name length (1 byte)
	 *     Font name (null-padded to fill remaining bytes)
	 */
	public void parseFonts(byte[] allBytes, Document doc) {
		int fntmPos = findPattern(allBytes, KEYWORD_FNTM, 0);

		if (fntmPos < 0) {
			return;
		}

		println("Parsing FNTM at position: 0x%08X", fntmPos);

		// Skip to font entries (after FNTM keyword and some header bytes)
		int pos = fntmPos + 4;

		// Read block size
		int blockSize = getArrayInt(allBytes, pos);
		pos += 4;

		// Skip header bytes (observed pattern: 0x0015 0x0002 before first font)
		// Look for font entry pattern
		while (pos < fntmPos + 4 + blockSize && pos + FONT_ENTRY_SIZE < allBytes.length) {
			// Check for font entry header pattern (0x00 0x15 observed)
			if (allBytes[pos] == 0x00 && allBytes[pos + 1] == 0x15) {
				// Skip header (2 bytes) and index (2 bytes)
				pos += 4;

				// Read font name length
				int nameLen = allBytes[pos] & 0xFF;
				pos++;

				if (nameLen > 0 && nameLen < 64 && pos + nameLen <= allBytes.length) {
					String fontName = new String(Arrays.copyOfRange(allBytes, pos, pos + nameLen),
							Charset.forName("MacRoman")).trim();
					if (!fontName.isEmpty() && fontName.matches("[\\x20-\\x7E]+")) {
						doc.addFont(fontName);
					}
				}

				// Skip to next entry (remainder of 69-byte entry minus what we've read)
				pos += (FONT_ENTRY_SIZE - 5);
			} else {
				pos++;
			}
		}
	}

	/**
	 * Process the first DSET block to understand content structure.
	 */
	public void processFirstDSet(byte[] allBytes, int startPosition) {
		println("First DSET block");

		int position = startPosition;
		boolean dsetDone = false;

		while (!dsetDone) {
			int blockLength = getArrayInt(allBytes, position);
			position = position + 4;
			println("\tblock length:%d", blockLength);

			short blockCount = getArrayShort(allBytes, position);
			position = position + 2;
			println("\tblock count:%d", blockCount);

			for (int i = 0; i < blockCount; i++) {
				position = position + 30;
			}
			println("\tblocks end at: 0x%08X (%d)", position, position);

			dsetDone = true;
		}
	}

	/**
	 * Find pattern in byte array starting from given position.
	 */
	public int findPattern(byte[] allBytes, byte[] pattern, int startPos) {
		for (int i = startPos; i <= allBytes.length - pattern.length; i++) {
			boolean found = true;
			for (int j = 0; j < pattern.length; j++) {
				if (allBytes[i + j] != pattern[j]) {
					found = false;
					break;
				}
			}
			if (found) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Find the last occurrence of pattern in byte array.
	 */
	public int findLastPattern(byte[] allBytes, byte[] pattern) {
		for (int i = allBytes.length - pattern.length; i >= 0; i--) {
			boolean found = true;
			for (int j = 0; j < pattern.length; j++) {
				if (allBytes[i + j] != pattern[j]) {
					found = false;
					break;
				}
			}
			if (found) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Read a 4-byte big-endian integer from byte array.
	 */
	public int getArrayInt(byte[] array, int position) {
		if (position + 4 > array.length) {
			return 0;
		}
		ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(array, position, position + 4));
		return wrapped.getInt();
	}

	/**
	 * Read a 2-byte big-endian short from byte array.
	 */
	public short getArrayShort(byte[] array, int position) {
		if (position + 2 > array.length) {
			return 0;
		}
		ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(array, position, position + 2));
		return wrapped.getShort();
	}

	public void println(String line, Object... args) {
		System.out.format(line + "\n", args);
	}

	/**
	 * Converts a main(String[] args) array into a parsed CommandLine
	 */
	private CommandLine getCommandLine(String[] args) {
		CommandLine line = null;
		CommandLineParser parser = new GnuParser();
		Options options = getOptions();
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			exitWithUsage("Invalid args encountered:" + e.getMessage());
		}
		return line;
	}

	/**
	 * Gets command line options for the application
	 */
	public Options getOptions() {
		Options options = new Options();
		options.addOption("f", "file", true, "file to parse");
		options.addOption("o", "output", false, "output converted document");
		options.addOption("p", "print-content", false, "print content");
		return options;
	}

	/**
	 * Reads settings from command line
	 */
	private void processCommandLine(CommandLine cl) {
		if (cl.hasOption("f")) {
			opt_file = cl.getOptionValue("f");
		}

		if (cl.hasOption("o")) {
			opt_output = true;
		}

		if (cl.hasOption("p")) {
			opt_print_content = true;
		}

		if (opt_file == null || opt_file.isEmpty()) {
			exitWithUsage("You must specify a file to parse");
		}
	}

	/**
	 * Exits the application with a message in the log
	 */
	private void exit(String msg) {
		LOGGER.info(msg);
		System.exit(1);
	}

	/**
	 * Exits the application with a message in the log and prints the usage to stdout
	 */
	private void exitWithUsage(String msg) {
		LOGGER.info(msg);
		usage();
		System.exit(1);
	}

	/**
	 * Formats and prints the command line options to stdout
	 */
	private void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("parse.sh", getOptions());
	}
}
