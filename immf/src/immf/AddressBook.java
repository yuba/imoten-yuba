package immf;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AddressBook {
	private static final Log log = LogFactory.getLog(AddressBook.class);
	
	// メールアドレスからimodeAddressを検索
	private Map<String, ImodeAddress> pcAddrMap;	// iモード.net 上で登録したアドレス帳
	private Map<String, ImodeAddress> dsAddrMap;	// ケータイデータお預かりサービスの携帯電話帳
	
	private Date created;
	
	public AddressBook(){
		this.created = new Date();
		this.pcAddrMap = new HashMap<String, ImodeAddress>();
		this.dsAddrMap = new HashMap<String, ImodeAddress>();
	}
	
	/*
	 * メールアドレスから名前の入ったImodeAddressを取得
	 * iモード.net 上で登録したアドレス帳が、
	 * ケータイデータお預かりサービスの携帯電話帳より優先される
	 */
	public ImodeAddress getImodeAddress(String mailAddress){
		ImodeAddress r = this.pcAddrMap.get(mailAddress);
		if(r!=null){
			return r;
		}else{
			return this.dsAddrMap.get(mailAddress);
		}
	}
	
	public InternetAddress getInternetAddress(String mailAddress, String charset){
		ImodeAddress ia = this.getImodeAddress(mailAddress);
		try{
			if(ia==null){
				return new InternetAddress(mailAddress);
			}else{
				return new MyInternetAddress(mailAddress,ia.getName(), charset);
			}
		}catch (Exception e) {
			log.warn("mail addrress format error.["+mailAddress+"]",e);
			try{
				return new InternetAddress(mailAddress);
			}catch (Exception ex) {
				return null;
			}
		}
	}
	
	public void addPcAddr(ImodeAddress ia){
		this.pcAddrMap.put(ia.getMailAddress(), ia);
	}
	
	public void addDsAddr(ImodeAddress ia){
		this.dsAddrMap.put(ia.getMailAddress(), ia);
	}
	public Date getCreated(){
		return created;
	}
}
