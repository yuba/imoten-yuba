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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ContentType;
import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

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

		// UTF-8の「ゔ 」(1文字)はShift_JISで変換されないので「う゛」(2文字)に変換
		s = StringUtils.replace(s, "\u3094", "う゛");

		// 通常日本語では使用しないはずの半角記号
		s = StringUtils.replace(s, "\u00ab", "\u226a"); // 「«」→「≪」
		s = StringUtils.replace(s, "\u00af", "\uffe3"); // 「¯」→「￣」
		s = StringUtils.replace(s, "\u00b5", "\u03bc"); // 「µ」→「μ」
		s = StringUtils.replace(s, "\u00b7", "\u30fb"); // 「·」→「・」
		s = StringUtils.replace(s, "\u00b8", "\uff0c"); // 「¸」→「，」
		s = StringUtils.replace(s, "\u00bb", "\u226b"); // 「»」→「≫」
		s = StringUtils.replace(s, "\u203e", "\u007e"); // 「‾」→「~」
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

    public static String getFileName(Part part) throws MessagingException {
        String[] disposition = part.getHeader("Content-Disposition");
        if (disposition == null || disposition.length < 1) {
            return null;
        }
        // 本来そのまま返すところだが日本固有のデコード処理を挟む。
        return decodeParameterSpciallyJapanese(
        		getParameter(disposition[0], "filename"));
    }

    static class Encoding {
        String encoding = "us-ascii";
        String lang = "";
    }

    public static String getParameter(String header, String name) {

        HeaderTokenizer tokenizer =
                new HeaderTokenizer(header, HeaderTokenizer.MIME, true);
        HeaderTokenizer.Token token;
        StringBuffer sb = new StringBuffer();
        // It is specified in first encoded-part.
        Encoding encoding = new Encoding();

        String n;
        String v;

        try {
            while (true) {
                token = tokenizer.next();
                if (token.getType() == HeaderTokenizer.Token.EOF) break;
                if (token.getType() != ';') continue;

                token = tokenizer.next();
                checkType(token);
                n = token.getValue();

                token = tokenizer.next();
                if (token.getType() != '=') {
                    throw new ParseException(
                            "Illegal token : " + token.getValue());
                }

                token = tokenizer.next();
                checkType(token);
                v = token.getValue();

                if (n.equalsIgnoreCase(name)) {
                    // It is not divided and is not encoded.
                    return v;
                }

                int index = name.length();

                if (!n.startsWith(name) || n.charAt(index) != '*') {
                    // another parameter
                    continue;
                }
                // be folded, or be encoded
                int lastIndex = n.length() - 1;
                if (n.charAt(lastIndex) == '*') {
                    sb.append(decodeRFC2231(v, encoding));
                } else {
                    sb.append(v);
                }
                if (index == lastIndex) {
                    // not folding
                    break;
                }
            }
            return new String(sb);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        throw new InternalError();
    }

    private static void checkType(HeaderTokenizer.Token token)
                throws ParseException {
        int t = token.getType();
        if (t != HeaderTokenizer.Token.ATOM &&
            t != HeaderTokenizer.Token.QUOTEDSTRING) {
            throw new ParseException("Illegal token : " + token.getValue());
        }
    }

    // "lang" tag is ignored...
    private static String decodeRFC2231(String s, Encoding encoding)
                throws ParseException, UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        int i = 0;

        int work = s.indexOf('\'');
        if (work > 0) {
            encoding.encoding = s.substring(0, work);
            work++;
            i = s.indexOf('\'', work);
            encoding.lang = s.substring(work, i);
            i++;
        }

        try {
            for (; i < s.length(); i++) {
                if (s.charAt(i) == '%') {
                    sb.append((char)Integer.parseInt(
                            s.substring(i + 1, i + 3), 16));
                    i += 2;
                    continue;
                }
                sb.append(s.charAt(i));
            }
            return new String(
                    new String(sb).getBytes("ISO-8859-1"),
                    encoding.encoding);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException(s + " :: this string were not decoded.");
        }
    }

    public static String decodeParameterSpciallyJapanese(String s)
    		throws ParseException {
    	try {
    		boolean unicode = false;
    		for (int i = 0; i < s.length(); i++) {
			    if (s.charAt(i) > 0xff) { // Unicode
			        unicode = true;
			        break;
			    }
			}
			if (!unicode) {
			    // decode by character encoding.
			    s = new String(s.getBytes("ISO-8859-1"), "JISAutoDetect");
			}
			// decode by RFC2047.
			// if variable s isn't encoded-word, it's ignored.
			return MimeUtility.decodeText(s);
		} catch (UnsupportedEncodingException e) {
		}
		throw new ParseException("Unsupported Encoding");
	}

	public static String html2text(String html){
		Source src = new Source(html);
		return src.getRenderer().toString();
	}
	/*
	 * From,CCなどの情報をBodyの先頭に付加する場合の文字列
	 */
	public static String getHeaderInfo(ImodeMail imm, boolean isHtml, boolean subjectEmojiReplace, Config conf){
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

		for(String s : imm.getOtherInfoList()){
			header.append(" 追加情報:").append(s).append("\r\n");
		}

		if(isHtml){
			String fontfamily = conf.getMailFontFamily();
			if(fontfamily!=null){
				buf.append("<pre style=\"white-space:pre-wrap;word-wrap:break-word;font-family:\'"+fontfamily+"\';\">");
			}else{
				buf.append("<pre style=\"white-space:pre-wrap;word-wrap:break-word;\">");
			}
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

	/* MimeUtility.encodeTextはサロゲートペアの間で分割することがあるので、自前のencoderを用意 */
	public static String encodeGoomojiSubject(String subject) throws UnsupportedEncodingException {
		final int maxlen = 75 - ("=?UTF-8?B?".length() + "?=".length());
		StringBuilder sb = new StringBuilder();

		int mark = 0;
		int utf8len = "X-Goomoji-Subject: ".length();
		for (int i = 0; i < subject.length(); ) {
			int cp = subject.codePointAt(i);
			int len;
			if (cp < 0x7f)
				len = 1;
			else if (cp <= 0x7ff)
				len = 2;
			else if (cp <= 0xffff)
				len = 3;
			else
				len = 4;

			if (4 * ((utf8len + len - 1) / 3 + 1) >= maxlen) {
				if (mark > 0)
					sb.append("\r\n ");
				sb.append(MimeUtility.encodeWord(subject.substring(mark, i), "UTF-8", "B"));
				mark = i;
				utf8len = 0;
			}

			utf8len += len;
			i += Character.charCount(cp);
		}
		if (mark > 0)
			sb.append("\r\n ");
		sb.append(MimeUtility.encodeWord(subject.substring(mark), "UTF-8", "B"));

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static InputStream png2gif(InputStream is) throws IOException{
		InputStream gis = null;
		BufferedInputStream pis = new BufferedInputStream(is);

		try{
			pis.mark(0);
			ImageInputStream iis = ImageIO.createImageInputStream(pis);
			Iterator i = ImageIO.getImageReaders(iis);
			if(i.hasNext()&&((ImageReader)i.next()).getFormatName().equals("gif")){
				// 渡されたデータがgifそのものであればそのまま返却
				pis.reset();
				gis = pis;
			}
		}catch(Exception e){}finally{
			pis.reset();
		}
		if(gis!=null){
			return gis;
		}

		// PNG -> GIF変換
		try{
			BufferedImage png = ImageIO.read(pis);
			ByteArrayOutputStream converted = new ByteArrayOutputStream();
			if(ImageIO.write(png, "gif", converted)){
				gis = new ByteArrayInputStream(converted.toByteArray());
			}
		}catch(Exception e){}finally{
			pis.reset();
		}
		if(gis!=null){
			return gis;
		}else{
			return pis;
		}
	}
	
	/*
	 * メール末尾の空行を削除する
	 */
	public static void stripLastEmptyLines(SenderMail sendMail){
		String plainText = sendMail.getPlainTextContent();
		if(plainText!=null){
			String term = "d51ded3800e80423";
			sendMail.addPlainTextContent(term);
			plainText = HtmlConvert.replaceAllCaseInsenstive(plainText, "(.*)[\r\n]*"+term,"$1");
			sendMail.setPlainTextContent(plainText);
			//log.info("Stripped text: " + sendMail.getPlainTextContent());
		}
		
		String html = sendMail.getHtmlContent();
		if(html!=null){
			html = HtmlConvert.replaceAllCaseInsenstive(html, "(<br>|<div>(<br>)*</div>)*</body>", "</body>");
			sendMail.setHtmlContent(html);
			//log.info("Stripped html: " + sendMail.getHtmlContent());
		}
	}
	
	/*
	 * iPhoneのMobileMail.appで返信時に必ず引用返信されて付加される部分を削除する
	 */
	public static void stripAppleQuotedLines(SenderMail sendMail){
		/*
		 * TEXTパート - 以下の形式を削除
		 * 
		 * | :
		 * |
		 * |On 年/月/日, at 時:分, メールアドレス wrote:
		 * |
		 * |>
		 */
		String plainText = sendMail.getPlainTextContent();
		if(plainText!=null){
			plainText = HtmlConvert.replaceAllCaseInsenstive(plainText, "[\r\n]*On \\d+/\\d+/\\d+, at \\d+:\\d+, [^\n]* wrote:.*","");
			sendMail.setPlainTextContent(plainText);
			//log.info("Stripped text: " + sendMail.getPlainTextContent());
		}
		
		/*
		 * HTMLパート - 以下の形式を削除
		 * 
		 * | :
		 * | <div><br></div>
		 * |</div>
		 * |<div><br>
		 * |On 年/月/日, at 時:分, メールアドレス &lt;<a href="mailto:...></a>&gt; wrote:<br>
		 * |<br><br></div>
		 * |<div></div>
		 * |<blockquote type="cite">
		 */
		String html = sendMail.getHtmlContent();
		if(html!=null){
			// 厳密一致（仮）
			html = HtmlConvert.replaceAllCaseInsenstive(html, "(<div><br></div>)*</div><div><br>On \\d+/\\d+/\\d+, at \\d+:\\d+, [^<>]*<a href=[^<>]*>[^<>]*</a>[^<>]* wrote:(<br>)*(</?div>)+<blockquote type=.*</blockquote>", "</div>");
			// htmlWorkingContent由来
			html = HtmlConvert.replaceAllCaseInsenstive(html, "(<br>)*On \\d+/\\d+/\\d+, at \\d+:\\d+, [^<>]* wrote:(<br>)*&gt;.*</body>", "</body>");
			sendMail.setHtmlContent(html);
			//log.info("Stripped html: " + sendMail.getHtmlContent());
		}
	}
}
