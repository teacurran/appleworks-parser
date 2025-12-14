package com.wirelust.appleworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 19-Oct-2015
 *
 * @author T. Curran
 *
 * Represents a parsed AppleWorks/ClarisWorks document.
 * Document types are determined by the 2-byte value at offset 0x80.
 */
public class Document {

	// Document type constants (2-byte values at offset 0x80-0x81)
	public static final int TYPE_SPREADSHEET = 0x0000;
	public static final int TYPE_DRAWING = 0x0101;
	public static final int TYPE_TEXT = 0x0202;
	public static final int TYPE_SPREADSHEET_V5 = 0x0303;
	public static final int TYPE_PAINTING = 0x1515;

	// Version information
	private int majorVersion;
	private int minorVersion;
	private int buildVersion;
	private int previousMajorVersion;
	private int previousMinorVersion;
	private int previousBuildVersion;

	// Document type
	private int type;

	// Page dimensions (in points, 72 points = 1 inch)
	private short pageHeight;
	private short pageWidth;
	private short innerHeight;
	private short innerWidth;

	// Margins (in points)
	private short marginTop;
	private short marginLeft;
	private short marginBottom;
	private short marginRight;
	private short margin5;
	private short margin6;

	// Font information from FNTM block
	private List<String> fonts = new ArrayList<>();

	// Style runs from CHAR block
	private List<StyleRun> styleRuns = new ArrayList<>();

	// Block index from ETBL (maps block name to file offset)
	private Map<String, Integer> blockIndex = new HashMap<>();

	// Thumbnail image data from SNAP block (PICT format)
	private byte[] thumbnail;

	// Document content
	private String content;

	// Getters and Setters

	public int getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	public int getBuildVersion() {
		return buildVersion;
	}

	public void setBuildVersion(int buildVersion) {
		this.buildVersion = buildVersion;
	}

	public int getPreviousMajorVersion() {
		return previousMajorVersion;
	}

	public void setPreviousMajorVersion(int previousMajorVersion) {
		this.previousMajorVersion = previousMajorVersion;
	}

	public int getPreviousMinorVersion() {
		return previousMinorVersion;
	}

	public void setPreviousMinorVersion(int previousMinorVersion) {
		this.previousMinorVersion = previousMinorVersion;
	}

	public int getPreviousBuildVersion() {
		return previousBuildVersion;
	}

	public void setPreviousBuildVersion(int previousBuildVersion) {
		this.previousBuildVersion = previousBuildVersion;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public short getPageHeight() {
		return pageHeight;
	}

	public void setPageHeight(short pageHeight) {
		this.pageHeight = pageHeight;
	}

	public short getPageWidth() {
		return pageWidth;
	}

	public void setPageWidth(short pageWidth) {
		this.pageWidth = pageWidth;
	}

	public short getInnerHeight() {
		return innerHeight;
	}

	public void setInnerHeight(short innerHeight) {
		this.innerHeight = innerHeight;
	}

	public short getInnerWidth() {
		return innerWidth;
	}

	public void setInnerWidth(short innerWidth) {
		this.innerWidth = innerWidth;
	}

	public short getMarginTop() {
		return marginTop;
	}

	public void setMarginTop(short marginTop) {
		this.marginTop = marginTop;
	}

	public short getMarginLeft() {
		return marginLeft;
	}

	public void setMarginLeft(short marginLeft) {
		this.marginLeft = marginLeft;
	}

	public short getMarginBottom() {
		return marginBottom;
	}

	public void setMarginBottom(short marginBottom) {
		this.marginBottom = marginBottom;
	}

	public short getMarginRight() {
		return marginRight;
	}

	public void setMarginRight(short marginRight) {
		this.marginRight = marginRight;
	}

	public short getMargin5() {
		return margin5;
	}

	public void setMargin5(short margin5) {
		this.margin5 = margin5;
	}

	public short getMargin6() {
		return margin6;
	}

	public void setMargin6(short margin6) {
		this.margin6 = margin6;
	}

	public List<String> getFonts() {
		return fonts;
	}

	public void setFonts(List<String> fonts) {
		this.fonts = fonts;
	}

	public void addFont(String font) {
		this.fonts.add(font);
	}

	public List<StyleRun> getStyleRuns() {
		return styleRuns;
	}

	public void setStyleRuns(List<StyleRun> styleRuns) {
		this.styleRuns = styleRuns;
	}

	public void addStyleRun(StyleRun styleRun) {
		this.styleRuns.add(styleRun);
	}

	public Map<String, Integer> getBlockIndex() {
		return blockIndex;
	}

	public void setBlockIndex(Map<String, Integer> blockIndex) {
		this.blockIndex = blockIndex;
	}

	public Integer getBlockOffset(String blockName) {
		return blockIndex.get(blockName);
	}

	public byte[] getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(byte[] thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Returns a human-readable document type name.
	 */
	public String getTypeName() {
		switch (type) {
			case TYPE_SPREADSHEET:
			case TYPE_SPREADSHEET_V5:
				return "Spreadsheet";
			case TYPE_DRAWING:
				return "Drawing";
			case TYPE_TEXT:
				return "Word Processing";
			case TYPE_PAINTING:
				return "Painting";
			default:
				return "Unknown (" + String.format("0x%04X", type) + ")";
		}
	}

	/**
	 * Returns true if the document was converted from an older version.
	 */
	public boolean wasConverted() {
		return previousMajorVersion != majorVersion ||
				previousMinorVersion != minorVersion ||
				previousBuildVersion != buildVersion;
	}

	/**
	 * Returns the version string (e.g., "6.7.225").
	 */
	public String getVersionString() {
		return String.format("%d.%d.%d", majorVersion, minorVersion, buildVersion);
	}

	/**
	 * Returns true if the document is in landscape orientation.
	 */
	public boolean isLandscape() {
		return pageWidth > pageHeight;
	}
}
