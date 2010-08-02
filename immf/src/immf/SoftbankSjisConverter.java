package immf;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class SoftbankSjisConverter {
	private static Charset sjis = Charset.forName("Windows-31J");
	private static CharsetDecoder decoder = sjis.newDecoder();		
	
	private static boolean isFirstByte(byte b) {
		int i = (int)b & 0xff;
		return (0x81 <= i && i <= 0x9f) || (0xe0 <= i && i <= 0xfc);
	}
	
	private static boolean isSecondByte(byte b) {
		int i = (int)b & 0xff;
		return i != 0x7f && 0x40 <= i && i <= 0xfc;
	}
	
	private static boolean isEmoji(byte firstByte, byte secondByte) {
		int first = (int)firstByte & 0xff;
		//int second = (int)secondByte & 0xff;
		
		return first == 0xf7 || first == 0xf9 || first == 0xfb;
	}	
	
	private static char convertEmoji(byte firstByte, byte secondByte) {
		int first = (int)firstByte & 0xff;
		int second = (int)secondByte & 0xff;
		char c = 0;
		
		if (first == 0xf7) {
			if (second < 0xa0)
				c = 0xe100;
			else
				c = 0xe200;
		} else if (first == 0xf9) {
			if (second < 0xa0)
				c = 0xe000;
			else
				c = 0xe300;
		} else if (first == 0xfb) {
			if (second < 0xa0)
				c = 0xe400;
			else
				c = 0xe500;
		}
		
		if (second < 0x80) 
			c += second - 0x40;
		else if (second < 0xa0)
			c += second - 0x41;
		else
			c += second - 0xa0;

		return c;
	}	
	
	public static String convert(String str) {
		int state = 0;
		byte first = 0;
		byte bytes[] = null;
		try {
			bytes = str.getBytes("Windows-31J");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		ByteBuffer in = ByteBuffer.wrap(bytes);
		CharBuffer out = CharBuffer.allocate(str.length());
		
		ByteBuffer dup = in.duplicate();
		
		while (dup.remaining() > 0) {
			byte b = dup.get();
			
			if (state == 0 && isFirstByte(b)) {
				state = 1;
				first = b;
			} else if (state == 1 && isSecondByte(b)) {
				state = 2;
				
				if (isEmoji(first, b)) {
					if (in.position() < dup.position() - 2) {
						in.limit(dup.position() - 2);
						CoderResult result = decoder.decode(in, out, true);
						in.limit(dup.limit());
						
						if (!result.isUnderflow())
							throw new RuntimeException("Failed to decode Softbank Shift JIS " + result);
					}
					
					if (out.remaining() == 0)
						throw new RuntimeException("Failed to decode Softbank Shift JIS");
					
					in.position(in.position() + 2);
					out.put(convertEmoji(first, b));
				}
				
				first = 0;
			} else if (state == 2 && isFirstByte(b)) {
				state = 1;
				first = b;
			} else {
				state = 0;
			}
		}		
		
		if (state == 1)
			throw new RuntimeException("Failed to decode Softbank Shift JIS");
		
		CoderResult result = decoder.decode(in, out, true);

		if (!result.isUnderflow() && !result.isOverflow())
			throw new RuntimeException("Failed to decode Softbank Shift JIS " + result);

		out.flip();
		String resultStr = out.toString();
		
        return resultStr;
	}
}
