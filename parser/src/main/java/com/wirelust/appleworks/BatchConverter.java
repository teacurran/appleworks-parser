package com.wirelust.appleworks;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Batch converter for AppleWorks files.
 * Converts all CWK files in an input directory to ODT format in an output directory.
 *
 * Usage: java BatchConverter <input_dir> <output_dir>
 */
public class BatchConverter {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: BatchConverter <input_dir> <output_dir>");
			System.out.println("  Converts all .cwk files in input_dir to .odt files in output_dir");
			System.exit(1);
		}

		String inputDir = args[0];
		String outputDir = args[1];

		File inputFolder = new File(inputDir);
		File outputFolder = new File(outputDir);

		if (!inputFolder.exists() || !inputFolder.isDirectory()) {
			System.err.println("Input directory does not exist: " + inputDir);
			System.exit(1);
		}

		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		}

		// Find all CWK files
		File[] cwkFiles = inputFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".cwk");
			}
		});

		if (cwkFiles == null || cwkFiles.length == 0) {
			System.out.println("No .cwk files found in: " + inputDir);
			System.exit(0);
		}

		System.out.println("Converting " + cwkFiles.length + " CWK files...");
		System.out.println("========================================");

		Parser parser = new Parser();
		OdfWriter writer = new OdfWriter();

		int success = 0;
		int failed = 0;

		for (int i = 0; i < cwkFiles.length; i++) {
			File cwkFile = cwkFiles[i];
			String baseName = cwkFile.getName().replaceAll("\\.[cC][wW][kK]$", "");
			File odtFile = new File(outputFolder, baseName + ".odt");

			System.out.printf("[%3d/%3d] %-50s ", i + 1, cwkFiles.length, cwkFile.getName());

			try {
				Document doc = parser.parse(cwkFile.getAbsolutePath());

				if (doc != null && doc.getContent() != null) {
					// Delete existing output file if present
					if (odtFile.exists()) {
						odtFile.delete();
					}

					boolean written = writer.write(doc, odtFile.getAbsolutePath());
					if (written) {
						System.out.println("\u001B[32mOK\u001B[0m");
						success++;
					} else {
						System.out.println("\u001B[31mFAILED (write)\u001B[0m");
						failed++;
					}
				} else {
					System.out.println("\u001B[33mSKIPPED (no content)\u001B[0m");
					failed++;
				}
			} catch (Exception e) {
				System.out.println("\u001B[31mFAILED (" + e.getMessage() + ")\u001B[0m");
				failed++;
			}
		}

		System.out.println("========================================");
		System.out.println();
		System.out.println("Conversion complete:");
		System.out.println("  \u001B[32mSuccess: " + success + "\u001B[0m");
		System.out.println("  \u001B[31mFailed:  " + failed + "\u001B[0m");
		System.out.println();
		System.out.println("Output files are in: " + outputFolder.getAbsolutePath());
	}
}
