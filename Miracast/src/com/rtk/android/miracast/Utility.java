package com.rtk.android.miracast;

public class Utility {
	
	public static String SI_LANG_TO_ISO639(int langId) {
		switch(langId) {
		case (('e'<<8)|'n'):
		case (('e'<<16)|('n'<<8)|'g'):	
			return "English";
		case (('z'<<8)|'h'):			
		case (('c'<<16)|('h'<<8)|'i'):
		case (('z'<<16)|('h'<<8)|'o'):
			return "Chinese";
		case (('j'<<8)|'a'):
		case (('j'<<16)|('p'<<8)|'n'):
			return "Japanese";
		case (('e'<<8)|'s'):	
		case (('s'<<16)|('p'<<8)|'a'):	
			return "Spanish";
		case (('f'<<8)|'r'):
		case (('f'<<16)|('r'<<8)|'a'):
		case (('f'<<16)|('r'<<8)|'e'):
			return "French";
		case (('d'<<8)|'e'):
		case (('g'<<16)|('e'<<8)|'r'):
		case (('d'<<16)|('e'<<8)|'u'):
			return "German";
		case (('i'<<8)|'t'):
		case (('i'<<16)|('t'<<8)|'a'):
			return "Italian";
		case (('i'<<8)|'n'):
		case (('i'<<16)|('n'<<8)|'d'):
			return "Indonesian";
		case (('k'<<8)|'o'):
		case (('k'<<16)|('o'<<8)|'r'):
			return "Korean";
		case (('t'<<8)|'h'):
		case (('t'<<16)|('h'<<8)|'a'):
			return "Thai";
		case (('p'<<8)|'t'):
		case (('p'<<16)|('o'<<8)|'r'):
			return "Portuguese";
		case (('n'<<8)|'l'):
		case (('d'<<16)|('u'<<8)|'t'):
		case (('n'<<16)|('l'<<8)|'d'):
			return "Dutch";
		case (('c'<<8)|'s'):
		case (('c'<<16)|('z'<<8)|'e'):
		case (('c'<<16)|('e'<<8)|'s'):
			return "Czech";
		case (('h'<<8)|'u'):
		case (('h'<<8)|('u'<<8)|'n'):
			return "Hungarian";
		case (('r'<<8)|'u'):
		case (('r'<<16)|('u'<<8)|'s'):
			return "Russian";
		case (('p'<<8)|'l'):
		case (('p'<<16)|('o'<<8)|'l'):
			return "Polish";
		case (('f'<<8)|'i'):
		case (('f'<<16)|('i'<<8)|'n'):
			return "Finnish";
		case (('n'<<8)|'o'):
		case (('n'<<16)|('o'<<8)|'r'):
			return "Norwegian";
		case (('s'<<8)|'v'):
		case (('s'<<16)|('w'<<8)|'e'):
			return "Swedish";
		case (('d'<<8)|'a'):
		case (('d'<<16)|('a'<<8)|'n'):
			return "Danish";
		case (('e'<<8)|'l'):
		case (('g'<<16)|('r'<<8)|'c'):
		case (('g'<<16)|('r'<<8)|'e'):
		case (('e'<<16)|('l'<<8)|'l'):
			return "Greek";
		case (('g'<<8)|'a'):
		case (('g'<<16)|('l'<<8)|'e'):
			return "Irish";
		case (('h'<<8)|'i'):
		case (('h'<<16)|('i'<<8)|'n'):
			return "Hindi";
		case (('s'<<8)|'l'):
		case (('s'<<16)|'l'<<8|'v'):
			return "Slovenian";
		case (('i'<<8)|'w'):
		case (('h'<<8)|'e'):
		case (('h'<<16)|('e'<<8)|'b'):
			return "Hebrew";
		case (('t'<<8)|'r'):
		case (('t'<<16)|('u'<<8)|'r'):
			return "Turkish";
		case (('r'<<8)|'o'):
		case (('r'<<16)|('u'<<8)|'m'):
		case (('r'<<16)|('o'<<8)|'n'):
			return "Romanian";
		case (('a'<<8)|'r'):
		case (('a'<<16)|('r'<<8)|'a'):
			return "Arabic";
		case (('b'<<8)|'g'):
		case (('b'<<16)|('u'<<8)|'l'):
			return "Bulgarian";
		case (('l'<<8)|'a'):
		case (('l'<<16)|'a'<<8|'t'):
			return "Latin";
		case (('l'<<8)|'t'):
		case (('l'<<16)|('i'<<8)|'t'):
			return "Lithuanian";
		case (('l'<<8)|'v'):
		case (('l'<<16)|('a'<<8)|'v'):
			return "Latvian";
		case (('m'<<8)|'s'):
		case (('m'<<16)|('a'<<8)|'y'):
		case (('m'<<16)|('s'<<8)|'a'):
			return "Malay";  
		case (('m'<<8)|'y'):
		case (('b'<<16)|('u'<<8)|'r'):
		case (('m'<<16)|('y'<<8)|'a'):
			return "Burmese";
		case (('n'<<8)|'e'):
		case (('n'<<16)|('e'<<8)|'p'):
			return "Nepali";
		case (('s'<<8)|'k'):
		case (('s'<<16)|('l'<<8)|'o'):
		case (('s'<<16)|('l'<<8)|'k'):
			return "Slovaks";
		case (('s'<<8)|'q'):
		case (('a'<<16)|('l'<<8)|'b'):
		case (('s'<<16)|('q'<<8)|'i'):
			return "Albanian";
		case (('s'<<8)|'r'):
		case (('s'<<16)|('r'<<8)|'p'):
			return "Serbia";
		case (('u'<<8)|'k'):
		case (('u'<<16)|('k'<<8)|'r'):
			return "Ukrainian";
		case (('v'<<8)|'i'):
		case (('v'<<16)|('i'<<8)|'e'):
			return "Vietnamese";
		case (('h'<<8)|'r'):
		case (('h'<<16)|('r'<<8)|'v'):
			return "Croatian";
		case (('h'<<8)|'y'):
		case (('a'<<16)|('r'<<8)|'m'):
		case (('h'<<16)|('y'<<8)|'e'):
			return "Armenian";
		case (('i'<<8)|'s'):
		case (('i'<<16)|('c'<<8)|'e'):
		case (('i'<<16)|('s'<<8)|'l'):
			return "Icelandic";
		case (('k'<<8)|'m'):
		case (('k'<<16)|('h'<<8)|'m'):
			return "Cambodian";
		case (('c'<<16)|('a'<<8)|'t'):
			return "Catalan";
		case (('e'<<8)|'t'):
		case (('e'<<16)|('s'<<8)|'t'):
			return "Estonian";
		default:
			return null;
		}
	}
	
	public static String AUDIO_TYPE_TABLE(int audioType)
	{
		switch(audioType){
		default:
		case -1:
			return "Unknown";
		case 0x00001000:
			return "MPEG";
		case 0x00001001:
			return "Dolby AC3";
		case 0x00001002:
			return "Dolby Digital Plus";
		case 0x00001003:
			return "PCM";
		case 0x00001004:
			return "DTS";
		case 0x00001005:
			return "DTS HD";
		case 0x00001006:
			return "AAC";
		case 0x00001007:
			return "RAW AAC";
		case 0x00001008:
			return "MP4";
		case 0x00001009:
			return "WMA";
		case 0x0000100a:
			return "WMAPRO";
		case 0x0000100b:
			return "OGG";
		case 0x0000100c:
			return "RealAudio Cook";
		case 0x0000100d:
			return "RealAudio Lossless";
		case 0x0000100e:
			return "ADPCM";
		case 0x0000100f:
			return "ULAW";
		case 0x00001010:
			return "ALAW";
		case 0x00001011:
			return "FLAC";
		case 0x00001012:
			return "trueHD";
		case 0x00001013:
			return "AMRWB";
		case 0x00001014:
			return "AMRNB";
		case 0x00001015:
			return "APE";
		case 0x00001016:
			return "SILK";
		case 0x00001017:
			return "G729";
		case 0x00001018:
			return "DV";
		}
	}
	
	public static String language_code_map(String lang)
	{
		//Map 639-2/T TO 639-2/B
		if(lang.compareTo("eng") == 0)
			return "eng";
		else if(lang.compareTo("zho") == 0)
			return "chi";
		else if(lang.compareTo("msa") == 0)
			return "may";
		else if(lang.compareTo("tha") == 0)
			return "tha";
		else if(lang.compareTo("vie") == 0)
			return "vie";
		else if(lang.compareTo("rus") == 0)
			return "rus";
		else if(lang.compareTo("ara") == 0)
			return "ara";
		else if(lang.compareTo("fas") == 0)
			return "per";
		else if(lang.compareTo("fra") == 0)
			return "fre";
		else if(lang.compareTo("ind") == 0)
			return "ind";
		else if(lang.compareTo("heb") == 0)
			return "heb";
		else
			return null;
	}
}
