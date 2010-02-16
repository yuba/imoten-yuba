package immf.charsetcp932;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Cp932Provider extends CharsetProvider {
	private Map<String, Charset> charsets = new HashMap<String, Charset>();
	public Cp932Provider(){
		this.charsets.put("CP932", new Cp932("CP932",new String[0]));
	}
	
	@Override
	public Charset charsetForName(String name) {
		return this.charsets.get(name.toUpperCase());
	}

	@Override
	public Iterator<Charset> charsets() {
		return this.charsets.values().iterator();
	}

}
