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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.UsernamePasswordValidator;

public class SendMailBridge implements UsernamePasswordValidator, MyWiserMailListener{
	private static final int MaxRecipient = 5;
	private static final Log log = LogFactory.getLog(SendMailBridge.class);

	private ImodeNetClient client;
	
	private String user;
	private String passwd;
	private String alwaysBcc;
	
	private MyWiser wiser;
	
	public SendMailBridge(Config conf, ImodeNetClient client){
		if(conf.getSenderSmtpPort()<=0){
			return;
		}
		this.client = client;
		this.user = conf.getSenderUser();
		this.passwd = conf.getSenderPasswd();
		this.alwaysBcc = conf.getSenderAlwaysBcc();
				
		this.wiser = new MyWiser(this, conf.getSenderSmtpPort(),this);
		this.wiser.start();
		

	}
	
	/**
	 * SMTPでメールが送られてきた
	 * @param msg
	 * @throws IOException
	 */
	public void receiveMail(MyWiserMessage msg) throws IOException {
		try{
			SenderMail senderMail = new SenderMail();
			
			log.debug("================================================");
			log.debug("Sender   "+msg.getEnvelopeSender());
			log.debug("Receiver "+msg.getEnvelopeReceiver());

			MimeMessage mime = msg.getMimeMessage();
						
			List<InternetAddress> to = getRecipients(mime, "To");
			List<InternetAddress> cc = getRecipients(mime, "Cc");		
			List<InternetAddress> bcc = getBccRecipients(msg.getEnvelopeReceiver(),to,cc);
			
			int maxRecipients = MaxRecipient;
			if(this.alwaysBcc!=null){
				maxRecipients--;
				bcc.add(new InternetAddress(this.alwaysBcc));
			}
			log.debug("To   "+StringUtils.join(to," / "));
			log.debug("cc   "+StringUtils.join(cc," / "));
			log.debug("bcc   "+StringUtils.join(bcc," / "));

			senderMail.setTo(to);
			senderMail.setCc(cc);
			senderMail.setBcc(bcc);
			
			if(maxRecipients< (to.size()+cc.size()+bcc.size())){
				throw new IOException("Too Much Recipients");
			}
			log.debug("subject  "+mime.getSubject());
			senderMail.setSubject(mime.getSubject());
			
			Object content = mime.getContent();
			if (content instanceof String) {
				// テキストメール
				String strContent = (String) content;
				senderMail.setContent(strContent);
			}else{
				// HTMLメール添付ファイルはエラー
				throw new IOException("MimeMultiPart unsupported. Send Plain Text Mail.");
			}
			log.debug("Content  "+mime.getContent());
			log.debug("================================================");
			
			this.client.sendMail(senderMail);
			
		}catch (IOException e) {
			throw e;
		}catch (Exception e) {
			throw new IOException("ReceiveMail Error.",e);
		}
	}
	
	private static List<InternetAddress> getBccRecipients(List<String> allRecipients, List<InternetAddress> to, List<InternetAddress> cc) throws AddressException{
		List<String> addrList = new ArrayList<String>();
		for (String addr : allRecipients) {
			addrList.add(addr);
		}
		for (InternetAddress ia : to) {
			addrList.remove(ia.getAddress());
		}
		for (InternetAddress ia : cc) {
			addrList.remove(ia.getAddress());
		}
		List<InternetAddress> r = new ArrayList<InternetAddress>();
		for (String addr : addrList) {
			r.add(new InternetAddress(addr));
		}
		return r;
	}
	
	private static List<InternetAddress> getRecipients(MimeMessage msg, String type) throws MessagingException{
		List<InternetAddress> r = new ArrayList<InternetAddress>();
		String[] headers = msg.getHeader(type);
		if(headers==null){
			return r;
		}
		for (String h : headers) {
			InternetAddress[] addrs = InternetAddress.parse(h);
			CollectionUtils.addAll(r, addrs);
		}
		return r;
	}

	public void login(String user, String pass) throws LoginFailedException {
		if(!StringUtils.equals(this.user, user)
				||!StringUtils.equals(this.passwd, pass)){
			log.warn("SMTP Auth. Bad User Pass "+user+"/"+pass);
			throw new LoginFailedException("SMTP Auth Login Error.");
		}
	}

}
