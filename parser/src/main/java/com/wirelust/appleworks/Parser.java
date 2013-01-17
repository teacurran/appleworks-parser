package com.wirelust.appleworks;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
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

		try {

			File file = new File(opt_file);

			FileInputStream fileInputStream = new FileInputStream(file);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			CountingInputStream countingInputStream = new CountingInputStream(bufferedInputStream);
			DataInputStream dataInputStream = new DataInputStream(countingInputStream);

			// version
			// 06 07 E1 00
			int version1 = dataInputStream.read();
			int version2 = dataInputStream.read();
			int version3 = dataInputStream.read();
			dataInputStream.skipBytes(1);
			println("version=%d.%d.%d", version1, version2, version3);

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
			int previous_version1 = dataInputStream.read();
			int previous_version2 = dataInputStream.read();
			int previous_version3 = dataInputStream.read();
			dataInputStream.skipBytes(1);
			println("previous version=%d.%d.%d", previous_version1, previous_version2, previous_version3);


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
			short page_height = dataInputStream.readShort();
			println("page height=%d", page_height);

			// page width
			short page_width = dataInputStream.readShort();
			println("page width=%d", page_width);

			// margins
			// 00 48 00 48 00 48 00 48 00 48 00 48
			short margin1 = dataInputStream.readShort();
			println("margin1=%d", margin1);

			short margin2 = dataInputStream.readShort();
			println("margin2=%d", margin2);

			short margin3 = dataInputStream.readShort();
			println("margin3=%d", margin3);

			short margin4 = dataInputStream.readShort();
			println("margin4=%d", margin4);

			short margin5 = dataInputStream.readShort();
			println("margin5=%d", margin5);

			short margin6 = dataInputStream.readShort();
			println("margin6=%d", margin6);

						// page height
			short page_inner_height = dataInputStream.readShort();
			println("page inner height=%d", page_inner_height);

			// page width
			short page_inner_width = dataInputStream.readShort();
			println("page inner width=%d", page_inner_width);

			// this isn't really the end of the header. I just don't know where it ends yet.
			int header_end_position = countingInputStream.getCount();


			// TODO: figure out a more efficient way to do this.
			// for now, we are just reading the whole file into an array

			FileInputStream fileInputStream2 = new FileInputStream(file);
			BufferedInputStream bufferedInputStream2 = new BufferedInputStream(fileInputStream2);


			byte allBytes[] = IOUtils.toByteArray(bufferedInputStream2);

			int content_start_position = 0;
			int content_end_position = 0;
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
				}

				if (dsetIndex == 1 && !content_start_found) {
					content_start_found = true;
					content_start_position = blockPosition;
				}
			}

			if (content_start_found) {
				println("content starts at position=%d", content_start_position);

				boolean content_end_found = false;
				int blockPosition = content_start_position;
				int contentBlockIndex = 0;
				while (!content_end_found) {
					int blockLen = getArrayInt(allBytes, blockPosition);

					String block = new String(Arrays.copyOfRange(allBytes, blockPosition + 4, blockPosition + 4 + blockLen), "MacRoman");

					println("\tcontent block:%d, length:%d", contentBlockIndex, blockLen);

					String block2 = block.replaceAll("\r", "\n");

					System.out.println(block2);

					// TODO: convert strings into UTF

					blockPosition = blockPosition + 4 + blockLen;
					contentBlockIndex++;

					if (block.charAt(block.length()-1) == 0x00) {
						content_end_found = true;
						content_end_position = blockPosition;
					}

				}

				println("content ends at position=%d", content_end_position);
			}

		} catch (IOException e) {
			LOGGER.error("ioexception", e);
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