package immf;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

public class MyInternetAddress extends InternetAddress {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1180530557146966532L;

	public MyInternetAddress(String address, String personal, String charset) throws UnsupportedEncodingException{
		super(address,personal,charset);
	}
	  
	// 必ずBエンコーディングにする
	public void setPersonal(String name, String charset)
	throws UnsupportedEncodingException
	{
		personal = name;
		if(name != null)
			encodedPersonal = MimeUtility.encodeWord(name, charset, "B");
		else
			encodedPersonal = null;
	}
}
