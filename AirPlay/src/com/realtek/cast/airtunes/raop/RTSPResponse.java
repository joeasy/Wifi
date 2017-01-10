package com.realtek.cast.airtunes.raop;

import java.util.Arrays;

public class RTSPResponse {
	
	private StringBuilder response = new StringBuilder();
	
	private boolean isHeaderFinialized = false;

	private byte[] content;

	public RTSPResponse(String header) {
    	response.append(header + "\r\n");
	}
	
	public void append(String key, String value) {
    	response.append(key + ": " + value + "\r\n");
	}
	
	/**
	 * close the response
	 */
	public void finalizeHeader() {
    	response.append("\r\n");
    	isHeaderFinialized = true;
	}
	
	public void ensureHeaderFinialized() {
		if (!isHeaderFinialized) {
			finalizeHeader();
		}
	}
	
//	public String getRawPacket() {
//		return response.toString();
//	}
	
	@Override
	public String toString() {
		return " > " + response.toString().replaceAll("\r\n", "\r\n > ");
	}

	public void setContent(byte[] content, int offset, int length) {
		this.content = Arrays.copyOfRange(content, offset, offset + length);
	}
	
	public byte[] getRawPacket() {
		byte[] head = response.toString().getBytes();
		if (content == null) {
			return head;
		}
		byte[] buf = new byte[head.length + content.length];
		System.arraycopy(head, 0, buf, 0, head.length);
		System.arraycopy(content, 0, buf, head.length, content.length);
	    return buf;
    }
	
}
