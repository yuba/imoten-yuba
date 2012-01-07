/*
 * imoten - i mode.net mail tensou(forward)
 *
 * Copyright (C) 2011 ryu aka 508.P905 (http://code.google.com/p/imoten/)
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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ForwardMailPicker implements Runnable{
	private static final Log log = LogFactory.getLog(ForwardMailPicker.class);

	private Config conf;
	private ServerMain server;
	private BlockingDeque<ImodeMail> forwardmailQueue = new LinkedBlockingDeque<ImodeMail>();

	public ForwardMailPicker(Config conf, ServerMain server){
		this.conf = conf;
		this.server = server;
		int id = conf.getConfigId();
	
		if(true){
			Thread t = new Thread(this);
			t.setName("ForwardMailPicker["+id+"]");
			t.setDaemon(true);
			t.start();
		}
	}
	
	public void add(ImodeMail mail) throws InterruptedException{
		this.forwardmailQueue.put(mail);
	}

	public void run() {
		boolean errNotify = true;
		int retryLimit = this.conf.getForwardRetryMaxCount();
		int retryCount = 0;
		while(true){
			ImodeMail mail = null;
			try{
				mail = this.forwardmailQueue.take();
			}catch(InterruptedException e){
				continue;
			}
			
			// メール送信
			try{
				ImodeForwardMail forwardMail = new ImodeForwardMail(mail,this.conf);
				forwardMail.send();
				log.info("転送完了");
				if(!errNotify){
					server.notify("iモードメール転送エラーはリトライで解消しました。");
					errNotify = true;
					retryCount = 0;
				}
				
			}catch(Exception e){
				if(retryLimit>0 && ++retryCount>retryLimit){
					log.error("Mail forward error, give up forwarding...",e);
					server.notify("iモードメール転送エラー、リトライで回復せずメール転送中止。");
					errNotify = true;
					retryCount = 0;
					continue;
				}
				
				//送信エラー時はメールをキューに入れなおしてリトライする。
				try{
					this.forwardmailQueue.putFirst(mail);
				}catch(InterruptedException ie){}
				log.warn("Mail forward error, retrying...",e);
				if(errNotify){
					server.notify("iモードメール転送エラー、後でメール転送リトライします。");
					errNotify = false;
				}

				try {
					Thread.sleep(this.conf.getForwardRetryIntervalSec()*1000);
				}catch (Exception te) {}
			}
		}
	}
}
