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
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

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
		
		log.info("SMTPサーバを起動します。");
		this.wiser = new MyWiser(this, conf.getSenderSmtpPort(),this,
				conf.getSenderTlsKeystore(),
				conf.getSenderTlsKeyType(),
				conf.getSenderTlsKeyPasswd());
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

			log.info("==== SMTPサーバがメールを受信しました。====");
			log.info("From       "+msg.getEnvelopeSender());
			log.info("Recipients  "+msg.getEnvelopeReceiver());

			MimeMessage mime = msg.getMimeMessage();
						
			List<InternetAddress> to = getRecipients(mime, "To");
			List<InternetAddress> cc = getRecipients(mime, "Cc");		
			List<InternetAddress> bcc = getBccRecipients(msg.getEnvelopeReceiver(),to,cc);
			
			int maxRecipients = MaxRecipient;
			if(this.alwaysBcc!=null){
				log.debug("add alwaysbcc "+this.alwaysBcc);
				maxRecipients--;
				bcc.add(new InternetAddress(this.alwaysBcc));
			}
			log.info("To   "+StringUtils.join(to," / "));
			log.info("cc   "+StringUtils.join(cc," / "));
			log.info("bcc   "+StringUtils.join(bcc," / "));

			senderMail.setTo(to);
			senderMail.setCc(cc);
			senderMail.setBcc(bcc);
			
			if(maxRecipients< (to.size()+cc.size()+bcc.size())){
				log.warn("送信先が多すぎます。iモード.netの送信先は最大5です。");
				throw new IOException("Too Much Recipients");
			}
			log.info("subject  "+mime.getSubject());
			senderMail.setSubject(mime.getSubject());
			
			String contentType = mime.getContentType();
			log.info("ContentType:"+contentType);
			
			Object content = mime.getContent();
			if (content instanceof String) {
				// テキストメール
				String strContent = (String) content;
				if(contentType.toLowerCase().startsWith("text/html")){
					// htmlはテキスト形式にフォーマット変換
					Source src = new Source(strContent);
					strContent = src.getRenderer().toString();
					log.info("convert to text");
					log.info(strContent);
				}
				senderMail.setContent(strContent);
				
			}else if(content instanceof Multipart){
				Multipart mp = (Multipart)content;
				parseMultipart(senderMail, mp);
				
			}else{
				// HTMLメール添付ファイルはエラー
				log.warn("未知のコンテンツ "+content.getClass().getName());
				throw new IOException("Unsupported type "+content.getClass().getName()+".");
			}
			log.info("Content  "+mime.getContent());
			log.info("====");
			
			this.client.sendMail(senderMail);
			
		}catch (IOException e) {
			log.warn("Bad Mail Received.",e);
			throw e;
		}catch (Exception e) {
			log.error("ReceiveMail Error.",e);
			throw new IOException("ReceiveMail Error."+e.getMessage(),e);
		}
	}
	
	private static void parseMultipart(SenderMail sendMail, Multipart mp) throws IOException{
		String contentType = mp.getContentType();
		log.info("Multipart ContentType:"+contentType);

		try{
			int count = mp.getCount();
			log.info("count "+count);

			for(int i=0; i<count; i++){
				parseBodypart(sendMail, mp.getBodyPart(i));
			}
		}catch (Exception e) {
			log.error("parse multipart error.",e);
			throw new IOException("MimeMultiPart error."+e.getMessage(),e);
		}
	}
	
	private static void parseBodypart(SenderMail sendMail, BodyPart bp) throws IOException{
		try{
			String contentType = bp.getContentType().toLowerCase();
			log.info("Bodypart ContentType:"+contentType);
			
			if(contentType.startsWith("multipart/")){
				parseMultipart(sendMail, (Multipart)bp.getContent());
				
			}else if(sendMail.getContent()==null && contentType.startsWith("text/plain")){
				log.info("set Content text ["+(String)bp.getContent()+"]");
				sendMail.setContent((String)bp.getContent());
				
			}else if(sendMail.getContent()==null && contentType.startsWith("text/html")){
				log.info("set Content html ["+(String)bp.getContent()+"]");
				// htmlはテキスト形式に変換
				Source src = new Source((String)bp.getContent());
				String content = src.getRenderer().toString();
				log.info("convert to text");
				log.info(content);
				sendMail.setContent(content);
			}
		}catch (Exception e) {
			log.error("parse bodypart error.",e);
			throw new IOException("BodyPart error."+e.getMessage(),e);
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
			log.warn("SMTP 認証エラー User "+user+"/ Pass "+pass);
			throw new LoginFailedException("SMTP Auth Login Error.");
		}
	}

}
