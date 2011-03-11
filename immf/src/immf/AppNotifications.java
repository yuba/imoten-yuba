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

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.mail.internet.InternetAddress;
import javax.net.ssl.SSLException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class AppNotifications extends DefaultHandler implements Runnable{
	private static final Log log = LogFactory.getLog(AppNotifications.class);
	private static final String CredUrl = "https://www.appnotifications.com/user_session.xml";
	private static final String PushUrl = "https://www.appnotifications.com/account/notifications.json";
	private static final String IconUrl = "http://imode.net/cmn/images/favicon.ico";
	private static final String Message = "新着iモードメールあり";
	private static final int ReconnectInterval = 3000;
	private static final int ReconnectCount = 100;
	
	private StatusManager status;
	private Deque<ImodeMail> recieveMailQueue = new LinkedList<ImodeMail>();
	private Deque<ImodeMail> sendMailQueue = new LinkedList<ImodeMail>();
	
	private DefaultHttpClient httpClient;
	private boolean elemCr = false;
	private boolean elemOk = false;

	private String email;
	private String password;
	private String message;
	private String sound;
	private String iconUrl;
	private boolean pushFromInfo;
	private boolean pushSubjectInfo;
	private boolean pushReplyButton;

	private String credentials;
	private String ok = "";
	private int newmails = 0;

	enum sounds {
		beep1("5.caf"),
		bellmodern("10.caf"),
		bellonetone("21.caf"),
		bellsimple("11.caf"),
		belltriple("12.caf"),
		detonatorcharge("20.caf"),
		digitalalarm("19.caf"),
		flourish("16.caf"),
		light("17.caf"),
		magicchime("18.caf"),
		magiccoin1("3.caf"),
		notifier1("1.caf"),
		notifier2("2.caf"),
		notifier3("4.caf"),
		orchestrallong("13.caf"),
		orchestralshort("14.caf"),
		score("15.caf");
		
		private String file;
		private sounds(String file){
			this.file = file;
		}
		public String filename() {
			return this.file;
		}
	}

	public AppNotifications(Config conf, StatusManager status){
		this.status = status;

		this.email = conf.getForwardPushEmail();
		this.password = conf.getForwardPushPassword();
		this.message = conf.getForwardPushMessage();
		if(this.message==null)
			this.message = Message;
		this.sound = soundFile(conf.getForwardPushSound());
		this.iconUrl = conf.getForwardPushIconUrl();
		if(this.iconUrl.isEmpty())
			this.iconUrl = IconUrl;
		this.pushFromInfo = conf.isForwardPushFrom();
		this.pushSubjectInfo = conf.isForwardPushSubject();
		this.pushReplyButton = conf.isForwardPushReplyButton();
		this.credentials = status.getPushCredentials();
		if(this.credentials==null)
			this.credentials = "";
	
		if(this.email.length()>0 && this.password.length()>0){
			Thread t = new Thread(this);
			t.setName("AppNotifications");
			t.setDaemon(true);
			t.start();
		}else{
			if(this.credentials.length()>0)
				this.setCredentials("");
		}
	}

	public void run(){
		boolean authOk = false;
		if(this.credentials.isEmpty()){
			if(!this.auth()){
				log.warn("認証失敗。登録したメールアドレスとパスワードを使用してください。");
				return;
			}
			authOk = true;
			log.info("credentials取得成功。push通知を開始します。");
		}else{
			log.info("push通知を開始します。");
		}
		int c = 0;
		while(true){
			try {
				Thread.sleep(ReconnectInterval);
			}catch (Exception e) {}

			// 今のところリトライ間隔は5分固定
			if(this.sendMailQueue.size()>0 && c++>ReconnectCount){
				log.info("push通知リトライ。");
				
			}else if(this.recieveMailQueue.size()>newmails){
				log.warn("キュー矛盾発生。");

			}else if(newmails==0){
				continue;
				
			}else if(this.recieveMailQueue.size()<newmails){
				// メールが溜まりきるまで待つ
				if(c++>100){
					log.warn("通知メール待ちタイムアウト発生。");
					newmails = this.recieveMailQueue.size();
				}
				continue;
			}
			c = 0;

			synchronized(this.recieveMailQueue){
				for(int i = this.recieveMailQueue.size(); i>0; i--){
					this.sendMailQueue.add(this.recieveMailQueue.remove());
				}
				newmails = 0;
			}

			// push通知実施
			int count = this.sendMailQueue.size();
			ImodeMail mail = this.sendMailQueue.peek();
			String pushMessage = this.message;
			String pushCommand = "";

			if(!this.pushFromInfo){
				pushMessage += "("+Integer.toString(count)+"通)";
			}
			String delimiter = "";
			if(!pushMessage.isEmpty()){
				delimiter = "\n";
			}
			if(mail!=null && this.pushSubjectInfo){
				String subject = mail.getSubject();
				pushMessage += delimiter + subject;
				delimiter = " ";
			}
			if(mail!=null && this.pushFromInfo){
				InternetAddress fromAddr = mail.getFromAddr();
				String from = fromAddr.getPersonal();
				if(from==null||from.isEmpty()){
					from = fromAddr.getAddress();
				}
				if(count==1){
					pushMessage += delimiter + "("+from+")";
				}else{
					pushMessage += delimiter + "("+from+",他"+Integer.toString(count-1)+"通)";
				}
			}
			if(mail!=null && this.pushReplyButton){
				// スペース(%20)は使用禁止。+に置換されてしまう。
				InternetAddress fromAddr = mail.getFromAddr();
				pushCommand = "mailto:" + fromAddr.getAddress();
				for (InternetAddress to : mail.getToAddrList()) {
					pushCommand += "%2C" + to.getAddress();
				}
				String ccList = "";
				for (InternetAddress cc : mail.getCcAddrList()) {
					if(ccList.isEmpty()){
						ccList = "?cc=" + cc.getAddress();
					}else{
						ccList += "%2C" + cc.getAddress();
					}
				}
				if(!ccList.isEmpty()){
					pushCommand += ccList;
					//pushCommand += "&subject=";
				//}else{
					//pushCommand += "?subject=";
				}
				/* 
				 * XXX
				 * Apple URL Scheme Referenceでは RFC2368 に従うとあるが、これに従い
				 * 8bit文字を使用した subject のエンコードとして RFC2047 に従ってもデコードされない。
				 * また、エンコードをせずに生8bit文字を送信すると Push が悲鳴をあげる。
				 * Push のバージョンアップで解消するかなぁ。
				 */
			}
			try{
				log.info("push通知:"+pushMessage.replace("\n","/"));
				if(this.pushReplyButton){
					this.send(pushMessage, pushCommand);
				}else{
					this.send(pushMessage);
				}
				authOk = true;
				this.sendMailQueue.clear();

			}catch(SocketException se){
				// 通信異常。リトライ。
				log.warn("通信エラーによりpush通知失敗。後でリトライします。",se);
				
			}catch(ClientProtocolException cpe){
				// 通信異常。リトライ。
				log.warn("通信エラーによりpush通知失敗。後でリトライします。",cpe);
				
			}catch(UnknownHostException uhe){
				// DNSエラー。リトライ。
				log.warn("DNSエラーによりpush通知失敗。後でリトライします。",uhe);

			}catch(SSLException ssle){
				// SSLエラー。リトライ。
				log.warn("SSLエラーによりpush通知失敗。後でリトライします。",ssle);

			}catch(MyHttpException mhe){
				// 通信異常。リトライ。
				log.warn("サーバエラーによりpush通知失敗。後でリトライします。",mhe);

			}catch(Exception e){
				log.warn("push失敗。リトライします。",e);
				if(authOk){
					continue;
				}
				
				// 未認証あるいは通知が一度も成功していない場合に再認証を試みる
				this.credentials = "";
				
				// status.iniに直接記述したcredentials誤りの場合があるため再認証してリトライする。
				if(this.auth()){
					authOk = true;
					log.info("credentials再取得成功。");
					try{
						log.info("push通知:"+pushMessage.replace("\n","/"));
						if(this.pushReplyButton){
							this.send(pushMessage, pushCommand);
						}else{
							this.send(pushMessage);
						}
						this.sendMailQueue.clear();

					}catch(Exception ex){
						log.warn("push失敗。push通知を停止します。",e);
						this.setCredentials("");
						this.sendMailQueue.clear();
						return;
					}
				}else{
					log.warn("再認証失敗。push通知を停止します。");
					this.setCredentials("");
					this.sendMailQueue.clear();
					return;
				}
			}
		}
	}
	
	public void pushPrepare(int folderId, int count){
		if(!isActive()){
			return;
		}
		if(folderId==ImodeNetClient.FolderIdSent){
			return;
		}
		log.info("通知対象メール"+count+"通");
		newmails += count;
	}
	
	public void push(int folderId, ImodeMail mail) throws Exception{
		if(!isActive()){
			return;
		}
		if(folderId==ImodeNetClient.FolderIdSent){
			return;
		}
		this.recieveMailQueue.add(mail);
	}
	
	public void push(String message){
		if(!isActive()){
			return;
		}
		try{
			this.send(message);
		}catch(Exception e){
			log.warn("push失敗",e);
		}
	}
	public void pushError(int folderId){
		if(!isActive()){
			return;
		}
		if(folderId==ImodeNetClient.FolderIdSent){
			return;
		}
		if(newmails>0)newmails--;
	}
	
	
	private boolean auth(){
		boolean httpOk = false;
		while(!httpOk){
			try{
				SAXParserFactory spfactory = SAXParserFactory.newInstance();
				SAXParser parser = spfactory.newSAXParser();
				byte[] xml = this.getCredentials();
				InputStream is = new ByteArrayInputStream(xml);
				parser.parse(is, this);
				httpOk = true;
			}catch(Exception e){
				log.warn("通信エラー。5分後にリトライします。",e);
				try {
					Thread.sleep(ReconnectInterval*ReconnectCount);
				}catch (Exception ee) {}
			}
		}
		if(!ok.equals("OK")){
			ok = "";
			return false;
		}
		this.setCredentials();
		return true;
	}
	
	private byte[] getCredentials() throws Exception {
		this.httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(CredUrl);
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("user_session[email]",this.email));
		formparams.add(new BasicNameValuePair("user_session[password]",this.password));
		UrlEncodedFormEntity entity = null;
		try{
			entity = new UrlEncodedFormEntity(formparams,"UTF-8");
		}catch (Exception e) {}
		post.setEntity(entity);
		byte[] xml = null;
		try{
			HttpResponse res = this.httpClient.execute(post);
			int status = res.getStatusLine().getStatusCode();
			if(status == 200){
				xml = EntityUtils.toByteArray(res.getEntity());
			}
		}finally{
			post.abort();
		}
		return xml;
	}
	
	private void setCredentials() {
		this.status.setPushCredentials(this.credentials);
		try{
			this.status.save();
			log.info("statusファイルを保存しました");
		}catch (Exception e) {
			log.error("Status File save Error.",e);
		}				
	}
	
	private void setCredentials(String c) {
		this.credentials = c;
		this.setCredentials();
	}
	
	/*
	 * Pushウィンドウ 表示可能文字数覚え書き
	 * 
	 * [通常]
	 *  半角文字140字、全角文字30字
	 *  
	 * [action_loc_keyに""設定時]
	 *  全角文字27字(3文字減る)
	 * [action_loc_keyに"Reply"設定時]
	 *  全角文字26字(4文字減る)
	 * [action_loc_keyに"返信"設定時]
	 *  全角文字25字(5文字減る)
	 *  
	 * [run_commandに50byte設定時]
	 *  全角文字21字(9文字減る)
	 * [run_commandに80byte設定時]
	 *  全角文字17字(13文字減る)
	 */
	private void send(String message, String command) throws Exception {
		this.httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(PushUrl);
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("user_credentials",this.credentials));
		if(!this.sound.isEmpty()){
			formparams.add(new BasicNameValuePair("notification[sound]",this.sound));
		}
		formparams.add(new BasicNameValuePair("notification[message]",message));
		formparams.add(new BasicNameValuePair("notification[icon_url]",this.iconUrl));
		formparams.add(new BasicNameValuePair("notification[message_level]","2"));
		formparams.add(new BasicNameValuePair("notification[silent]","0"));
		if(command!=null && !command.isEmpty()){
			// 表示文字数をかせぐためaction_loc_keyは半角で・・・
			formparams.add(new BasicNameValuePair("notification[action_loc_key]","Reply"));
			formparams.add(new BasicNameValuePair("notification[run_command]",command));
		}else{
			formparams.add(new BasicNameValuePair("notification[action_loc_key]",""));
		}
		UrlEncodedFormEntity entity = null;
		try{
			entity = new UrlEncodedFormEntity(formparams,"UTF-8");
		}catch (Exception e) {}
		post.setEntity(entity);
		try{
			HttpResponse res = this.httpClient.execute(post);
			int status = res.getStatusLine().getStatusCode();
			if(status != 200){
				if(status >= 500){
					throw new MyHttpException("http server error. status="+status);
				}else{
					throw new Exception("http server error. status="+status);
				}
			}
			String resString = EntityUtils.toString(res.getEntity());
			log.info("サーバ応答:" + resString);
			JSONObject json = JSONObject.fromObject(resString);
			int id = json.getInt("id");
			if(id<1){
				throw new Exception("illegal id returned");
			}
		}catch(JSONException e){
			/*
			 * (1)JSONObject.fromObjectのexceptionであればpostデータ不正
			 * (2)id取得ができていれば成功
			 *  (なお認証失敗だと {"Response":"Not Authorized"} が返り、HTTPのstatusが401となる)
			 */
			throw new Exception("wrong request");
		}finally{
			post.abort();
		}
	}

	private void send(String message) throws Exception {
		this.send(message, null);
	}

	private String soundFile(String key){
		if(key==null||key.isEmpty()){
			return "";
		}
		String k = key.toLowerCase().replaceAll("\\s", "");
		String filename = "";
		for(sounds s : sounds.values()){
			if(k.equals(s.toString())){
				filename = s.filename();
				log.info("通知音:"+key);
				break;
			}
		}
		if(filename.isEmpty()){
			log.info("通知音取得失敗");
			filename = "0.caf";
		}
		return filename;
	}

	private boolean isActive(){
		if(this.credentials!=null&&this.credentials.length()>0){
			return true;
		}else{
			return false;
		}
	}

	/*
	 * SAX
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(qName.equals("single-access-token")){
			this.elemCr = true;
		}
		if(qName.equals("OK")){
			this.elemOk = true;
		}
	}
	public void endElement(String uri, String localName, String qName) {
		if(qName.equals("single-access-token")){
			this.elemCr = false;
		}
		if(qName.equals("OK")){
			this.elemOk = false;
		}
	}
	public void characters(char[] ch, int offset, int length) {
		if(this.elemCr){
			this.credentials = new String(ch, offset, length);
		}
		if(this.elemOk){
			this.ok = new String(ch, offset, length);
		}
	}
	
	class MyHttpException extends Exception {
		private static final long serialVersionUID = -5836602194926641698L;
		public MyHttpException(String s) {
			super(s);
		}
	}
}
