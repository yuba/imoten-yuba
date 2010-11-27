/*
 * imoten - i mode.net mail tensou(forward)
 *
 * Copyright (C) 2010 ryu aka 508.P905 (http://code.google.com/p/imoten/)
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SendMailPicker implements Runnable{
	private static final Log log = LogFactory.getLog(SendMailPicker.class);

	private Config conf;
	private ServerMain server;
	private ImodeNetClient client;
	private StatusManager status;
	private BlockingDeque<SenderMail> sendmailQueue = new LinkedBlockingDeque<SenderMail>();

	private boolean forcePlainText;
	private boolean isForwardSent;

	public SendMailPicker(Config conf, ServerMain server, ImodeNetClient client, StatusManager status){
		this.conf = conf;
		this.server = server;
		this.client = client;
		this.status = status;
		this.forcePlainText = conf.isSenderMailForcePlainText();
		this.isForwardSent = conf.isForwardSent();
	
		if(true){
			Thread t = new Thread(this);
			t.setName("SendMailPicker");
			t.setDaemon(true);
			t.start();
		}
	}
	
	public void add(SenderMail mail) throws InterruptedException{
		this.sendmailQueue.put(mail);
	}

	public void run() {
		while(true){
			SenderMail mail = null;
			try{
				mail = this.sendmailQueue.take();
			}catch(InterruptedException e){
				continue;
			}
			
			// メール送信
			log.info("Picked!");
			try{
				this.client.sendMail(mail, this.forcePlainText);
				if(isForwardSent){
					status.setNeedConnect();
				}
				
			}catch(LoginException e){
				//未ログイン時はメールをキューに入れなおしてリトライする。
				try{
					this.sendmailQueue.putFirst(mail);
				}catch(InterruptedException ie){}
				log.warn("nologin error, retrying...");

				// XXX すぐリトライするべきか待つべきか
				try {
					//Thread.sleep(this.conf.getLoginRetryIntervalSec()*1000);
					Thread.sleep(5000);
				}catch (Exception te) {}
				
			}catch (IOException e) {
				log.warn("Bad Mail Received.",e);
				errorNotify("Bad Mail Received.\n" + e.getMessage(), mail);
			}
		}
	}
	
	/*
	 * 送信失敗したメールをお知らせ
	 */
	private void errorNotify(String error, SenderMail errMail){
		ForwardEmail mail = new ForwardEmail(conf);

		mail.setCharset(this.conf.getMailEncode());

		// SMTP Server
		mail.setHostName(conf.getSmtpServer());
		mail.setSmtpPort(conf.getSmtpPort());
		mail.setSocketConnectionTimeout(conf.getSmtpConnectTimeoutSec()*1000);
		mail.setSocketTimeout(conf.getSmtpTimeoutSec()*1000);
		mail.setTLS(conf.isSmtpTls());

		if(!StringUtils.isBlank(conf.getSmtpUser())){
			mail.setAuthentication(conf.getSmtpUser(), conf.getSmtpPasswd());
		}

		if(!StringUtils.isBlank(conf.getPopServer())
				&& !StringUtils.isBlank(conf.getPopUser())){
			// POP before SMTP
			mail.setPopBeforeSmtp(true, conf.getPopServer(), conf.getPopUser(), conf.getPopPasswd());
		}

		try{
			mail.setFrom(this.conf.getSmtpMailAddress(),"imoten");
			mail.addTo(this.conf.getSmtpMailAddress());
			
			if(!this.conf.getForwardTo().isEmpty()){
				mail.addHeader("Resent-To", StringUtils.join(this.conf.getForwardTo(), ","));
			}
			if(!this.conf.getForwardCc().isEmpty()){
				mail.addHeader("Resent-Cc", StringUtils.join(this.conf.getForwardCc(), ","));
			}
			if(!this.conf.getForwardBcc().isEmpty()){
				mail.addHeader("Resent-Bcc", StringUtils.join(this.conf.getForwardBcc(), ","));
			}

			mail.setSubject("メール送信エラー");

			String body = "imotenがメール送信に失敗しました\r\n\r\n";
			body += "【エラーメッセージ】\r\n";
			body += error;
			body += "\r\n\r\n";

			body += "【エラーメール】\r\n";

			if(errMail==null){
				body += "null";

			}else{
				// To
				List<InternetAddress> list = errMail.getTo();
				List<String> addrList = new ArrayList<String>();
				for(InternetAddress addr : list) {
					addrList.add(addr.getAddress());
				}
				String errTo = StringUtils.join(addrList, ",");
				if(!errTo.isEmpty()){
					body += "To: " + errTo + "\r\n";
				}

				// Cc
				list = errMail.getCc();
				addrList = new ArrayList<String>();
				for(InternetAddress addr : list) {
					addrList.add(addr.getAddress());
				}
				String errCc = StringUtils.join(addrList, ",");
				if(!errCc.isEmpty()){
					body += "Cc: " + errCc + "\r\n";
				}

				// Bcc
				list = errMail.getBcc();
				addrList = new ArrayList<String>();
				for(InternetAddress addr : list) {
					addrList.add(addr.getAddress());
				}
				String errBcc = StringUtils.join(addrList, ",");
				if(!errBcc.isEmpty()){
					body += "Bcc: " + errBcc + "\r\n";
				}

				// Subject
				String errSubject="";
				errSubject = errMail.getSubject();
				body += "Subject: " + errSubject + "\r\n";
				body += "\r\n";

				// Body
				String errBody = "";
				errBody = errMail.getPlainBody();
				if(errBody.length() > 100){
					body += errBody.substring(0,100);
					body += "...";
				}else{
					body += errBody;
				}
			}

			mail.setMsg(body);
			
			mail.send();
			log.info("エラーメール返信");

			server.notify("メール送信失敗\n"+error);

		}catch(EmailException e){
			log.warn("エラーメール返信失敗",e);
		}
	}

	// 転送先のアドレス全部にメールを送る
	private class ForwardEmail extends SimpleEmail{
		private Config conf;
		public ForwardEmail(Config conf){
			this.conf = conf;
		}
		
		@Override
		public void buildMimeMessage() throws EmailException {
			super.buildMimeMessage();
			MimeMessage msg = this.getMimeMessage();
			try{
				msg.removeHeader("To");
			}catch (Exception e) {
				log.warn(e);
			}
		}
		
		@Override
		protected MimeMessage createMimeMessage(Session aSession) {
			List<InternetAddress> recipients = new ArrayList<InternetAddress>();
			List<String> list = conf.getForwardTo();
			for (String addr : list) {
				try{
					recipients.add(new InternetAddress(addr));
				}catch (Exception e) {
					log.warn("ForwardTo error "+addr,e);
				}
			}
			list = conf.getForwardCc();
			for (String addr : list) {
				try{
					recipients.add(new InternetAddress(addr));
				}catch (Exception e) {
					log.warn("ForwardCc error "+addr,e);
				}
			}
			list = conf.getForwardBcc();
			for (String addr : list) {
				try{
					recipients.add(new InternetAddress(addr));
				}catch (Exception e) {
					log.warn("ForwardBcc error "+addr,e);
				}
			}
			String from = this.conf.getSmtpMailAddress();
			try{
				return new MyMimeMessage(aSession, new InternetAddress(from), recipients);
			}catch (Exception e) {
				log.warn("From error "+from,e);
				return null;
			}
		}
	}
}
