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

import immf.google.contact.GoogleContactsAccessor;
import immf.growl.GrowlNotifier;
import immf.growl.concrete.NMAClient;
import immf.growl.concrete.ProwlClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.cookie.Cookie;

public class ServerMain {
	public static final String Version = "imoten (imode.net mail tenson) ver. 1.1.36";
	private static final Log log = LogFactory.getLog(ServerMain.class);

	private ImodeNetClient client;
	private SendMailPicker spicker;
	private Config conf;
	private StatusManager status;
	private SkypeForwarder skypeForwarder;
	private ImKayacNotifier imKayacNotifier;
	private AppNotifications appNotifications;
	private GrowlNotifier prowlNotifier;
	private GrowlNotifier nmaNotifier;
	private int numForwardSite;
	private Map<Config, ForwardMailPicker> forwarders = new HashMap<Config, ForwardMailPicker>();
	private Map<Config, List<String>> ignoreDomainsMap = new HashMap<Config, List<String>>();

	public ServerMain(File conffile){
		System.out.println("StartUp ["+Version+"]");
		log.info("StartUp ["+Version+"]");
		this.setShutdownHook();
		this.verCheck();
		try{
			log.info("Load Config file "+conffile.getAbsolutePath());
			FileInputStream is = new FileInputStream(conffile);
			this.conf = new Config(is);
			this.numForwardSite = conf.countForwardSite();
			if(numForwardSite>1){
				log.info("複数の転送設定があります:"+numForwardSite);
			}

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
		this.client.setMailAddrCharset(this.conf.getMailEncode());
		this.client.setCsvAddressBook(this.conf.getCsvAddressFile());
		//GoogleContactsAccesorの初期化
		GoogleContactsAccessor.initialize(this.conf.getGmailId(), this.conf.getGmailPasswd());
		this.client.setVcAddressBook(this.conf.getVcAddressFile());

		CharacterConverter subjectCharConv = new CharacterConverter();
		for (String file : conf.getForwardSubjectCharConvertFile()){
			try {
				subjectCharConv.load(new File(file));
			} catch (Exception e) {
				log.error("文字変換表("+file+")が読み込めませんでした。",e);
			}
		}
		ImodeForwardMail.setSubjectCharConv(subjectCharConv);

		if(conf.isForwardAddGoomojiSubject()){
			CharacterConverter goomojiSubjectCharConv = new CharacterConverter();
			if(conf.getForwardGoogleCharConvertFile()!=null) {
				try {
					goomojiSubjectCharConv.load(new File(conf.getForwardGoogleCharConvertFile()));
				} catch (Exception e) {
					log.error("文字変換表("+conf.getForwardGoogleCharConvertFile()+")が読み込めませんでした。",e);
				}
			}
			ImodeForwardMail.setGoomojiSubjectCharConv(goomojiSubjectCharConv);
		}

		StringConverter strConv = new StringConverter();
		if(conf.getForwardStringConvertFile()!=null) {
			try {
				strConv.load(new File(conf.getForwardStringConvertFile()));
			} catch (Exception e) {
				log.error("文字列変換表("+conf.getForwardStringConvertFile()+")が読み込めませんでした。",e);
			}
		}
		ImodeForwardMail.setStrConv(strConv);

		// 転送抑止ドメインリスト読み込み
		this.loadIgnoreDomainList(this.conf, 1);

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
		spicker = new SendMailPicker(conf, this, this.client, this.status);
		new SendMailBridge(conf, this.client, this.spicker, this.status);

		// メール転送
		Config forwardConf = this.conf;
		ForwardMailPicker fpicker = new ForwardMailPicker(forwardConf, this);
		forwarders.put(forwardConf, fpicker);
		
		for(int i=2; i<=numForwardSite; i++){
			try{
				log.info("Load Config file["+i+"] "+conffile.getAbsolutePath());
				FileInputStream is = new FileInputStream(conffile);
				is = new FileInputStream(conffile);
				forwardConf = new Config(is, i);
				fpicker = new ForwardMailPicker(forwardConf, this);
				forwarders.put(forwardConf, fpicker);

				// 転送抑止ドメインリスト読み込み
				this.loadIgnoreDomainList(forwardConf, i);

			}catch (Exception e) {
				log.fatal("Config Error. 設定ファイルに問題があります。",e);
				e.printStackTrace();
				System.exit(1);
			}
		}

		// skype
		this.skypeForwarder = new SkypeForwarder(conf.getForwardSkypeChat(),conf.getForwardSkypeSms(),conf);

		// im.kayac.com
		this.imKayacNotifier = new ImKayacNotifier(this.conf);

		// appnotifications
		this.appNotifications = new AppNotifications(this.conf, this.status);

		// Growl APIs
		// for Prowl
		this.prowlNotifier = GrowlNotifier.getInstance(ProwlClient.getInstance(), this.conf);

		// for Notify My Android
		this.nmaNotifier = GrowlNotifier.getInstance(NMAClient.getInstance(), this.conf);

		Date lastUpdate = null;
		while(true){
			if(lastUpdate != null){
				// 接続フラグを見るためにステータスファイルをチェック
				try{
					this.status.load();
				}catch (Exception e) {}
				long diff = System.currentTimeMillis() - lastUpdate.getTime();
				if(diff < conf.getForceCheckIntervalSec()*1000 && !this.status.needConnect()){
					//接続フラグが立っていなければ次のチェックまで待つ
					try{
						Thread.sleep(conf.getCheckFileIntervalSec()*1000);
					}catch (Exception e) {}
					continue;
				}
			}

			Map<Integer,List<String>> mailIdListMap = null;
			try{
				// メールID一覧取得(降順)
				mailIdListMap = this.client.getMailIdList();
				this.client.checkAddressBook();

			}catch (LoginException e) {
				log.error("ログインエラー",e);
				if(lastUpdate == null){
					// 起動直後はcookieが切れている可能性があるのでクッキーを破棄してリトライ
					log.info("起動直後は5秒後にログイン処理を行います。");
					try{
						Thread.sleep(1000*5);
					}catch (Exception ex) {
					}
					lastUpdate = new Date();
					continue;
				}
				try{
					// 別の場所でログインされた
					log.info("Wait "+this.conf.getLoginRetryIntervalSec()+" sec.");
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

			String newestId="0";	// 次のlastIdを求める
			Iterator<Integer> folderIdIte =  mailIdListMap.keySet().iterator();
			while(folderIdIte.hasNext()){
				// フォルダごとに処理
				Integer fid = folderIdIte.next();
				List<String> mailIdList = mailIdListMap.get(fid);

				folderProc(fid,mailIdList);

				if(!mailIdList.isEmpty()){
					String newestInFolder = mailIdList.get(0);
					if(newestId.compareToIgnoreCase(newestInFolder)<0){
						newestId = newestInFolder;
					}
				}
			}

			// 接続フラグのリセット
			this.status.resetNeedConnect();

			// status.ini の更新
			if(StringUtils.isBlank(this.status.getLastMailId())){
				this.status.setLastMailId(newestId);
				log.info("LastMailIdが空なので、次のメールから転送を開始します。");
			}
			String lastId = this.status.getLastMailId();
			if(lastId!=null && !lastId.equals(newestId)){
				this.status.setLastMailId(newestId);
				log.info("LastMailId("+newestId+")に更新しました");
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
			lastUpdate = new Date();

			// 次のチェックまで待つ
			try{
				Thread.sleep(conf.getCheckIntervalSec()*1000);
			}catch (Exception e) {}
		}

	}


	private void folderProc(Integer fid, List<String> mailIdList){
		String lastId = this.status.getLastMailId();
		log.info("FolderID "+fid+"  受信メールIDの数:"+mailIdList.size()+"  lastId:"+lastId);

		String newestId = "";

		if(StringUtils.isBlank(lastId)){
			if(!mailIdList.isEmpty()){
				// 最初の起動では現在の最新メールの次から転送処理する
				if(newestId.compareToIgnoreCase(mailIdList.get(0))<0){
					return;
				}
			}else{
				// メールがひとつも無かった
				return;
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
			appNotifications.pushPrepare(fid, forwardIdList.size());
			for (String id : forwardIdList) {
				this.forward(fid,id);
			}
		}
	}


	private void verCheck(){
		String verndor = System.getProperty("java.vendor");
		String version = System.getProperty("java.version");
		log.info("Java vendor  "+verndor);
		log.info("Java version "+version);
		log.info("defaultCharset "+Charset.defaultCharset());
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
	private void forward(int folderId, String mailId){
		if(folderId==ImodeNetClient.FolderIdSent
				&& numForwardSite == 1
				&& !this.conf.isForwardSent()){
			// 送信メールは転送しない
			return;
		}
		ImodeMail mail = null;
		try{
			// download
			mail = this.client.getMail(folderId, mailId);
			if(log.isInfoEnabled()){
				log.info("Downloaded Mail ########");
				log.info(mail.toLoggingString());
				log.info("########################");
			}
		}catch (Exception e) {
			log.warn("i mode.net mailId["+mailId+"] download Error.",e);
			return;
		}

		String from = mail.getFromAddr().getAddress();
		List<String> ignoreDomains = new ArrayList<String>();
		try{
			// 送信
			for (Map.Entry<Config, ForwardMailPicker> f : forwarders.entrySet()) {
				Config forwardConf = f.getKey();
				int id = forwardConf.getConfigId();

				if(folderId==ImodeNetClient.FolderIdSent
						&& !forwardConf.isForwardSent()){
					// 送信メールは転送しない
					continue;
				}
				
				//  転送抑止ドメインリストと比較して送信可否判定
				boolean notForward = false;
				ignoreDomains = ignoreDomainsMap.get(forwardConf);
				for (String domain : ignoreDomains) {
					if(from.endsWith(domain)){
						log.info("送信者:"+from+" のメール転送中止["+id+"]");
						notForward = true;
					}
				}
				if(notForward){
					continue;
				}
				
				ForwardMailPicker fpicker = f.getValue();
				if(forwardConf.isForwardAsync()){
					// 別スレッド(ForwardMailPicker)で送信。送信失敗時はリトライあり。
					fpicker.add(mail);
				}else{
					ImodeForwardMail forwardMail = new ImodeForwardMail(mail,forwardConf);
					forwardMail.send();
					if(numForwardSite>1){
						log.info("転送処理完了["+id+"]");
					}
				}
			}

		}catch (Exception e) {
			log.error("mail["+mailId+"] forward Error.",e);
			return;
		}

		//  転送抑止ドメインリストと比較してPush送信可否判定
		ignoreDomains = ignoreDomainsMap.get(this.conf);
		for (String domain : ignoreDomains) {
			if(from.endsWith(domain)){
				this.appNotifications.pushError(folderId);
				return;
			}
		}

		try{
			this.skypeForwarder.forward(mail);
		}catch (Exception e) {
			log.error("mail["+mailId+"] skype forward Error.",e);
			return;
		}

		try{
			this.imKayacNotifier.forward(mail);
		}catch (Exception e) {
			log.error("mail["+mailId+"] im.kayac forward Error.",e);
			return;
		}

		try{
			this.prowlNotifier.forward(mail);
		}catch (Exception e) {
			log.error("mail["+mailId+"] Prowl forward Error.",e);
			return;
		}

		try{
			this.nmaNotifier.forward(mail);
		}catch (Exception e) {
			log.error("mail["+mailId+"] NMA forward Error.",e);
			return;
		}

		try{
			this.appNotifications.push(folderId, mail);
		}catch (Exception e) {
			this.appNotifications.pushError(folderId);
			log.error("mail["+mailId+"] AppNotifications push Error.",e);
			return;
		}

		try{
			// 負荷をかけないように
			Thread.sleep(1000);
		}catch (Exception e) {}
	}

	/*
	 * 任意のメッセージを直接通知する。
	 */
	public void notify(String message){
		// XXX skypeForwarder?
		// XXX imKayacNotifier?
		this.appNotifications.push(message);
	}

	/*
	 * 転送抑止ドメインリスト作成
	 */
	private void loadIgnoreDomainList(Config conf, int index) {
		List<String> ignoreDomains = new ArrayList<String>();
		String ignoreDomainTxt = conf.getIgnoreDomainFile();
		File ignoreDomainFile = new File(ignoreDomainTxt);
		if(!ignoreDomainFile.exists()){
			log.info("# 転送抑止ドメインリスト["+index+"]("+ignoreDomainTxt+")は存在しません。");
			this.ignoreDomainsMap.put(conf, ignoreDomains);
			return;
		}
		BufferedReader br = null;
		FileReader fr = null;
		try{
			fr = new FileReader(ignoreDomainFile);
			br = new BufferedReader(fr);
			//int id = 0;

			String line = null;
			while((line = br.readLine()) != null){
				//id++;
				try{
					if(line.startsWith("#")){
						continue;
					}
					if(!line.contains(".")){
						continue;
					}
					ignoreDomains.add(line);

				}catch (Exception e) {
					log.warn("loadIgnoreDomainList error.",e);
				}
			}
			br.close();
		}catch (Exception e){
			log.warn("loadIgnoreDomainList "+ignoreDomainTxt+" error.",e);

		}finally{
			Util.safeclose(br);
			Util.safeclose(fr);
			String ignores = "";
			for (String domain : ignoreDomains) {
				ignores += domain + " ";
			}
			log.info("# 転送抑止ドメイン["+index+"]:"+ignores);
			this.ignoreDomainsMap.put(conf, ignoreDomains);
		}
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

