package com.rtk.dmp;

public class DecodeImageState 
{
	
	 public final static int STATE_NOT_DECODED = 0;
	 public final static int STATE_DECODEING   = 1;
	 public final static int STATE_DECODE_DONE = 2;
	 public final static int STATE_DECODE_RESULT_SUCCESS = 3;
	 public final static int STATE_DECODE_RESULT_FAIL = 4;
	 public final static int STATE_DECODE_RESULT_INIT = 5;
	 public final static int STATE_DECODE_RETRY = 6;
	 public final static int STATE_DECODE_CANCEL = 7;
	 public final static int STATE_DECODE_TRANSITTING = -9;
}

