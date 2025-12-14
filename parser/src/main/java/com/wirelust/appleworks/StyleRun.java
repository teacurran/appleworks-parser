package com.wirelust.appleworks;

/**
 * Represents a style run within a ClarisWorks/AppleWorks document.
 *
 * A style run defines the formatting applied to a range of text.
 * Style runs are parsed from the CHAR block within the document.
 *
 * The CHAR block contains entries approximately 20 bytes each:
 * - Bytes 0-1: Style flags
 * - Bytes 2-5: Unknown (usually 0x00000000)
 * - Bytes 6-7: Unknown (0x0300 observed)
 * - Bytes 8-9: Unknown (often 0xFFFF)
 * - Bytes 10-11: Unknown
 * - Bytes 12-13: Text length for this run
 * - Bytes 14-15: Character offset (cumulative position in text)
 */
public class StyleRun {

	// Style flag constants (observed values, may be incomplete)
	public static final int FLAG_NORMAL = 0x0C01;
	public static final int FLAG_BOLD = 0x0901;
	public static final int FLAG_ITALIC = 0x0E01;
	public static final int FLAG_UNDERLINE = 0x0A01;

	private int startOffset;
	private int endOffset;
	private int styleFlags;
	private boolean bold;
	private boolean italic;
	private boolean underline;
	private int fontIndex;
	private int fontSize;

	public StyleRun() {
	}

	public StyleRun(int startOffset, int endOffset, int styleFlags) {
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.styleFlags = styleFlags;
		parseStyleFlags(styleFlags);
	}

	/**
	 * Parses the style flags to set individual style attributes.
	 * Note: This is based on observed patterns and may not be complete.
	 */
	private void parseStyleFlags(int flags) {
		// These interpretations are based on observed patterns
		// and may need refinement as more samples are analyzed
		switch (flags) {
			case FLAG_BOLD:
				this.bold = true;
				break;
			case FLAG_ITALIC:
				this.italic = true;
				break;
			case FLAG_UNDERLINE:
				this.underline = true;
				break;
			default:
				// Normal or unknown style
				break;
		}
	}

	public int getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public int getStyleFlags() {
		return styleFlags;
	}

	public void setStyleFlags(int styleFlags) {
		this.styleFlags = styleFlags;
		parseStyleFlags(styleFlags);
	}

	public boolean isBold() {
		return bold;
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public boolean isItalic() {
		return italic;
	}

	public void setItalic(boolean italic) {
		this.italic = italic;
	}

	public boolean isUnderline() {
		return underline;
	}

	public void setUnderline(boolean underline) {
		this.underline = underline;
	}

	public int getFontIndex() {
		return fontIndex;
	}

	public void setFontIndex(int fontIndex) {
		this.fontIndex = fontIndex;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	/**
	 * Returns the length of this style run in characters.
	 */
	public int getLength() {
		return endOffset - startOffset;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("StyleRun[");
		sb.append(startOffset).append("-").append(endOffset);
		sb.append(", flags=0x").append(String.format("%04X", styleFlags));
		if (bold) sb.append(", BOLD");
		if (italic) sb.append(", ITALIC");
		if (underline) sb.append(", UNDERLINE");
		if (fontIndex > 0) sb.append(", font=").append(fontIndex);
		if (fontSize > 0) sb.append(", size=").append(fontSize);
		sb.append("]");
		return sb.toString();
	}
}
