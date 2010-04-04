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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

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
	private boolean forcePlainText;
	
	private MyWiser wiser;
	private CharacterConverter charConv;
	
	public SendMailBridge(Config conf, ImodeNetClient client){
		if(conf.getSenderSmtpPort()<=0){
			return;
		}
		this.client = client;
		this.user = conf.getSenderUser();
		this.passwd = conf.getSenderPasswd();
		this.alwaysBcc = conf.getSenderAlwaysBcc();
		this.forcePlainText = conf.isSenderMailForcePlainText();
		this.charConv = new CharacterConverter();
		if(conf.getSenderCharCovertFile()!=null){
			try{
				this.charConv.load(new File(conf.getSenderCharCovertFile()));
			}catch (Exception e) {
				log.error("文字変換表("+conf.getSenderCharCovertFile()+")が読み込めませんでした。",e);
			}
		}
		
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
			String subject = mime.getSubject();
			log.info("subject  "+subject);
			subject = this.charConv.convert(subject);
			log.debug(" conv "+subject);
			senderMail.setSubject(subject);
			
			String contentType = mime.getContentType().toLowerCase();
			log.info("ContentType:"+contentType);
			
			Object content = mime.getContent();
			if (content instanceof String) {
				// テキストメール
				String strContent = (String) content;
				if(contentType.toLowerCase().startsWith("text/html")){
					log.info("Single plainText part "+strContent);
					strContent = this.charConv.convert(strContent);
					log.debug(" conv "+strContent);
					senderMail.setHtmlContent(strContent);
				}else{
					log.info("Single html part "+strContent);
					strContent = this.charConv.convert(strContent);
					log.debug(" conv "+strContent);
					senderMail.setPlainTextContent(strContent);
				}
			}else if(content instanceof Multipart){
				Multipart mp = (Multipart)content;
				parseMultipart(senderMail, mp, getSubtype(contentType));
				
			}else{
				log.warn("未知のコンテンツ "+content.getClass().getName());
				throw new IOException("Unsupported type "+content.getClass().getName()+".");
			}
			
			log.info("Content  "+mime.getContent());
			log.info("====");
			
			this.client.sendMail(senderMail, this.forcePlainText);
			
		}catch (IOException e) {
			log.warn("Bad Mail Received.",e);
			throw e;
		}catch (Exception e) {
			log.error("ReceiveMail Error.",e);
			throw new IOException("ReceiveMail Error."+e.getMessage(),e);
		}
	}
	
	/*
	 * マルチパートを処理
	 */
	private void parseMultipart(SenderMail sendMail, Multipart mp, String subtype) throws IOException{
		String contentType = mp.getContentType();
		log.info("Multipart ContentType:"+contentType);

		try{
			int count = mp.getCount();
			log.info("count "+count);

			for(int i=0; i<count; i++){
				parseBodypart(sendMail, mp.getBodyPart(i), subtype);
			}
		}catch (Exception e) {
			log.error("parse multipart error.",e);
			//throw new IOException("MimeMultiPart error."+e.getMessage(),e);
		}
	}
	
	private static String getSubtype(String contenttype){
		try{
			String r = contenttype.split("\\r?\\n")[0];
			return r.split("/")[1].replaceAll("\\s*;.*", "");
		}catch (Exception e) {
		}
		return "";
	}
	
	/*
	 * 各パートの処理
	 */
	private void parseBodypart(SenderMail sendMail, BodyPart bp, String subtype) throws IOException{
		try{
			String contentType = bp.getContentType().toLowerCase();
			log.info("Bodypart ContentType:"+contentType);
			log.info("subtype:"+subtype);
			
			if(contentType.startsWith("multipart/")){
				parseMultipart(sendMail, (Multipart)bp.getContent(), getSubtype(contentType));
				
			}else if(sendMail.getPlainTextContent()==null
					&& contentType.startsWith("text/plain")){
				// 最初に存在するplain/textは本文
				String content = (String)bp.getContent();
				log.info("set Content text ["+content+"]");
				content = this.charConv.convert(content);
				log.debug(" conv "+content);
				sendMail.setPlainTextContent(content);
				
			}else if(sendMail.getHtmlContent()==null 
					&& contentType.startsWith("text/html") 
					&& (subtype.equalsIgnoreCase("alternative")
							|| subtype.equalsIgnoreCase("related"))){
				String content = (String)bp.getContent();
				log.info("set Content html ["+content+"]");
				content = this.charConv.convert(content);
				log.debug(" conv "+content);
				// 本文 htmlはテキスト形式に変換
				sendMail.setHtmlContent(content);

			}else{
				log.debug("attach");
				// 本文ではない 

				if(subtype.equalsIgnoreCase("related")){
					// インライン添付
					SenderAttachment file = new SenderAttachment();
					file.setInline(true);
					file.setContentType(contentType);
					//file.setFilename(bp.getFileName());
					file.setFilename(uniqId()+"."+getSubtype(contentType));
					file.setData(inputstream2bytes(bp.getInputStream()));
					file.setContentId(bp.getHeader("Content-Id")[0]);
					sendMail.addAttachmentFileIdList(file);
					log.info("Inline Attachment "+file.loggingString());
					
				}else{
					// 通常の添付ファイル
					SenderAttachment file = new SenderAttachment();
					file.setInline(false);
					file.setContentType(contentType);
					String fname = MimeUtility.decodeText(bp.getFileName());
					file.setFilename(fname);
					file.setData(inputstream2bytes(bp.getInputStream()));
					sendMail.addAttachmentFileIdList(file);
					log.info("Attachment "+file.loggingString());
				}
			}
		}catch (Exception e) {
			log.error("parse bodypart error.",e);
			throw new IOException("BodyPart error."+e.getMessage(),e);
		}
	}
	private static int fileNameId=0;
	private static String uniqId(){
		fileNameId++;
		return System.currentTimeMillis()+"_"+fileNameId;
	}
	private static byte[] inputstream2bytes(InputStream is) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			byte[] buf = new byte[1024 * 4];
			while(is.available()>0){
				int len = is.read(buf);
				if(len<=0){
					break;
				}
				bos.write(buf,0,len);
			}
		}finally{
			try{
				is.close();
			}catch (Exception e) {}
			try{
				bos.close();
			}catch (Exception e) {}
		}
		return bos.toByteArray();
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
		log.debug("* SMTP Login *");
		if(!StringUtils.equals(this.user, user)
				||!StringUtils.equals(this.passwd, pass)){
			log.warn("SMTP 認証エラー User "+user+"/ Pass "+pass);
			throw new LoginFailedException("SMTP Auth Login Error.");
		}
	}

}
