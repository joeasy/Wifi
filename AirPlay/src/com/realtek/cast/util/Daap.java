package com.realtek.cast.util;

import android.util.Log;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Digital Audio Access Protocol (DAAP)
 * http://www.gyfgafguf.dk/raspbian/daapdocs.txt
 * @author Jason Lin
 *
 */
public class Daap {
	
	private static final String TAG = "Daap";
	
//	public static final int CODE_MDCL = 'm' << 24 | 'd' << 16 | 'c' << 8 | 'l'; /* dmap.dictionary */
	public static final int CODE_MSTT = 'm' << 24 | 's' << 16 | 't' << 8 | 't'; /* dmap.status */
	public static final int CODE_MIID = 'm' << 24 | 'i' << 16 | 'i' << 8 | 'd'; /* dmap.itemid */
	public static final int CODE_MINM = 'm' << 24 | 'i' << 16 | 'n' << 8 | 'm'; /* dmap.itemname */
	public static final int CODE_MIKD = 'm' << 24 | 'i' << 16 | 'k' << 8 | 'd'; /* dmap.itemkind */
	public static final int CODE_MPER = 'm' << 24 | 'p' << 16 | 'e' << 8 | 'r'; /* dmap.persistentid */
	public static final int CODE_MCON = 'm' << 24 | 'c' << 16 | 'o' << 8 | 'n'; /* dmap.container */
	public static final int CODE_MCTI = 'm' << 24 | 'c' << 16 | 't' << 8 | 'i'; /* dmap.containeritemid */
	public static final int CODE_MPCO = 'm' << 24 | 'p' << 16 | 'c' << 8 | 'o'; /* dmap.parentcontainerid */
	public static final int CODE_MSTS = 'm' << 24 | 's' << 16 | 't' << 8 | 's'; /* dmap.statusstring */
	public static final int CODE_MIMC = 'm' << 24 | 'i' << 16 | 'm' << 8 | 'c'; /* dmap.itemcount */
	public static final int CODE_MRCO = 'm' << 24 | 'r' << 16 | 'c' << 8 | 'o'; /* dmap.returnedcount */
	public static final int CODE_MTCO = 'm' << 24 | 't' << 16 | 'c' << 8 | 'o'; /* dmap.specifiedtotalcount */
	public static final int CODE_MLCL = 'm' << 24 | 'l' << 16 | 'c' << 8 | 'l'; /* dmap.listing */
	public static final int CODE_MLIT = 'm' << 24 | 'l' << 16 | 'i' << 8 | 't'; /* dmap.listingitem */
	public static final int CODE_MBCL = 'm' << 24 | 'b' << 16 | 'c' << 8 | 'l'; /* dmap.bag */
	public static final int CODE_MDCL = 'm' << 24 | 'd' << 16 | 'c' << 8 | 'l'; /* dmap.dictionary */
	public static final int CODE_MSRV = 'm' << 24 | 's' << 16 | 'r' << 8 | 'v'; /* dmap.serverinforesponse */
	//TODO: public static final int CODE_MSAUD = 'm' << 24 | 's' << 16 | 'a' << 8 | 'u'; /* dmap.authenticationmethod */
	public static final int CODE_MSLR = 'm' << 24 | 's' << 16 | 'l' << 8 | 'r'; /* dmap.loginrequired */
	public static final int CODE_MPRO = 'm' << 24 | 'p' << 16 | 'r' << 8 | 'o'; /* dmap.protocolversion */
	public static final int CODE_APRO = 'a' << 24 | 'p' << 16 | 'r' << 8 | 'o'; /* daap.protocolversion */
	public static final int CODE_MSAL = 'm' << 24 | 's' << 16 | 'a' << 8 | 'l'; /* dmap.supportsuatologout */
	public static final int CODE_MSUP = 'm' << 24 | 's' << 16 | 'u' << 8 | 'p'; /* dmap.supportsupdate */
	public static final int CODE_MSPI = 'm' << 24 | 's' << 16 | 'p' << 8 | 'i'; /* dmap.supportspersistentids */
	public static final int CODE_MSEX = 'm' << 24 | 's' << 16 | 'e' << 8 | 'x'; /* dmap.supportsextensions */
	public static final int CODE_MSBR = 'm' << 24 | 's' << 16 | 'b' << 8 | 'r'; /* dmap.supportsbrowse */
	public static final int CODE_MSQY = 'm' << 24 | 's' << 16 | 'q' << 8 | 'y'; /* dmap.supportsquery */
	public static final int CODE_MSIX = 'm' << 24 | 's' << 16 | 'i' << 8 | 'x'; /* dmap.supportsindex */
	public static final int CODE_MSRS = 'm' << 24 | 's' << 16 | 'r' << 8 | 's'; /* dmap.supportsresolve */
	public static final int CODE_MSTM = 'm' << 24 | 's' << 16 | 't' << 8 | 'm'; /* dmap.timeoutinterval */
	public static final int CODE_MSDC = 'm' << 24 | 's' << 16 | 'd' << 8 | 'c'; /* dmap.databasescount */
	public static final int CODE_MCCR = 'm' << 24 | 'c' << 16 | 'c' << 8 | 'r'; /* dmap.contentcodesresponse */
	public static final int CODE_MCNM = 'm' << 24 | 'c' << 16 | 'n' << 8 | 'm'; /* dmap.contentcodesnumber */
	public static final int CODE_MCNA = 'm' << 24 | 'c' << 16 | 'n' << 8 | 'a'; /* dmap.contentcodesname */
	public static final int CODE_MCTY = 'm' << 24 | 'c' << 16 | 't' << 8 | 'y'; /* dmap.contentcodestype */
	public static final int CODE_MLOG = 'm' << 24 | 'l' << 16 | 'o' << 8 | 'g'; /* dmap.loginresponse */
	public static final int CODE_MLID = 'm' << 24 | 'l' << 16 | 'i' << 8 | 'd'; /* dmap.sessionid */
	public static final int CODE_MUPD = 'm' << 24 | 'u' << 16 | 'p' << 8 | 'd'; /* dmap.updateresponse */
	public static final int CODE_MSUR = 'm' << 24 | 's' << 16 | 'u' << 8 | 'r'; /* dmap.serverrevision */
	public static final int CODE_MUTY = 'm' << 24 | 'u' << 16 | 't' << 8 | 'y'; /* dmap.updatetype */
	public static final int CODE_MUDL = 'm' << 24 | 'u' << 16 | 'd' << 8 | 'l'; /* dmap.deletedidlisting */
	public static final int CODE_AVDB = 'a' << 24 | 'v' << 16 | 'd' << 8 | 'b'; /* daap.serverdatabases */
	public static final int CODE_ABRO = 'a' << 24 | 'b' << 16 | 'r' << 8 | 'o'; /* daap.databasebrowse */
	public static final int CODE_ABAL = 'a' << 24 | 'b' << 16 | 'a' << 8 | 'l'; /* daap.browsealbumlistung */
	public static final int CODE_ABAR = 'a' << 24 | 'b' << 16 | 'a' << 8 | 'r'; /* daap.browseartistlisting */
	public static final int CODE_ABCP = 'a' << 24 | 'b' << 16 | 'c' << 8 | 'p'; /* daap.browsecomposerlisting */
	public static final int CODE_ABGN = 'a' << 24 | 'b' << 16 | 'g' << 8 | 'n'; /* daap.browsegenrelisting */
	public static final int CODE_ADBS = 'a' << 24 | 'd' << 16 | 'b' << 8 | 's'; /* daap.databasesongs */
	public static final int CODE_ASAL = 'a' << 24 | 's' << 16 | 'a' << 8 | 'l'; /* daap.songalbum */
	public static final int CODE_ASAR = 'a' << 24 | 's' << 16 | 'a' << 8 | 'r'; /* daap.songartist */
	public static final int CODE_ASBT = 'a' << 24 | 's' << 16 | 'b' << 8 | 't'; /* daap.songsbeatsperminute */
	public static final int CODE_ASBR = 'a' << 24 | 's' << 16 | 'b' << 8 | 'r'; /* daap.songbitrate */
	public static final int CODE_ASCM = 'a' << 24 | 's' << 16 | 'c' << 8 | 'm'; /* daap.songcomment */
	public static final int CODE_ASCO = 'a' << 24 | 's' << 16 | 'c' << 8 | 'o'; /* daap.songcompilation */
	public static final int CODE_ASDA = 'a' << 24 | 's' << 16 | 'd' << 8 | 'a'; /* daap.songdateadded */
	public static final int CODE_ASDM = 'a' << 24 | 's' << 16 | 'd' << 8 | 'm'; /* daap.songdatemodified */
	public static final int CODE_ASDC = 'a' << 24 | 's' << 16 | 'd' << 8 | 'c'; /* daap.songdisccount */
	public static final int CODE_ASDN = 'a' << 24 | 's' << 16 | 'd' << 8 | 'n'; /* daap.songdiscnumber */
	public static final int CODE_ASDB = 'a' << 24 | 's' << 16 | 'd' << 8 | 'b'; /* daap.songdisabled */
	public static final int CODE_ASEQ = 'a' << 24 | 's' << 16 | 'e' << 8 | 'q'; /* daap.songeqpreset */
	public static final int CODE_ASFM = 'a' << 24 | 's' << 16 | 'f' << 8 | 'm'; /* daap.songformat */
	public static final int CODE_ASGN = 'a' << 24 | 's' << 16 | 'g' << 8 | 'n'; /* daap.songgenre */
	public static final int CODE_ASDT = 'a' << 24 | 's' << 16 | 'd' << 8 | 't'; /* daap.songdescription */
	public static final int CODE_ASRV = 'a' << 24 | 's' << 16 | 'r' << 8 | 'v'; /* daap.songrelativevolume */
	public static final int CODE_ASSR = 'a' << 24 | 's' << 16 | 's' << 8 | 'r'; /* daap.songsamplerate */
	public static final int CODE_ASSZ = 'a' << 24 | 's' << 16 | 's' << 8 | 'z'; /* daap.songsize */
	public static final int CODE_ASST = 'a' << 24 | 's' << 16 | 's' << 8 | 't'; /* daap.songstarttime */
	public static final int CODE_ASSP = 'a' << 24 | 's' << 16 | 's' << 8 | 'p'; /* daap.songstoptime */
	public static final int CODE_ASTM = 'a' << 24 | 's' << 16 | 't' << 8 | 'm'; /* daap.songtime */
	public static final int CODE_ASTC = 'a' << 24 | 's' << 16 | 't' << 8 | 'c'; /* daap.songtrackcount */
	public static final int CODE_ASTN = 'a' << 24 | 's' << 16 | 't' << 8 | 'n'; /* daap.songtracknumber */
	public static final int CODE_ASUR = 'a' << 24 | 's' << 16 | 'u' << 8 | 'r'; /* daap.songuserrating */
	public static final int CODE_ASYR = 'a' << 24 | 's' << 16 | 'y' << 8 | 'r'; /* daap.songyear */
	public static final int CODE_ASDK = 'a' << 24 | 's' << 16 | 'd' << 8 | 'k'; /* daap.songdatakind */
	public static final int CODE_ASUL = 'a' << 24 | 's' << 16 | 'u' << 8 | 'l'; /* daap.songdataurl */
	public static final int CODE_APLY = 'a' << 24 | 'p' << 16 | 'l' << 8 | 'y'; /* daap.databaseplaylists */
	public static final int CODE_ABPL = 'a' << 24 | 'b' << 16 | 'p' << 8 | 'l'; /* daap.baseplaylist */
	public static final int CODE_APSO = 'a' << 24 | 'p' << 16 | 's' << 8 | 'o'; /* daap.playlistsongs */
	public static final int CODE_PRSV = 'p' << 24 | 'r' << 16 | 's' << 8 | 'v'; /* daap.resolve */
	public static final int CODE_ARIF = 'a' << 24 | 'r' << 16 | 'i' << 8 | 'f'; /* daap.resolveinfo */
	public static final int CODE_AENV = 'a' << 24 | 'e' << 16 | 'N' << 8 | 'V'; /* com.apple.itunes.norm-volume */
	public static final int CODE_AESP = 'a' << 24 | 'e' << 16 | 'S' << 8 | 'P'; /* com.apple.itunes.smart-playlist */
	
	public static Class<?> getType(int tag) {
		switch (tag) {
			case CODE_MSTT: return int.class;
			case CODE_MIID: return int.class;
			case CODE_MINM: return String.class;
			case CODE_MIKD: return byte.class;
			case CODE_MPER: return long.class;
			case CODE_MCON: return Daap.class;
			case CODE_MCTI: return int.class;
			case CODE_MPCO: return int.class;
			case CODE_MSTS: return String.class;
			case CODE_MIMC: return int.class;
			case CODE_MRCO: return int.class;
			case CODE_MTCO: return int.class;
			case CODE_MLCL: return Daap.class;
			case CODE_MLIT: return Daap.class;
			case CODE_MBCL: return Daap.class;
			case CODE_MDCL: return Daap.class;
			case CODE_MSRV: return Daap.class;
// TODO:	case CODE_MSAUD: return byte.class;
			case CODE_MSLR: return byte.class;
// TODO:	case CODE_MPRO: return version.class;
// TODO:	case CODE_APRO: return version.class;
			case CODE_MSAL: return byte.class;
			case CODE_MSUP: return byte.class;
			case CODE_MSPI: return byte.class;
			case CODE_MSEX: return byte.class;
			case CODE_MSBR: return byte.class;
			case CODE_MSQY: return byte.class;
			case CODE_MSIX: return byte.class;
			case CODE_MSRS: return byte.class;
			case CODE_MSTM: return int.class;
			case CODE_MSDC: return int.class;
			case CODE_MCCR: return Daap.class;
			case CODE_MCNM: return int.class;
			case CODE_MCNA: return String.class;
			case CODE_MCTY: return short.class;
			case CODE_MLOG: return Daap.class;
			case CODE_MLID: return int.class;
			case CODE_MUPD: return Daap.class;
			case CODE_MSUR: return int.class;
			case CODE_MUTY: return byte.class;
			case CODE_MUDL: return Daap.class;
			case CODE_AVDB: return Daap.class;
			case CODE_ABRO: return Daap.class;
			case CODE_ABAL: return Daap.class;
			case CODE_ABAR: return Daap.class;
			case CODE_ABCP: return Daap.class;
			case CODE_ABGN: return Daap.class;
			case CODE_ADBS: return Daap.class;
			case CODE_ASAL: return String.class;
			case CODE_ASAR: return String.class;
			case CODE_ASBT: return short.class;
			case CODE_ASBR: return short.class;
			case CODE_ASCM: return String.class;
			case CODE_ASCO: return byte.class;
			case CODE_ASDA: return Date.class;
			case CODE_ASDM: return Date.class;
			case CODE_ASDC: return short.class;
			case CODE_ASDN: return short.class;
			case CODE_ASDB: return byte.class;
			case CODE_ASEQ: return String.class;
			case CODE_ASFM: return String.class;
			case CODE_ASGN: return String.class;
			case CODE_ASDT: return String.class;
			case CODE_ASRV: return byte.class;
			case CODE_ASSR: return int.class;
			case CODE_ASSZ: return int.class;
			case CODE_ASST: return int.class;
			case CODE_ASSP: return int.class;
			case CODE_ASTM: return int.class;
			case CODE_ASTC: return short.class;
			case CODE_ASTN: return short.class;
			case CODE_ASUR: return byte.class;
			case CODE_ASYR: return short.class;
			case CODE_ASDK: return byte.class;
			case CODE_ASUL: return String.class;
			case CODE_APLY: return Daap.class;
			case CODE_ABPL: return byte.class;
			case CODE_APSO: return Daap.class;
			case CODE_PRSV: return Daap.class;
			case CODE_ARIF: return Daap.class;
			case CODE_AENV: return int.class;
			case CODE_AESP: return byte.class;
			default:
				return null;
		}
	}
	
	public static Daap wrap(byte[] data, int offset, int length) {
		ByteBuffer buf = ByteBuffer.wrap(data, offset, length);
		return new Daap(buf);
	}
	
	private final SparseArray<Object> mMap = new SparseArray<Object>();
	
	private Daap(ByteBuffer buffer) {
		while(buffer.position() < buffer.capacity() - 8) {
			int tag = buffer.getInt();
			int size = buffer.getInt();
			if (size > buffer.capacity()) {
				size = buffer.capacity();
			}
			Class<?> type = getType(tag);
			if (type == Daap.class) {
				mMap.put(tag, Daap.wrap(buffer.array(), buffer.position(), size));
				
			} else if (type == int.class && checkSize(size, 4, buffer)) {
				mMap.put(tag, buffer.getInt());
				
			} else if (type == long.class && checkSize(size, 8, buffer)) {
				mMap.put(tag, buffer.getLong());
				
			} else if (type == short.class && checkSize(size, 2, buffer)) {
				mMap.put(tag, buffer.getShort());
				
			} else if (type == byte.class && checkSize(size, 1, buffer)) {
				mMap.put(tag, buffer.get());
				
			} else if (type == Date.class && checkSize(size, 4, buffer)) {
				int d = buffer.getInt();
				Date date = new Date(d & 0xFFFFFFFFL);
				mMap.put(tag, date);
				
			} else if (type == String.class) {
				mMap.put(tag, new String(buffer.array(), buffer.position(), size));
				buffer.position(buffer.position() + size);
				
			} else {
				byte[] str = new byte[4];
				for (int i = 0; i < 4; i++) {
					str[3 - i] = (byte) ((tag >> 8 * i) & 0xFF);
				}
				Log.w(TAG, String.format("Unhandled item: tag=%s, size=%d", new String(str), size));
				buffer.position(buffer.position() + size);
			}
		}
	}
	
	private static final boolean checkSize(int size, int correctSize, ByteBuffer buffer) {
		if (size != correctSize) {
			buffer.position(buffer.position() + size);
			Log.e(TAG, String.format("Invalid size: %d/%d",size, correctSize));
			return false;
		}
		return true;
	}
	
	public String getValue(int code) {
		String value = null;
		Object obj = mMap.get(code);
		if (obj != null) {
			value = obj.toString();
		}
		return value;
	}
}
