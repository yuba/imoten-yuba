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

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.cookie.Cookie;

public class ServerMain {
	public static final String Version = "imoten (imode.net mail tenson) ver. 1.1.1";
	private static final Log log = LogFactory.getLog(ServerMain.class);
	
	private ImodeNetClient client;
	private Config conf;
	private StatusManager status;
	
	public ServerMain(File conffile){
		System.out.println("StartUp ["+Version+"]");
		log.info("StartUp ["+Version+"]");
		this.setShutdownHook();
		this.verCheck();
		try{
			log.info("Load Config file "+conffile.getAbsolutePath());
			FileInputStream is = new FileInputStream(conffile);
			this.conf = new Config(is);

		}catch (Exception e) {
			log.fatal("Config Error. 設定ファイルに問題があります。",e);
			e.printStackTrace();
			System.exit(1);
		}
		
		// cookieと最後に転送したメールID
		File stFile = new File(conf.getStatusFile());
		log.info("Load Status file "+stFile.getAbsolutePath());
		this.status = new StatusManager(stFile);
		try{
			this.status.load();
		}catch (Exception e) {
			// ステータスファイルが無い場合
			log.info("Status File load error. "+e.getMessage());
			log.info("Statusファイルが作成されます。");
		}
		log.info("Loaded LastMailID="+this.status.getLastMailId());
		
		this.client = new ImodeNetClient(this.conf.getDocomoId(),conf.getDocomoPasswd());
		this.client.setConnTimeout(this.conf.getHttpConnectTimeoutSec()*1000);
		this.client.setSoTimeout(this.conf.getHttpSoTimeoutSec()*1000);
		
		try{
			// 前回のcookie
			if(this.conf.isSaveCookie()){
				log.info("Load cookie");
				for (Cookie	cookie : this.status.getCookies()) {
					this.client.addCookie(cookie);
				}
			}
		}catch (Exception e) {}
		
		// メール送信
		new SendMailBridge(conf, this.client);
		boolean first = true;
		while(true){
			List<String> mailIdList = null;
			try{
				// メールID一覧取得(降順)
				mailIdList = this.client.getMailIdList(0);

			}catch (LoginException e) {
				log.error("ログインエラー",e);
				if(first){
					// 起動直後はcookieが切れている可能性があるのでクッキーを破棄してリトライ
					log.info("起動直後はすぐにログイン処理を行います。");
					continue;
				}
				try{
					// 別の場所でログインされた
					log.info("Logout Wait "+this.conf.getLoginRetryIntervalSec()+" sec.");
					Thread.sleep(this.conf.getLoginRetryIntervalSec()*1000);
				}catch (Exception ex) {}
				continue;
			}catch (Exception e) {
				log.error("Get Mail ID List error.", e);
				try{
					Thread.sleep(this.conf.getLoginRetryIntervalSec()*1000);
				}catch (Exception ex) {}
				continue;
			}finally{
				first = false;
			}
			
			String lastId = this.status.getLastMailId();
			log.info("受信したメールIDの数:"+mailIdList.size()+"  lastId:"+lastId);

			if(StringUtils.isBlank(lastId)){
				if(!mailIdList.isEmpty()){
					// 最初の起動では現在の最新メールの次から転送処理する
					this.status.setLastMailId(mailIdList.get(0));
					log.info("LastMailIdが空なので、次のメールから転送を開始します。");
				}else{
					// メールがひとつも無かった
				}
			}else{
				List<String> forwardIdList = new LinkedList<String>();
				for (String id : mailIdList) {
					if(lastId.compareToIgnoreCase(id)<0){
						// 昇順に入れていく
						forwardIdList.add(0, id);
					}
				}
				log.info("転送するメールIDの数 "+forwardIdList.size());
				for (String id : forwardIdList) {
					this.forward(id);
					this.status.setLastMailId(id);
				}
			}
			if(lastId!=null && !lastId.equals(this.status.getLastMailId())){
				log.info("LastMailId("+this.status.getLastMailId()+")に更新しました");
			}
			try{
				if(this.conf.isSaveCookie()){
					this.status.setCookies(client.getCookies());
				}
				this.status.save();
				log.info("statusファイルを保存しました");
			}catch (Exception e) {
				log.error("Status File save Error.",e);
			}
			try{
				Thread.sleep(conf.getCheckIntervalSec()*1000);
			}catch (Exception e) {}
		}
		
	}
	
	private void verCheck(){
		String verndor = System.getProperty("java.vendor");
		String version = System.getProperty("java.version");
		log.info("Java vendor  "+verndor);
		log.info("Java version "+version);
		try{
			String[] v = version.split("\\.");
			if(Integer.parseInt(v[0])>=2 || Integer.parseInt(v[1])>=6){
				return;
			}else{
				log.warn("注意 動作にはJava ver 1.6 以上が必要となります。");
			}
		}catch (Exception e) {
			log.warn(e.getMessage());
		}
	}
	
	/*
	 * メールをダウンロードして送信
	 */
	private void forward(String mailId){
		ImodeMail mail = null;
		try{
			// download
			mail = this.client.getMail(0, mailId);
			if(log.isInfoEnabled()){
				log.info("Downloaded Mail ########");
				log.info(mail.toLoggingString());
				log.info("########################");
			}
		}catch (Exception e) {
			log.warn("i mode.net mailId["+mailId+"] download Error.",e);
			return;
		}
		try{
			// 送信
			ImodeForwardMail forwardMail = new ImodeForwardMail(mail,this.conf);
			forwardMail.send();
						
		}catch (Exception e) {
			log.error("mail["+mailId+"] forward Error.",e);
			return;
		}
		try{
			// 負荷をかけないように
			Thread.sleep(1000);
		}catch (Exception e) {}
	}
	
	/*
	 * 停止時にログを出力
	 */
	private void setShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
        		System.out.println("Shutdown ["+Version+"]");
        		log.info("Shutdown ["+Version+"]");
            }
        });;

	}
	
	public static void main(String[] args){
		try{
			String confFile = Config.ConfFile;
			if(args.length>0){
				confFile = args[0];
			}
			new ServerMain(new File(confFile));
		}catch (Exception e) {
			e.printStackTrace();
			log.fatal("Startup Error.",e);
		}
	}
}

