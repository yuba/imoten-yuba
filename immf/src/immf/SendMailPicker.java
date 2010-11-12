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
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

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
		SimpleEmail mail = new SimpleEmail();

		//mail.setCharset(this.conf.getMailEncode());
		mail.setCharset("UTF-8");

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
			mail.setFrom(conf.getSmtpMailAddress());
		
			// 転送先のアドレス全部にエラーメールを送る
			List<String> list = conf.getForwardTo();
			for (String addr : list) {
				mail.addTo(addr);
			}
			list = conf.getForwardCc();
			for (String addr : list) {
				mail.addCc(addr);
			}
			list = conf.getForwardBcc();
			for (String addr : list) {
				mail.addBcc(addr);
			}

			mail.setSubject("メール送信エラー");

			String body = "imotenが以下のメール送信に失敗しました\r\n\r\n";
			body += "【エラーメッセージ】\r\n";
			body += error;
			body += "\r\n\r\n";

			body += "【件名】\r\n";
			String errSubject="";
			if(errMail!=null){
				errSubject = errMail.getSubject();
			}
			body += errSubject;
			body += "\r\n";

			body += "【本文】\r\n";
			String errBody = "";
			if(errMail!=null){
				errBody = errMail.getPlainBody();
			}
			if(errBody.length() > 30){
				body += errBody.substring(0,30);
				body += "...";
			}else{
				body += errBody;
			}
			mail.setMsg(body);
			
			mail.send();
			log.info("エラーメール返信");

			server.notify("メール送信失敗\n"+error);

		}catch(EmailException e){
			log.warn("エラーメール返信失敗");
		}
	}
}
