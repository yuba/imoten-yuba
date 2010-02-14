/*
 * imoten - i mode.net mail tensou(forward)
 * 
 * Copyright (C) 2010 shoozhoo (http://code.google.com/p/imoten/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package immf;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ContentType;
import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import net.htmlparser.jericho.Source;

import org.apache.commons.lang.StringUtils;

public class Util {
	public static void safeclose(Closeable c){
		if(c!=null){
			try{
				c.close();
			}catch (Exception e) {}
		}
	}
	
	/**
	 * 「〜」などの文字を置き換える
	 * @param s
	 * @return
	 */
	public static String replaceUnicodeMapping(String s){
		if(StringUtils.isEmpty(System.getProperty("sun.nio.cs.map"))){
			s = StringUtils.replace(s, "\uff5e", "\u301c");
			s = StringUtils.replace(s, "\u2225", "\u2016");
			s = StringUtils.replace(s, "\uff0d", "\u2212");
			s = StringUtils.replace(s, "\uffe0", "\u00a2");
			s = StringUtils.replace(s, "\uffe1", "\u00a3");
			s = StringUtils.replace(s, "\uffe2", "\u00ac");
			s = StringUtils.replace(s, "\u2015", "\u2014");
		}
		// sun.nio.cs.map が設定されている場合はiso-2022-jpの代わりに
		// x-windows-iso2022jpが使用され自動で変換されるので置き換えない

		return s;
	}
	
	public static String reverseReplaceUnicodeMapping(String s){
		s = StringUtils.replace(s, "\u301c", "\uff5e");
		s = StringUtils.replace(s, "\u2016", "\u2225");
		s = StringUtils.replace(s, "\u2212", "\uff0d");
		s = StringUtils.replace(s, "\u00a2", "\uffe0");
		s = StringUtils.replace(s, "\u00a3", "\uffe1");
		s = StringUtils.replace(s, "\u00ac", "\uffe2");
		s = StringUtils.replace(s, "\u2014", "\u2015");
		return s;
	}
	
	public static String easyEscapeHtml(String s){
		StringBuilder buf = new StringBuilder();
		for(char c : s.toCharArray()){
			if(c=='>'){
				buf.append("&gt;");
			}else if(c=='<'){
				buf.append("&lt;");
			}else if(c=='&'){
				buf.append("&amp;");
			}else if(c=='"'){
				buf.append("&quot;");
			}else{
				buf.append(c);
			}
		}
		return buf.toString();
	}

	
	/*
	 * JavaMail完全解説 のページから使用させていただきました
	 * http://www.sk-jp.com/book/javamail/contents/
	 */
	public static void setFileName(Part part, String filename,
			String charset, String lang)
	throws MessagingException {

		ContentDisposition disposition;
		String[] strings = part.getHeader("Content-Disposition");
		if (strings == null || strings.length < 1) {
			disposition = new ContentDisposition(Part.ATTACHMENT);
		} else {
			disposition = new ContentDisposition(strings[0]);
			disposition.getParameterList().remove("filename");
		}

		part.setHeader("Content-Disposition",
				disposition.toString() +
				encodeParameter("filename", filename, charset, lang));

		ContentType cType;
		strings = part.getHeader("Content-Type");
		if (strings == null || strings.length < 1) {
			cType = new ContentType(part.getDataHandler().getContentType());
		} else {
			cType = new ContentType(strings[0]);
		}

		try {
			// I want to public the MimeUtility#doEncode()!!!
			String mimeString = MimeUtility.encodeWord(filename, charset, "B");
			// cut <CRLF>...
			StringBuffer sb = new StringBuffer();
			int i;
			while ((i = mimeString.indexOf('\r')) != -1) {
				sb.append(mimeString.substring(0, i));
				mimeString = mimeString.substring(i + 2);
			}
			sb.append(mimeString);

			cType.setParameter("name", new String(sb));
		} catch (UnsupportedEncodingException e) {
			throw new MessagingException("Encoding error", e);
		}
		part.setHeader("Content-Type", cType.toString());
	}

	public static String encodeParameter(String name, String value,
			String encoding, String lang) {
		StringBuffer result = new StringBuffer();
		StringBuffer encodedPart = new StringBuffer();

		boolean needWriteCES = !isAllAscii(value);
		boolean CESWasWritten = false;
		boolean encoded;
		boolean needFolding = false;
		int sequenceNo = 0;
		int column;

		while (value.length() > 0) {
			// index of boundary of ascii/non ascii
			int lastIndex;
			boolean isAscii = value.charAt(0) < 0x80;
			for (lastIndex = 1; lastIndex < value.length(); lastIndex++) {
				if (value.charAt(lastIndex) < 0x80) {
					if (!isAscii) break;
				} else {
					if (isAscii) break;
				}
			}
			if (lastIndex != value.length()) needFolding = true;

			RETRY:      while (true) {
				encodedPart.delete(0, encodedPart.length());
				String target = value.substring(0, lastIndex);

				byte[] bytes;
				try {
					if (isAscii) {
						bytes = target.getBytes("us-ascii");
					} else {
						bytes = target.getBytes(encoding);
					}
				} catch (UnsupportedEncodingException e) {
					bytes = target.getBytes(); // use default encoding
					encoding = MimeUtility.mimeCharset(
							MimeUtility.getDefaultJavaCharset());
				}

				encoded = false;
				// It is not strict.
				column = name.length() + 7; // size of " " and "*nn*=" and ";"

				for (int i = 0; i < bytes.length; i++) {
					if (bytes[i] > ' ' && bytes[i] < 'z'
							&& HeaderTokenizer.MIME.indexOf((char)bytes[i]) < 0) {
						encodedPart.append((char)bytes[i]);
						column++;
					} else {
						encoded = true;
						encodedPart.append('%');
						String hex  = Integer.toString(bytes[i] & 0xff, 16);
						if (hex.length() == 1) {
							encodedPart.append('0');
						}
						encodedPart.append(hex);
						column += 3;
					}
					if (column > 76) {
						needFolding = true;
						lastIndex /= 2;
						continue RETRY;
					}
				}

				result.append(";\r\n ").append(name);
				if (needFolding) {
					result.append('*').append(sequenceNo);
					sequenceNo++;
				}
				if (!CESWasWritten && needWriteCES) {
					result.append("*=");
					CESWasWritten = true;
					result.append(encoding).append('\'');
					if (lang != null) result.append(lang);
					result.append('\'');
				} else if (encoded) {
					result.append("*=");
				} else {
					result.append('=');
				}
				result.append(new String(encodedPart));
				value = value.substring(lastIndex);
				break;
			}
		}
		return new String(result);
	}
	
	public static String html2text(String html){
		Source src = new Source(html);
		return src.getRenderer().toString();
	}
	/*
	 * From,CCなどの情報をBodyの先頭に付加する場合の文字列
	 */
	public static String getHeaderInfo(ImodeMail imm, boolean isHtml, boolean subjectEmojiReplace){
		StringBuilder buf = new StringBuilder();
		StringBuilder header = new StringBuilder();

		header.append(" From:    ").append(imm.getFromAddr().toUnicodeString()).append("\r\n");
		header.append(" To:      ").append(imm.getMyMailAddr()).append("\r\n");
		for(InternetAddress addr : imm.getToAddrList()){
			header.append("          ").append(addr.toUnicodeString()).append("\r\n");
		}
		String prefix = " Cc:";
		for(InternetAddress addr : imm.getCcAddrList()){
			header.append(prefix+"      ").append(addr.toUnicodeString()).append("\r\n");
			prefix = "    ";
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd (EEE) HH:mm:ss");
		header.append(" Date:    ").append(df.format(imm.getTimeDate())).append("\r\n");
		String subject = imm.getSubject();
		if(subjectEmojiReplace){
			subject = EmojiUtil.replaceToLabel(subject);
		}
		header.append(" Subject: ").append(subject).append("\r\n");
		
		
		if(isHtml){
			buf.append("<pre style=\"white-space:pre-wrap;word-wrap:break-word;\">");
		}
		buf.append("----").append("\r\n");
		if(isHtml){
			buf.append(Util.easyEscapeHtml(header.toString()));
		}else{
			buf.append(header.toString());
		}
		buf.append("----").append("\r\n");
		if(isHtml){
			buf.append("</pre>");
		}
		buf.append("\r\n");
		return buf.toString();
	}
	/** check if contains only ascii characters in text. */
	public static boolean isAllAscii(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) > 0x7f) { // non-ascii
				return false;
			}
		}
		return true;
	}
}
