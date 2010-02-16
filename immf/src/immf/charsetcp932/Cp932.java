package immf.charsetcp932;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Cp932 extends Charset {
	private Charset charset;
	
	protected Cp932(String canonicalName, String[] aliases) {
		super(canonicalName, aliases);
		// すべてwindows-31jに委譲
		this.charset = Charset.forName("Windows-31J");
	}

	@Override
	public boolean contains(Charset cs) {
		return this.charset.contains(cs);
	}

	@Override
	public CharsetDecoder newDecoder() {
		return this.charset.newDecoder();
	}

	@Override
	public CharsetEncoder newEncoder() {
		return this.charset.newEncoder();
	}

}
