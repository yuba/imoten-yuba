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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Config {
	public static final String ConfFile = "../imoten.ini";
	public static final String StatusFile = "../status.ini";
	public static final String CsvAddressFile = "../address.csv";
	public static final String VcAddressFile = "../address.vcf";
	public static final String IgnoreDomainFile = "../notforward.txt";

	private static final Log log = LogFactory.getLog(Config.class);
	private Properties prop = new Properties();

	// imode.netアカウント
	private String docomoId;
	private String docomoPasswd;

	// SMTPサーバ
	private String smtpServer;
	private int smtpPort = 25;
	private boolean smtpTls = false;
	private String smtpMailAddress;
	private int smtpConnectTimeoutSec = 10;
	private int smtpTimeoutSec = 30;

	// SMTP認証
	private String smtpUser;
	private String smtpPasswd;

	// POP before SMTP認証
	private String popServer;
	//private int popPort = 110;
	private String popUser;
	private String popPasswd;
	//private boolean popSsl = false;

	// 題名の絵文字を[晴れ]のような文字列に置き換えるかどうか
	private boolean subjectEmojiReplace=true;

	// DontReplace:   置き換えない
	// ToString:      [晴れ]のような文字列に置き換え
	// ToInlineImage: Gmailの画像をダウンロードしてメールに添付(HTMLメールになる)
	// ToWebLink:     imgタグでGmailの画像へリンクする(HTMLメールになる)
	public enum BodyEmojiReplace {DontReplace,ToLabel,ToInlineImage,ToWebLink};

	// メールのボディの絵文字置き換え方法
	private BodyEmojiReplace bodyEmojiReplace=BodyEmojiReplace.ToInlineImage;

	// メールのボディの絵文字の位置とサイズ
	private String bodyEmojiVAlign = "middle";
	private String bodyEmojiSize = null;
	private String bodyEmojiVAlignHtml = null;
	private String bodyEmojiSizeHtml = null;

	// 受信メールの転送後の題名の先頭に追加する文字列
	private String subjectAppendPrefix="";

	// 受信メールの転送後の題名の最後に追加する文字列
	private String subjectAppendSuffix="";

	// 送信メールの転送後の題名の先頭に追加する文字列
	private String sentSubjectAppendPrefix="";

	// 送信メールの転送後の題名の最後に追加する文字列
	private String sentSubjectAppendSuffix="";

	// 送信メールも受信メールと同じように転送する場合true
	private boolean forwardSent = false;

	// 転送先 TO
	private List<String> forwardTo = new ArrayList<String>();

	// 転送先 CC
	private List<String> forwardCc = new ArrayList<String>();

	// 転送先 BCC
	private List<String> forwardBcc = new ArrayList<String>();

	// 転送メールのreplay-to
	private List<String>  forwardReplyTo = new ArrayList<String>();

	// 転送時にメールヘッダのFrom,To,CCなどの情報を送信用アカウントで送信するかどうか
	// falseの場合はiモードメールの情報をFrom,Toヘッダに設定します
	private boolean rewriteAddress = true;

	// trueにするとiモードメールのFrom,To,CCなどの情報をBodyの先頭に付加します
	private boolean headerToBody = true;

	// trueにするとiモードメールのToに自分のアドレスを付加しない
	private boolean hideMyaddr = false;

	// 転送メールの題名の文字変換ファイル
	private List<String> forwardSubjectCharConvertFile = new ArrayList<String>();

	// X-Goomoji-Subject関連
	private boolean forwardAddGoomojiSubject = false;
	private String forwardGoogleCharConvertFile = null;

	// 転送メールの本文の文字列変換ファイル
	private String forwardStringConvertFile = null;

	// 転送時に非同期にリトライ処理を行うかどうか(trueだと転送を別スレッドで行う)
	private boolean forwardAsync = false;

	// 定期的に新着をチェックする場合のチェック間隔(秒)
	private int checkIntervalSec = 60;
	private int checkFileIntervalSec = checkIntervalSec;

	// ログインエラー時のリトライ間隔(秒)
	// 失敗時はimode.netのメンテナンスの可能性があるので長めで
	private int loginRetryIntervalSec = 60 * 10;

	// 転送エラー時のリトライ間隔(秒)
	// 失敗時はメールサーバのメンテナンスの可能性があるので長めで
	// forwardAsync = true の場合のみ有効
	private int forwardRetryIntervalSec = 60 * 10;

	// trueの場合はcookieの情報をファイルに保存する。
	// 再起動など短時間プログラムを停止してもログイン状態が保持されるので
	// ログインメールが飛ばなくてすむ
	// 保存していても長時間間隔が空くと再ログインが必要になる
	private boolean saveCookie = true;

	// 最後のメールID,cookie(設定による)の情報を保存するファイル
	private String statusFile = StatusFile;

	// CSV形式のアドレス帳ファイル
	private String csvAddressFile = CsvAddressFile;

	// vCard形式のアドレス帳ファイル
	private String vcAddressFile = VcAddressFile;

	// Google Contacts APIで利用するGmailアカウントの情報
	private String gmailId;
	private String gmailPasswd;

	// 転送を無視するドメインリスト
	private String ignoreDomainFile = IgnoreDomainFile;

	// httpクライアントの接続タイムアウト
	private int httpConnectTimeoutSec = 10;

	// httpクライアントのreadタイムアウト
	private int httpSoTimeoutSec = 10;

	// Javamailのdebugフラグ
	private boolean mailDebugEnable = false;

	// メールのエンコード(charset)
	private static final String DefaultMailEncode = "UTF-8";
	private String mailEncode = DefaultMailEncode;

	// メールのフォント
	private String mailFontFamily = null;

	// 転送メールを multipart/alternative にするかどうか
	private boolean mailAlternative = false;

	// メールのhtml部分のContent-Transfer-Encoding
	private String contentTransferEncoding = null;

	// 送信用設定
	private int senderSmtpPort = -1;
	private String senderUser = "z8$k>Lo2#aEeo@a(aw!";
	private String senderPasswd = "<87a!Oa#3gpoYz0'->L";
	private String senderAlwaysBcc = null;
	private boolean senderMailForcePlainText = true;
	private List<String> senderCharCovertFile = new ArrayList<String>();
	private boolean senderUseGoomojiSubject = false;
	private String senderGoogleCharConvertFile = null;
	private boolean senderConvertSoftbankSjis = false;
	private int senderDuplicationCheckTimeSec = 0;
	private boolean senderStripiPhoneQuote = false;
	private boolean senderDocomoStyleSubject = false;
	private boolean senderAsync = false;	// trueだと送信を別スレッドで行う。エラー時はエラーメールが転送アドレスに送信される。

	// 送信用TLS
	private String senderTlsKeystore;
	private String senderTlsKeyType;
	private String senderTlsKeyPasswd;

	// Skypeに転送
	private String forwardSkypeChat;
	private String forwardSkypeSms;

	// im.kayac.com設定
	private String forwardImKayacUsername;
	private String forwardImKayacSecret;

	// AppNotifications(iPhone/iPod/iPadアプリPush)
	private String forwardPushEmail;
	private String forwardPushPassword;
	private String forwardPushMessage;
	private String forwardPushSound;
	private String forwardPushIconUrl;
	private boolean forwardPushNotifyFrom = true;
	private boolean forwardPushNotifySubject = false;
	private boolean forwardPushReplyButton = false;

	public Config(InputStream is) throws Exception{
		Reader reader = null;
		try{
			reader = new InputStreamReader(is,"UTF-8");
			this.prop.load(reader);
		}finally{
			Util.safeclose(reader);
		}


		this.docomoId = 		getString("docomo.id", null);
		this.docomoPasswd = 	getString("docomo.passwd", null);
		this.smtpServer = 		getString("smtp.server", null);
		this.smtpPort = 		getInt(   "smtp.port", this.smtpPort);
		this.smtpConnectTimeoutSec = getInt("smtp.connecttimeout", this.smtpConnectTimeoutSec);
		this.smtpTimeoutSec = getInt("smtp.timeout", this.smtpTimeoutSec);
		this.smtpTls = 			getBoolean("smtp.tls", this.smtpTls);
		this.smtpMailAddress = 	getString("smtp.from", null);
		this.smtpUser = 		getString("smtp.auth.user", null);
		this.smtpPasswd = 		getString("smtp.auth.passwd", null);
		this.popServer = 		getString("popbeforesmtp.server", null);
		//this.popPort = 			getInt(   "popbeforesmtp.port", this.popPort);
		this.popUser = 			getString("popbeforesmtp.user", null);
		this.popPasswd =		getString("popbeforesmtp.passwd", null);
		//this.popSsl = 			getBoolean("popbeforesmtp.ssl", this.popSsl);
		this.subjectEmojiReplace = getBoolean("emojireplace.subject", this.subjectEmojiReplace);
		String s = getString("emojireplace.body", "inline");
		if(s.equalsIgnoreCase("inline")){
			this.bodyEmojiReplace = BodyEmojiReplace.ToInlineImage;
		}else if(s.equalsIgnoreCase("label")){
			this.bodyEmojiReplace = BodyEmojiReplace.ToLabel;
		}else if(s.equalsIgnoreCase("link")){
			this.bodyEmojiReplace = BodyEmojiReplace.ToWebLink;
		}else{
			this.bodyEmojiReplace = BodyEmojiReplace.DontReplace;
		}
		this.bodyEmojiVAlign = getString("mail.emojiverticalalign", this.bodyEmojiVAlign);
		this.bodyEmojiSize = getString("mail.emojisize", null);
		this.bodyEmojiVAlignHtml = getString("mail.emojiverticalalignhtml", this.bodyEmojiVAlign);
		this.bodyEmojiSizeHtml = getString("mail.emojisizehtml", this.bodyEmojiSize);
		this.subjectAppendPrefix = getString("forward.subject.prefix", this.subjectAppendPrefix);
		this.subjectAppendSuffix = getString("forward.subject.suffix", this.subjectAppendSuffix);
		this.sentSubjectAppendPrefix = getString("forward.sent.subject.prefix", this.sentSubjectAppendPrefix);
		this.sentSubjectAppendSuffix = getString("forward.sent.subject.suffix", this.sentSubjectAppendSuffix);
		this.forwardSent = getBoolean("forward.sent", this.forwardSent);
		this.forwardTo = splitComma(getString("forward.to", ""));
		this.forwardCc = splitComma(getString("forward.cc", ""));
		this.forwardBcc = splitComma(getString("forward.bcc", ""));
		this.forwardSkypeChat = getString("forward.skype.chat", null);
		this.forwardSkypeSms = getString("forward.skype.sms", null);
		this.forwardImKayacUsername = getString("forward.im.kayac.username", "");
		this.forwardImKayacSecret = getString("forward.im.kayac.secret", "");
		this.forwardPushEmail = getString("forward.push.email","");
		this.forwardPushPassword = getString("forward.push.password","");
		this.forwardPushMessage = getString("forward.push.message",null);
		this.forwardPushSound = getString("forward.push.sound","");
		this.forwardPushIconUrl = getString("forward.push.iconurl","");
		this.forwardPushNotifyFrom = getBoolean("forward.push.notifyfrom",this.forwardPushNotifyFrom);
		this.forwardPushNotifySubject = getBoolean("forward.push.notifysubject",this.forwardPushNotifySubject);
		this.forwardPushReplyButton = getBoolean("forward.push.replybutton",this.forwardPushReplyButton);
		this.forwardReplyTo = splitComma(getString("forward.replyto", ""));
		this.rewriteAddress = getBoolean("forward.rewriteaddress", this.rewriteAddress);
		this.headerToBody = getBoolean("forward.headertobody", this.headerToBody);
		this.hideMyaddr = getBoolean("forward.hidemyaddr", this.hideMyaddr);
		this.forwardSubjectCharConvertFile = splitComma(getString("forward.subject.charconvfile", ""));
		this.forwardAddGoomojiSubject = getBoolean("forward.subject.addgoomoji", this.forwardAddGoomojiSubject);
		this.forwardGoogleCharConvertFile = getString("forward.subject.googlecharconvfile", this.forwardGoogleCharConvertFile);
		// パラメータ名は用途がわかりやすいように urlconv にしておく
		this.forwardStringConvertFile = getString("forward.body.urlconvfile", this.forwardStringConvertFile);
		this.forwardAsync = getBoolean("forward.async", this.forwardAsync);
		this.ignoreDomainFile = getString("forward.ignoredomainfile", this.ignoreDomainFile);
		this.checkIntervalSec = getInt("imodenet.checkinterval", this.checkIntervalSec);
		this.checkFileIntervalSec = getInt("imodenet.checkfileinterval", this.checkIntervalSec);
		this.loginRetryIntervalSec = getInt("imodenet.logininterval", this.loginRetryIntervalSec);
		this.forwardRetryIntervalSec = getInt("forward.retryinterval", this.forwardRetryIntervalSec);
		this.saveCookie = getBoolean("save.cookie", this.saveCookie);
		this.statusFile = getString("save.filename", this.statusFile);
		this.csvAddressFile = getString("addressbook.csv", this.csvAddressFile);
		this.vcAddressFile = getString("addressbook.vcard", this.vcAddressFile);
		this.gmailId = getString("gmail.id", null);
		this.gmailPasswd = getString("gmail.passwd", null);
		this.httpConnectTimeoutSec = getInt("http.conntimeout", this.httpConnectTimeoutSec);
		this.httpSoTimeoutSec = getInt("http.sotimeout", this.httpSoTimeoutSec);
		this.mailDebugEnable = getBoolean("mail.debug", this.mailDebugEnable);
		this.mailEncode = getString("mail.encode", this.mailEncode);
		this.contentTransferEncoding = getString("mail.contenttransferencoding", null);
		this.mailFontFamily = getString("mail.fontfamily", null);
		this.mailAlternative = getBoolean("mail.alternative", this.mailAlternative);
		this.senderSmtpPort = getInt("sender.smtp.port", this.senderSmtpPort);
		this.senderUser = getString("sender.smtp.user", this.senderUser);
		this.senderPasswd = getString("sender.smtp.passwd", this.senderPasswd);
		this.senderAlwaysBcc = getString("sender.alwaysbcc", this.senderAlwaysBcc);
		this.senderCharCovertFile = splitComma(getString("sender.charconvfile", ""));
		// imode.netでhtmlをチェックしてるようで、PCで作成したhtmlメールはエラーになるのでテキストのみ許可
		this.senderMailForcePlainText = getBoolean("sender.forceplaintext", this.senderMailForcePlainText);
		this.senderTlsKeystore = getString("sender.smtp.tls.keystore", null);
		this.senderTlsKeyType = getString("sender.smtp.tls.keytype", "JKS");
		this.senderTlsKeyPasswd = getString("sender.smtp.tls.keypasswd", null);
		this.senderGoogleCharConvertFile = getString("sender.googlecharconvfile", null);
		this.senderUseGoomojiSubject = getBoolean("sender.usegoomojisubject", this.senderUseGoomojiSubject);
		this.senderConvertSoftbankSjis = getBoolean("sender.convertsoftbanksjis", this.senderConvertSoftbankSjis);
		this.senderDuplicationCheckTimeSec = getInt("sender.duplicationchecktime", this.senderDuplicationCheckTimeSec);
		this.senderStripiPhoneQuote = getBoolean("sender.stripiphonequote", this.senderStripiPhoneQuote);
		this.senderDocomoStyleSubject = getBoolean("sender.docomostylesubject", this.senderDocomoStyleSubject);
		this.senderAsync = getBoolean("sender.async", this.senderAsync);

		// 最小値
		this.checkIntervalSec = Math.max(this.checkIntervalSec, 3);
		this.checkFileIntervalSec = Math.max(this.checkFileIntervalSec, 3);
		this.smtpConnectTimeoutSec = Math.max(this.smtpConnectTimeoutSec, 3);
		this.smtpTimeoutSec = Math.max(this.smtpTimeoutSec, 3);
		this.loginRetryIntervalSec = Math.max(this.loginRetryIntervalSec, 3);
		this.forwardRetryIntervalSec = Math.max(this.forwardRetryIntervalSec, 3);
		this.httpConnectTimeoutSec = Math.max(this.httpConnectTimeoutSec, 3);
		this.httpSoTimeoutSec = Math.max(this.httpSoTimeoutSec, 3);

		try{
			Charset.forName(this.mailEncode).name();
		}catch (Throwable e) {
			log.warn("mail.encode["+this.mailEncode+"] Error("+e.getMessage()+"). use "+DefaultMailEncode);
			this.mailEncode = DefaultMailEncode;
		}
	}

	private static List<String> splitComma(String str){
		List<String> r = new ArrayList<String>();
		for(String s : str.split(",")){
			s = stripSpace(s);
			if(s.isEmpty()){
				continue;
			}
			r.add(s);
		}
		return r;
	}
	private static String stripSpace(String s){
		return s.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
	}
	private String getString(String key,String def){
		return this.prop.getProperty(key, def);
	}
	private int getInt(String key, int def){
		return Integer.parseInt(this.prop.getProperty(key, Integer.toString(def)));
	}
	private boolean getBoolean(String key, boolean def){
		return this.prop.getProperty(key, Boolean.toString(def)).equalsIgnoreCase("true");
	}

	public String getDocomoId() {
		return docomoId;
	}

	public String getDocomoPasswd() {
		return docomoPasswd;
	}

	public String getGmailId() {
		return gmailId;
	}

	public String getGmailPasswd() {
		return gmailPasswd;
	}

	public String getSmtpServer() {
		return smtpServer;
	}

	public int getSmtpPort() {
		return smtpPort;
	}

	public boolean isSmtpTls() {
		return smtpTls;
	}

	public String getSmtpMailAddress() {
		return smtpMailAddress;
	}

	public String getSmtpUser() {
		return smtpUser;
	}

	public String getSmtpPasswd() {
		return smtpPasswd;
	}

	public String getPopServer() {
		return popServer;
	}

	/*
	public int getPopPort() {
		return popPort;
	}
	*/

	public String getPopUser() {
		return popUser;
	}

	public String getPopPasswd() {
		return popPasswd;
	}
	/*
	public boolean isPopSsl() {
		return popSsl;
	}
	*/

	public boolean isSubjectEmojiReplace() {
		return subjectEmojiReplace;
	}

	public BodyEmojiReplace getBodyEmojiReplace() {
		return bodyEmojiReplace;
	}

	public String getBodyEmojiVAlign() {
		return bodyEmojiVAlign;
	}

	public String getBodyEmojiSize() {
		return bodyEmojiSize;
	}

	public String getBodyEmojiVAlignHtml() {
		return bodyEmojiVAlignHtml;
	}

	public String getBodyEmojiSizeHtml() {
		return bodyEmojiSizeHtml;
	}

	public String getSubjectAppendPrefix() {
		return subjectAppendPrefix;
	}

	public List<String> getForwardTo() {
		return forwardTo;
	}

	public List<String> getForwardCc() {
		return forwardCc;
	}

	public List<String> getForwardBcc() {
		return forwardBcc;
	}

	public boolean isRewriteAddress() {
		return rewriteAddress;
	}

	public boolean isHeaderToBody() {
		return headerToBody;
	}

	public boolean isHideMyaddr() {
		return hideMyaddr;
	}

	public int getCheckIntervalSec() {
		return checkIntervalSec;
	}

	public int getCheckFileIntervalSec() {
		return checkFileIntervalSec;
	}

	public int getLoginRetryIntervalSec() {
		return loginRetryIntervalSec;
	}

	public int getForwardRetryIntervalSec() {
		return forwardRetryIntervalSec;
	}

	public boolean isSaveCookie() {
		return saveCookie;
	}

	public String getStatusFile() {
		return statusFile;
	}

	public int getHttpConnectTimeoutSec() {
		return httpConnectTimeoutSec;
	}

	public int getHttpSoTimeoutSec() {
		return httpSoTimeoutSec;
	}

	public int getSmtpConnectTimeoutSec() {
		return smtpConnectTimeoutSec;
	}

	public int getSmtpTimeoutSec() {
		return smtpTimeoutSec;
	}

	public static String getConfFile() {
		return ConfFile;
	}

	public Properties getProp() {
		return prop;
	}

	public List<String> getForwardReplyTo() {
		return forwardReplyTo;
	}

	public boolean isMailDebugEnable() {
		return mailDebugEnable;
	}

	public String getMailEncode() {
		return mailEncode;
	}

	public String getMailFontFamily() {
		return mailFontFamily;
	}

	public int getSenderSmtpPort() {
		return senderSmtpPort;
	}

	public String getSenderUser() {
		return senderUser;
	}

	public String getSenderPasswd() {
		return senderPasswd;
	}

	public String getSenderAlwaysBcc() {
		return senderAlwaysBcc;
	}

	public String getContentTransferEncoding() {
		return contentTransferEncoding;
	}

	public String getSenderTlsKeystore() {
		return senderTlsKeystore;
	}

	public String getSenderTlsKeyType() {
		return senderTlsKeyType;
	}

	public String getSenderTlsKeyPasswd() {
		return senderTlsKeyPasswd;
	}

	public boolean isSenderMailForcePlainText() {
		return senderMailForcePlainText;
	}

	public boolean isMailAlternative() {
		return mailAlternative;
	}

	public String getForwardSkypeChat() {
		return forwardSkypeChat;
	}

	public String getForwardSkypeSms() {
		return forwardSkypeSms;
	}

	public String getForwardImKayacUsername() {
		return forwardImKayacUsername;
	}

	public String getForwardImKayacSecret() {
		return forwardImKayacSecret;
	}

	public String getForwardPushEmail() {
		return forwardPushEmail;
	}

	public String getForwardPushPassword() {
		return forwardPushPassword;
	}

	public String getForwardPushMessage() {
		return forwardPushMessage;
	}

	public String getForwardPushSound() {
		return forwardPushSound;
	}

	public String getForwardPushIconUrl() {
		return forwardPushIconUrl;
	}

	public boolean isForwardPushFrom() {
		return forwardPushNotifyFrom;
	}

	public boolean isForwardPushSubject() {
		return forwardPushNotifySubject;
	}

	public boolean isForwardPushReplyButton() {
		return forwardPushReplyButton;
	}

	public String getIgnoreDomainFile() {
		return ignoreDomainFile;
	}

	public String getCsvAddressFile() {
		return csvAddressFile;
	}

	public String getVcAddressFile() {
		return vcAddressFile;
	}

	public List<String> getSenderCharCovertFile() {
		return senderCharCovertFile;
	}

	public String getSenderGoogleCharConvertFile() {
		return senderGoogleCharConvertFile;
	}

	public boolean isSenderUseGoomojiSubject() {
		return senderUseGoomojiSubject;
	}

	public boolean isSenderConvertSoftbankSjis() {
		return senderConvertSoftbankSjis;
	}

	public boolean isSenderStripiPhoneQuote() {
		return senderStripiPhoneQuote;
	}

	public boolean isSenderDocomoStyleSubject() {
		return senderDocomoStyleSubject;
	}

	public int getSenderDuplicationCheckTimeSec() {
		return senderDuplicationCheckTimeSec;
	}

	public List<String> getForwardSubjectCharConvertFile() {
		return forwardSubjectCharConvertFile;
	}

	public boolean isForwardAddGoomojiSubject() {
		return forwardAddGoomojiSubject;
	}

	public String getForwardGoogleCharConvertFile() {
		return forwardGoogleCharConvertFile;
	}

	public String getForwardStringConvertFile() {
		return forwardStringConvertFile;
	}

	public boolean isForwardAsync() {
		return forwardAsync;
	}

	public boolean isSenderAsync() {
		return senderAsync;
	}

	public String getSentSubjectAppendPrefix() {
		return sentSubjectAppendPrefix;
	}

	public boolean isForwardSent() {
		return forwardSent;
	}

	public String getSubjectAppendSuffix() {
		return subjectAppendSuffix;
	}

	public String getSentSubjectAppendSuffix() {
		return sentSubjectAppendSuffix;
	}
}

