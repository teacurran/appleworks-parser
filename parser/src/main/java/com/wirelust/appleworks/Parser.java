package com.wirelust.appleworks;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

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
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 1/15/13
 *
 * @author T. Curran
 *
 * Appleworks / clarisworks parser, as described here:
 *
 * http://wiki.wirelust.com/x/index.php/AppleWorks_/_ClarisWorks#Document_Header
 *
 */
public class Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

	static final byte[] KEYWORD_DSET	= {0x44, 0x53, 0x45, 0x54};


	private String opt_file;
	private boolean opt_output = false;
	private boolean opt_print_content = false;

	/**
	 * @param args command line arguments
	 */
	public static void main(
			String[] args) {

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

		byte allBytes[] = null;

		Charset macRomanCharset = Charset.forName("MacRoman");
		Charset utf8Charset = Charset.forName("UTF-8");

		// page height
		short page_height = 792;

		// page width
		short page_width = 612;

		// margins
		short margin1 = 72;
		short margin2 = 72;
		short margin3 = 72;
		short margin4 = 72;
		short margin5 = 72;
		short margin6 = 72;

		// page height
		short page_inner_height = 720;

		// page width
		short page_inner_width = 540;

		Document doc = new Document();

		try {

			File file = new File(opt_file);

			fileInputStream = new FileInputStream(file);
			bufferedInputStream = new BufferedInputStream(fileInputStream);
			countingInputStream = new CountingInputStream(bufferedInputStream);
			dataInputStream = new DataInputStream(countingInputStream);

			// version
			// 06 07 E1 00
			int version1 = dataInputStream.read();
			int version2 = dataInputStream.read();
			int version3 = dataInputStream.read();
			dataInputStream.skipBytes(1);
			println("version=%d.%d.%d", version1, version2, version3);

			int typePosition = 0;
			switch (version1) {
				case 5:
					typePosition = 268;
					break;
				case 6:
					typePosition = 278;
			}

			// BOBO
			// 42 4F 42 4F
			byte[] bobo = new byte[4];
			dataInputStream.read(bobo);

			if (bobo[0] != 0x42
					|| bobo[1] != 0x4F
					|| bobo[2] != 0x42
					|| bobo[3] != 0x4F) {
				exit("file appears to be invalid. BOBO header not found");
			}

			// previous version
			// 06 07 E1 00
			int previousVersion1 = dataInputStream.read();
			int previousVersion2 = dataInputStream.read();
			int previousVersion3 = dataInputStream.read();
			dataInputStream.skipBytes(1);
			println("previous version=%d.%d.%d", previousVersion1, previousVersion2, previousVersion3);

			// unknown
			// 00 00 00 00 00 00 00 00
			dataInputStream.skipBytes(8);

			// unnknown
			// 00 01
			dataInputStream.skipBytes(2);

			// unknown, possible marker
			// 01 89
			dataInputStream.skipBytes(2);

			// unknown
			// 1E EC
			dataInputStream.skipBytes(2);

			// unknown
			// 00 00 00 00
			dataInputStream.skipBytes(4);

			// page height
			page_height = dataInputStream.readShort();
			println("page height=%d", page_height);

			// page width
			page_width = dataInputStream.readShort();
			println("page width=%d", page_width);

			// margins
			// 00 48 00 48 00 48 00 48 00 48 00 48
			margin1 = dataInputStream.readShort();
			println("margin1=%d", margin1);

			margin2 = dataInputStream.readShort();
			println("margin2=%d", margin2);

			margin3 = dataInputStream.readShort();
			println("margin3=%d", margin3);

			margin4 = dataInputStream.readShort();
			println("margin4=%d", margin4);

			margin5 = dataInputStream.readShort();
			println("margin5=%d", margin5);

			margin6 = dataInputStream.readShort();
			println("margin6=%d", margin6);

			// page height
			page_inner_height = dataInputStream.readShort();
			println("page inner height=%d", page_inner_height);

			// page width
			page_inner_width = dataInputStream.readShort();
			println("page inner width=%d", page_inner_width);


			if (typePosition > 0 && countingInputStream.getCount() < typePosition) {
				dataInputStream.skipBytes(typePosition - countingInputStream.getCount());
			}

			doc.setType(dataInputStream.readByte());

			// this isn't really the end of the header. I just don't know where it ends yet.
			int headerEndPosition = countingInputStream.getCount();


			// TODO: figure out a more efficient way to do this.
			// for now, we are just reading the whole file into an array
			// once we have more of the file format figured out we should be
			// able to do it with a peek ahead input stream.


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
			int content_start_position = 0;
			int contentEndPosition = 0;
			boolean content_start_found = false;

			int dsetBlocks = 5; // all DSETs have beeen observed to have 5 blocks
			int dsetIndex = 0;
			byte[] keyword = new byte[4];
			for (int i=0; i<allBytes.length; i++) {
				keyword = Arrays.copyOfRange(allBytes, i, i + keyword.length);

				int blockPosition = i + keyword.length;
				if (Arrays.equals(keyword, KEYWORD_DSET)) {
					println("DSET found at position:%d", i);
					dsetIndex++;

					for (int blockIndex = 0; blockIndex < dsetBlocks; blockIndex++) {
						int blockLen = getArrayInt(allBytes, blockPosition);
						println("\tdset:%d, block:%d, length:%d", dsetIndex, blockIndex, blockLen);

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
					// OdfTextDocument really throws java.lang.Exception, really.
					LOGGER.error("error creating output document");
					opt_output = false;
				}

				if (convertedDoc != null) {
					OdfOfficeMasterStyles masterStyles = convertedDoc.getOfficeMasterStyles(); 
					StyleMasterPageElement masterStyle = masterStyles.getMasterPage("Standard");
					String layoutName = masterStyle.getStylePageLayoutNameAttribute();

					OdfStylePageLayout layoutStyle = masterStyle.getAutomaticStyles().getPageLayout(layoutName);
					layoutStyle.setProperty(OdfPageLayoutProperties.PageWidth, String.format("%dpt", page_width));
					layoutStyle.setProperty(OdfPageLayoutProperties.PageHeight, String.format("%dpt", page_height));
					
					if (page_width > page_height) {
						masterStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "landscape");
					} else {
						masterStyle.setProperty(OdfPageLayoutProperties.PrintOrientation, "portrait");
					}
				}
			}

			if (content_start_found) {
				println("content starts at position=%d", content_start_position);

				boolean contentEndFound = false;
				int blockPosition = content_start_position;
				int contentBlockIndex = 0;
				while (!contentEndFound) {
					int blockLen = getArrayInt(allBytes, blockPosition);

					String block = new String(Arrays.copyOfRange(allBytes, blockPosition + 4, blockPosition + 4 + blockLen), macRomanCharset);

					if (block.charAt(block.length()-1) == 0x00) {
						contentEndFound = true;
						contentEndPosition = blockPosition;

						// we don't want that last null character.
						block = block.substring(0, block.length() - 1);
					}

					println("\tcontent block:%d, length:%d", contentBlockIndex, blockLen);

					// TODO: I don't think this properly converts MacRoman to UTF8
					String blockUtf8 = new String(block.getBytes(utf8Charset)).replaceAll("\r", "\n");

					if (opt_print_content) {
						System.out.println(blockUtf8);
					}

					if (opt_output) {
						// add the text to the convert document
						try {
							convertedDoc.addText(blockUtf8);
						} catch (Exception e) {
							LOGGER.error("error adding text to converted document", e);
						}
					}


					blockPosition = blockPosition + 4 + blockLen;
					contentBlockIndex++;

				}

				println("content ends at position=%d", contentEndPosition);

				if (opt_output) {
					String convertedFileName = opt_file.replace(".cwk", ".odt");

					if (new File(convertedFileName).exists()) {
						LOGGER.error("unable to save converted document. '{}' exists.", convertedFileName);
					} else {
						// Save converted document
						try {
							convertedDoc.save(convertedFileName);
							convertedDoc.close();
						} catch (Exception e) {
							LOGGER.error("error saving converted document", e);
						}
					}
				}
			}
		}
	}

	public void processFirstDSet(byte allBytes[], int startPosition) {
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

			for (int i=0; i<blockCount; i++) {
				position = position + 30;
			}
			println("\tblocks end at:%d", position);

			dsetDone = true;
		}



	}

	public int getArrayInt(byte[] array, int position) {
		ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(array, position, position + 4));
		return wrapped.getInt();
	}

	public short getArrayShort(byte[] array, int position) {
		ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(array, position, position + 2));
		return wrapped.getShort();
	}

	public void println(String line, Object... args) {
		System.out.format(line + "\n", args);
	}

	/**
	 * Converts a main(String[] args) array into a parsed CommandLine
	 *
	 * @param args args from supplied to the main method
	 * @return Parsed CommandLine object
	 */
	private CommandLine getCommandLine(String[] args) {

		CommandLine line = null;
		// create the command line parser
		CommandLineParser parser = new GnuParser();
		Options options = getOptions();
		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (ParseException e) {
			// oops, something went wrong
			exitWithUsage("Invalid args encountered:" + e.getMessage());
		}
		return line;
	}

	/**
	 * Gets command line options for the application
	 *
	 * @return Options for the application
	 */
	public Options getOptions() {

		Options options = new Options();
		options.addOption("f", "file", true, "file to parse");
		options.addOption("o", "output", false, "output converted document");
		options.addOption("p", "print-content", false, "print content");

		return options;
	}

	/**
	 * reads settings from command line
	 *
	 * @param cl commandLine instance
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
	 *
	 * @param msg to be printed to the log right before System.exit
	 */
	private void exit(String msg) {

		LOGGER.info(msg);
		System.exit(1);
	}

	/**
	 * Exits the application with a message in the log and prints the usage to the stdout
	 *
	 * @param msg to be printed to the log
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

		// automatically generate the usage statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("parse.sh", getOptions());
	}

}
