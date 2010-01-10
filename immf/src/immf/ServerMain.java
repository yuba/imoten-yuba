package immf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.cookie.Cookie;

public class ServerMain {
	public static final String Version = "imode.net mail forwarder ver. 1.0.0";
	private static final Log log = LogFactory.getLog(ServerMain.class);
	
	private ImodeNetClient client;
	private Config conf;
	private StatusManager status;
	
	public ServerMain(File conffile){
		System.out.println("StartUp ["+Version+"]");
		log.info("StartUp ["+Version+"]");
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
			log.info("Statis File load error. "+e.getMessage());
		}
		log.info("Loaded LastMailID="+this.status.getLastMailId());
		
		this.client = new ImodeNetClient(this.conf.getDocomoId(),conf.getDocomoPasswd());
		this.client.setConnTimeout(this.conf.getHttpConnectTimeoutSec()*1000);
		this.client.setSoTimeout(this.conf.getHttpSoTimeoutSec()*1000);
		
		try{
			// 前回のcookie
			for (Cookie	cookie : this.status.getCookies()) {
				this.client.addCookie(cookie);
			}
		}catch (Exception e) {}
		
		while(true){
			List<String> mailIdList = null;
			try{
				// メールID一覧取得(降順)
				mailIdList = this.client.getMailIdList(0);
			}catch (LoginException e) {
				log.error("Login Error.",e);
				try{
					Thread.sleep(this.conf.getLoginRetryIntervalSec()*1000);
				}catch (Exception ex) {}
				continue;
			}catch (Exception e) {
				log.error("Get Mail ID List error.", e);
				try{
					Thread.sleep(this.conf.getLoginRetryIntervalSec()*1000);
				}catch (Exception ex) {}
				continue;
			}
			
			String lastId = this.status.getLastMailId();
			if(StringUtils.isBlank(lastId)){
				if(!mailIdList.isEmpty()){
					// 最初の起動では現在の最新メールの次から転送処理する
					this.status.setLastMailId(mailIdList.get(0));
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
				log.info("Forward ID size "+forwardIdList.size());
				for (String id : forwardIdList) {
					this.forward(id);
					this.status.setLastMailId(id);
				}
			}
			if(lastId!=null && !lastId.equals(this.status.getLastMailId())){
				log.info("Update LastMailId="+this.status.getLastMailId());
			}
			try{
				this.status.setCookies(client.getCookies());
				this.status.save();
			}catch (Exception e) {
				log.error("Status File save Error.",e);
			}
			try{
				Thread.sleep(conf.getCheckIntervalSec()*1000);
			}catch (Exception e) {}
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
	
	public static void main(String[] args){
		try{
			String confFile = Config.ConfFile;
			if(args.length>0){
				confFile = args[0];
			}

			new ServerMain(new File(confFile));
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}

