package com.wirelust.appleworks;

/**
 * Date: 19-Oct-2015
 *
 * @author T. Curran
 */
public class Document {
	public static final int TYPE_TEXT = 1;
	public static final int TYPE_PAINTING = 4;

	int type;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
