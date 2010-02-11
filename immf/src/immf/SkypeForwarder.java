package immf;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.skype.Skype;

public class SkypeForwarder implements Runnable{
	private static final Log log = LogFactory.getLog(SkypeForwarder.class);
	private static final int MaxQueueLength = 10;
	private String chat;
	private String sms;
	
	private List<ImodeMail> queue = new LinkedList<ImodeMail>();
	
	public SkypeForwarder(String chat,String sms){
		this.chat = chat;
		this.sms = sms;
		
		if(this.chat!=null || this.sms!=null){
			Thread t = new Thread(this);
			t.setName("SkypeForwarder");
			t.setDaemon(true);
			t.start();
		}
	}
	
	public void forward(ImodeMail mail){
		if(this.chat==null && this.sms==null){
			return;
		}
		synchronized (this.queue) {
			if(this.queue.size()>MaxQueueLength){
				// キューにたまっているのはSkypeAPIがフリーズしている状態
				log.error("Skype Queue size "+this.queue);
				log.error("おそらく Skype API がフリーズしています。メールはskypeに転送されません。");
				return;
			}
			this.queue.add(mail);
			this.queue.notifyAll();
		}
	}

	public void run() {
		log.info("Skype Forwarder Start");
		try{
			log.info("Skype Version "+Skype.getVersion());
		}catch (Exception e) {
			log.error("Skype getVersion Error.",e);
		}
		while(true){
			ImodeMail mail = null;
			synchronized (this.queue) {
				while(this.queue.isEmpty()){
					try{
						this.queue.wait(1000*60);
					}catch (Exception e) {}
				}
				mail = this.queue.remove(0);
			}
			if(this.chat!=null){
				this.sendToChat(mail);
			}
			if(this.sms!=null){
				this.sendToSms(mail);
			}
		}
	}
	
	private void sendToChat(ImodeMail mail){
		String text=mail.getBody();
		if(mail.isDecomeFlg()){
			text = Util.html2text(text);
		}
		text = Util.getHeaderInfo(mail, false, true) + text;
		log.info("Skype send Chat to ["+this.chat+"]");
		log.info(text);
		try{
			Skype.chat(this.chat).send(text);
		}catch (Exception e) {
			log.error("Skype Chat Error.",e);
		}
		log.info("Skype send Chat done");
	}
	
	private void sendToSms(ImodeMail mail){
		String text=mail.getBody();
		if(mail.isDecomeFlg()){
			text = Util.html2text(text);
		}
		text = smsHeader(mail) + text;
		
		text = text.substring(0,69);//日本語が含まれる場合、70字まで。それを超えると複数SMSが送信される
		
		log.info("Skype send SMS to ["+this.sms+"]");
		log.info(text);
		try{
			Skype.sendSMS(this.sms, text);
		}catch (Exception e) {
			log.error("Skype SMS Error.",e);
		}
		log.info("Skype send SMS done");
	}
	
	private static String smsHeader(ImodeMail mail){
		StringBuilder buf = new StringBuilder();
		SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm");
		buf.append("差:").append(mail.getFromAddr()).append("\r\n");
		buf.append("題:").append(EmojiUtil.replaceToLabel(mail.getSubject())).append("\r\n");
		buf.append("日:").append(df.format(mail.getTimeDate())).append("\r\n");
		buf.append("\r\n");
		return buf.toString();
	}

}
