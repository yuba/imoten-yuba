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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.internet.InternetAddress;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class ImodeNetClient implements Closeable{
	private static final Log log = LogFactory.getLog(ImodeNetClient.class);
	private static final String LoginUrl = 		"https://imode.net/dcm/dfw";
//	private static final String JsonUrl = 		"https://imode.net/imail/aclrs/acgi/";
	private static final String JsonUrl = 		"https://imode.net/imail/oexaf/acgi/";
	private static final String AttachedFileUrl ="https://imode.net/imail/oexaf/acgi/mailfileget";
	private static final String InlineFileUrl = 	"https://imode.net/imail/oexaf/acgi/mailimgget";
	private static final String SendMailUrl = 	"https://imode.net/imail/oexaf/acgi/mailsend";
	private static final String FileupUrl = 		"https://imode.net/imail/oexaf/acgi/fileupd";
	private static final String ImgupUrl = 		"https://imode.net/imail/oexaf/acgi/pcimgupd";
	private static final String PcAddrListUrl = 	"https://imode.net/imail/oexaf/acgi/pcaddrlist";
	private static final String DsAddrListUrl = 	"https://imode.net/imail/oexaf/acgi/dsaddrlist";
	private static final int MaxSendAttachment = 10;	

	private String name;
	private String pass;
	
	private DefaultHttpClient httpClient;
	private Boolean logined;
	
	private AddressBook addressBook;
	private String mailAddrCharset = "ISO-2022-jP";
	private String csvAddressBook;
	private String vcAddressBook;
	
	public ImodeNetClient(String name, String pass){
		this.name = name;
		this.pass = pass;
		
		this.httpClient = new DefaultHttpClient();
		this.httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
	
	}
	
	public void checkAddressBook(){
		if(this.addressBook==null){
			// 起動直後は必ず読み込む
			this.loadAddressBook();

		}else{
			Date loaded = this.addressBook.getCreated();
			long diff = System.currentTimeMillis() - loaded.getTime();
			if(diff> 1000*60*60*24){	// 1日おきに読み込む
				this.loadAddressBook();
			}
		}
	}
	/**
	 * i mode.netにログインする
	 * @throws LoginException
	 */
	private void login() throws LoginException{
		log.info("# iモード.netにログイン");
		try{
			this.httpClient.getCookieStore().clear();
			HttpPost post = new HttpPost(LoginUrl);
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("HIDEURL","?WM_AK=https%3a%2f%2fimode.net%2fag&path=%2fimail%2ftop&query="));
			formparams.add(new BasicNameValuePair("LOGIN","WM_LOGIN"));
			formparams.add(new BasicNameValuePair("WM_KEY","0"));
			formparams.add(new BasicNameValuePair("MDCM_UID",this.name));
			formparams.add(new BasicNameValuePair("MDCM_PWD",this.pass));
			UrlEncodedFormEntity entity = null;
			try{
				entity = new UrlEncodedFormEntity(formparams,"UTF-8");
			}catch (Exception e) {}
			post.setHeader("User-Agent","Mozilla/4.0 (compatible;MSIE 7.0; Windows NT 6.0;)");
			post.setEntity(entity);
			try{
				HttpResponse res = this.executeHttp(post);
				if(res==null){
					this.logined = Boolean.FALSE;
					throw new IOException("Redirect Error");
				}
				if(res.getStatusLine().getStatusCode()!=200){
					this.logined = Boolean.FALSE;
					throw new IOException("http login response bad status code "+res.getStatusLine().getStatusCode());
				}
				String body = toStringBody(res);
				if(body.indexOf("<title>認証エラー")>0){
					this.logined = Boolean.FALSE;
					log.info("認証エラー");
					log.debug(body);
					this.clearCookie();
					throw new LoginException("認証エラー");
				}
			}finally{
				post.abort();
			}
			
			
			post = new HttpPost(JsonUrl+"login");
			try{
				HttpResponse res = this.requestPost(post, null);
				if(res==null){
					this.logined = Boolean.FALSE;
					throw new IOException("Login Error");
				}
				if(res.getStatusLine().getStatusCode()!=200){
					this.logined = Boolean.FALSE;
					throw new IOException("http login2 response bad status code "+res.getStatusLine().getStatusCode());
				}
				this.logined = Boolean.TRUE;
			}finally{
				post.abort();
			}
			
		}catch (Exception e) {
			this.logined = Boolean.FALSE;
			throw new LoginException("Docomo i mode.net Login Error.",e);
		}
	}
	
	/**
	 * 
	 * @param folderId 取得するメールIDの入っているメールボックスのフォルダIDを指定する(0:受信ボックス？)
	 * @return フォルダIDをキーにしてそのフォルダの中のメールIDのリストを取得するMap
	 * @throws IOException
	 * @throws LoginException
	 */
	public synchronized Map<Integer,List<String>> getMailIdList() throws IOException,LoginException{
		log.info("# メールIDリストを取得");
		HttpPost post = null;

		try{
			// nullの場合は起動直後でcookieがあるので、ログインせずにトライする
			if(this.logined!=null && this.logined==Boolean.FALSE){
				this.login();
			}			
			
			post = new HttpPost(JsonUrl+"mailidlist");
			HttpResponse res = this.requestPost(post,null);
			if(!isJson(res)){
				toStringBody(res);
				post.abort();
				this.clearCookie();
				this.logined = Boolean.FALSE;
				throw new LoginException("Response is not Json");
			}
			JSONObject json = JSONObject.fromObject(toStringBody(res));

			String result = json.getJSONObject("common").getString("result");
			if(!result.equals("PW1000")){
				log.debug(json.toString(2));
				this.clearCookie();
				this.logined = Boolean.FALSE;
				throw new LoginException("Bad response "+result);
			}
			this.logined = Boolean.TRUE;
			//log.debug(json.toString(2));
			JSONArray array = json.getJSONObject("data").getJSONArray("folderList");
			
			Map<Integer, List<String>> r = new TreeMap<Integer, List<String>>();
			for(int i=0; i<array.size(); i++){
				json = array.getJSONObject(i);
				int folderId = json.getInt("folderId");
				// 1:送信フォルダ, 2:下書きフォルダ
				if(folderId==1 || folderId==2){
					// スキップ
					continue;
				}
				log.debug("FolderId "+folderId);
				@SuppressWarnings("unchecked")
				Collection<String> mailIdlist = (Collection<String>)JSONArray.toCollection(json.getJSONArray("mailIdList"),String.class);
				if(mailIdlist.isEmpty()){
					// 削除済みフォルダはスキップ(folderId=0でmailIdが空)
					continue;
				}
				List<String> list = new ArrayList<String>(mailIdlist);
				Collections.sort(list);
				Collections.reverse(list);
				r.put(folderId, list);
			}

			return r;
		}finally{
			post.abort();
		}
	}
	
	public synchronized ImodeMail getMail(int folderId, String mailId) throws IOException,LoginException{
		log.info("# メール情報を取得 フォルダID:"+folderId+" / メールID:"+mailId);
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("folder.id",Integer.toString(folderId)));
		formparams.add(new BasicNameValuePair("folder.mail.id",mailId));
		
		HttpPost post = null;
		JSONObject json = null;
		try{
			post = new HttpPost(JsonUrl+"maildetail");
			HttpResponse res = this.requestPost(post,formparams);
			json = JSONObject.fromObject(toStringBody(res));
			
			String result = json.getJSONObject("common").getString("result");
			if(!result.equals("PW1000")){
				log.debug(json.toString(2));
				this.clearCookie();
				this.logined = Boolean.FALSE;
				throw new LoginException("Bad response "+result);
			}
		}finally{
			post.abort();
		}
		//log.debug(json.toString(2));
		
		ImodeMail r = new ImodeMail();
		String myAddr = json.getJSONObject("common").getString("myAddr");
		r.setMyAddr(myAddr);
		json = json.getJSONObject("data").getJSONObject("previewMail");
		r.setFolderId(folderId);
		r.setMailId(mailId);
		r.setSubject(json.getString("subject"));
		r.setTime(json.getString("time"));
		r.setDecomeFlg(json.getInt("decomeFlg")!=0);
		r.setRecvType(json.getInt("recvType"));
		r.setBody(json.getString("body"));
		
		log.info("From     "+r.getFromAddr());
		log.info("Subject  "+r.getSubject());
		log.info("Time     "+r.getTime());
		log.info("DecoFlag "+r.isDecomeFlg());
		log.info("RcvType  "+r.getRecvType());
		
		// メールアドレス
		JSONArray addrs = json.getJSONArray("previewInfo");
		List<InternetAddress> tolist = new ArrayList<InternetAddress>();
		List<InternetAddress> cclist = new ArrayList<InternetAddress>();
		for(int i=0; i<addrs.size(); i++){
			JSONObject addrJson = addrs.getJSONObject(i);
			int type = addrJson.getInt("type");
			String addr = addrJson.getString("mladdr");
			if(type==0){
				r.setFromAddr(this.addressBook.getInternetAddress(addr,this.mailAddrCharset));
				log.info("From "+addr);
			}else if(type==1){
				tolist.add(this.addressBook.getInternetAddress(addr,this.mailAddrCharset));
				log.info("To   "+addr);
			}else if(type==2){
				cclist.add(this.addressBook.getInternetAddress(addr,this.mailAddrCharset));
				log.info("Cc   "+addr);
			}
		}
		r.setToAddrList(tolist);
		r.setCcAddrList(cclist);
		
		// 添付ファイル
		List<AttachedFile> attache = new ArrayList<AttachedFile>();
		List<AttachedFile> inline = new ArrayList<AttachedFile>();
		JSONArray attaches = json.getJSONArray("attachmentFile");
		log.info("添付ファイル "+attaches.size());
		for(int i=0; i<attaches.size(); i++){
			JSONObject attacheJson = attaches.getJSONArray(i).getJSONObject(0);
			AttachedFile f = this.getAttachedFile(AttachType.Attach, folderId, mailId, attacheJson.getString("id"));
			attache.add(f);
		}
		
		attaches = json.getJSONArray("inlineInfo");
		log.info("添付ファイル(インライン) "+attaches.size());
		for(int i=0; i<attaches.size(); i++){
			JSONObject inlineJson = attaches.getJSONObject(i);
			AttachedFile f = this.getAttachedFile(AttachType.Inline, folderId, mailId, inlineJson.getString("id"));
			inline.add(f);
		}
		
		r.setAttachFileList(attache);
		r.setInlineFileList(inline);
		return r;
	}
	
	private enum AttachType {Attach,Inline};
	/**
	 * 添付ファイルをダウンロードする
	 * 
	 * @param mailId
	 * @param attacheId
	 * @return
	 */
	private AttachedFile getAttachedFile(AttachType type, int folderId, String mailId, String fileId) throws IOException{
		log.debug("# 添付ファイルのダウンロード "+type+"/"+mailId+"/"+fileId);
		
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("folder.id",Integer.toString(folderId)));
		formparams.add(new BasicNameValuePair("folder.mail.id",mailId));
		if(type==AttachType.Attach){
			formparams.add(new BasicNameValuePair("folder.attach.id", fileId));
		}else{
			formparams.add(new BasicNameValuePair("folder.mail.img.id", fileId));
		}
		formparams.add(new BasicNameValuePair("cdflg","1"));
		
		String pwsp = this.getPwspQuery();
		
		HttpPost post = null;
		try{
			if(type==AttachType.Attach){
				post = new HttpPost(AttachedFileUrl+"?pwsp="+pwsp);
			}else{
				post = new HttpPost(InlineFileUrl+"?pwsp="+pwsp);
			}
			HttpResponse res = this.requestPost(post, formparams);

			AttachedFile file = new AttachedFile();
			file.setFolderId(folderId);
			file.setMailId(mailId);
			file.setId(fileId);
			Header h = res.getFirstHeader("Content-Disposition");
			HeaderElement[] hes = h.getElements();
			for (HeaderElement he : hes) {
				NameValuePair nvp = he.getParameterByName("filename");
				if(nvp!=null){
					file.setFilename(URLDecoder.decode(nvp.getValue(),"UTF-8"));
				}
			}

			HttpEntity entity = res.getEntity();
			file.setContentType(entity.getContentType().getValue());
			file.setData(EntityUtils.toByteArray(entity));
			log.info("ファイル名   "+file.getFilename());
			log.info("Content-type "+file.getContentType());
			log.info("サイズ       "+file.getData().length);
			return file;
		}finally{
			post.abort();
		}
	}
	
	private String getPwspQuery(){
		String pwsp = "";
		for (Cookie c : this.httpClient.getCookieStore().getCookies()) {
			if(c.getName().equalsIgnoreCase("pwsp")){
				String val = c.getValue();
				pwsp = val.substring(val.length()-32);
			}
		}
		return pwsp;
	}
	
	/**
	 * imode.netの送信フォールからメールを送信
	 * 
	 * @param mail
	 * @return
	 * @throws IOException
	 */
	public synchronized void sendMail(SenderMail mail, boolean forcePlaintext) throws IOException{
		if(this.logined==null || !this.logined){
			log.warn("iモード.netにログインできていません。");
			throw new IOException("imode.net nologin");
		}
		List<String> inlineFileIdList = new LinkedList<String>();
		List<String> attachmentFileIdList = new LinkedList<String>();
		if(!forcePlaintext){
			// htmlメールの場合インライン画像を送信する
			for (SenderAttachment file : mail.getInlineFile()) {
				String docomoFileId = this.sendAttachFile(
						inlineFileIdList, 
						true, 
						file.getContentTypeWithoutParameter(), 
						file.getFilename(), 
						file.getData());
				file.setDocomoFileId(docomoFileId);
			}
		}
		List<SenderAttachment> attachFiles = mail.getAttachmentFile();
		if(attachFiles.size()>MaxSendAttachment){
			log.warn("添付ファイルの数は最大"+MaxSendAttachment+"個");
			throw new IOException("Too mush attachment file. Max "+MaxSendAttachment);
		}
		for (SenderAttachment file : attachFiles) {
			// 添付ファイルを送信
			this.sendAttachFile(
					attachmentFileIdList, 
					false, 
					file.getContentTypeWithoutParameter(), 
					file.getFilename(), 
					file.getData());
		}
		
		// htmlメールかテキストメール決定する
		boolean htmlMail =false;
		String body = null;
		if(forcePlaintext){
			htmlMail = false;
			body = mail.getPlainBody();
		}else{
			body = mail.getHtmlBody(true);
			if(body==null){
				htmlMail = false;
				body = mail.getPlainBody();
			}else{
				htmlMail = true;
			}
		}
		log.info("Html "+htmlMail);
		log.info("body "+body);
		
		
		MultipartEntity multi = new MultipartEntity();
		try{
			multi.addPart("folder.id", new StringBody("0",Charset.forName("UTF-8")));
			String mailType=null;
			if(htmlMail){
				mailType = "1";
			}else{
				mailType = "0";
			}
			multi.addPart("folder.mail.type", new StringBody(mailType,Charset.forName("UTF-8")));
			
			// 送信先
			int recipient = 0;
			for (InternetAddress ia : mail.getTo()) {
				multi.addPart("folder.mail.addrinfo("+recipient+").mladdr", new StringBody(ia.getAddress(),Charset.forName("UTF-8")));
				multi.addPart("folder.mail.addrinfo("+recipient+").type", new StringBody("1",Charset.forName("UTF-8")));
				recipient++;
			}
			for (InternetAddress ia : mail.getCc()) {
				multi.addPart("folder.mail.addrinfo("+recipient+").mladdr", new StringBody(ia.getAddress(),Charset.forName("UTF-8")));
				multi.addPart("folder.mail.addrinfo("+recipient+").type", new StringBody("2",Charset.forName("UTF-8")));
				recipient++;
			}
			for (InternetAddress ia : mail.getBcc()) {
				multi.addPart("folder.mail.addrinfo("+recipient+").mladdr", new StringBody(ia.getAddress(),Charset.forName("UTF-8")));
				multi.addPart("folder.mail.addrinfo("+recipient+").type", new StringBody("3",Charset.forName("UTF-8")));
				recipient++;
			}
			if(recipient>=5){
				throw new IOException("Too Much Recipient");
			}
			
			// 題名
			multi.addPart("folder.mail.subject", new StringBody(Util.reverseReplaceUnicodeMapping(mail.getSubject()),Charset.forName("UTF-8")));
			
			// 内容
			body = Util.reverseReplaceUnicodeMapping(body);
			
			// imode.netの制限 本文は10000Bytes以内で
			if(body.getBytes().length>10000){
				// 文字数かbyte数かわからないのでとりあえずbyte数で制限
				log.warn("本文のサイズが大きすぎます。最大10000byte");
				throw new IOException("Too Big Message Body. Max 10000 byte.");
			}
			multi.addPart("folder.mail.data", new StringBody(body,Charset.forName("UTF-8")));
			
			if(!attachmentFileIdList.isEmpty()){
				// 添付ファイル
				for(int i=0; i<attachmentFileIdList.size();i++){
					multi.addPart("folder.tmpfile("+i+").file(0).id", new StringBody(attachmentFileIdList.get(i),Charset.forName("UTF-8")));
				}
			}

			// よくわかんない絵文字情報
			multi.addPart("iemoji(0).id", new StringBody(Character.toString((char)0xe709),Charset.forName("UTF-8")));			// e709 = UTF-8でee89c9の値
			multi.addPart("iemoji(1).id", new StringBody(Character.toString((char)0xe6f0),Charset.forName("UTF-8")));			// e6f0 = UTF-8でee9bb0の値
			
			multi.addPart("reqtype", new StringBody("0",Charset.forName("UTF-8")));

			HttpPost post = new HttpPost(SendMailUrl);
			try{
				addDumyHeader(post);
				post.setEntity(multi);

				HttpResponse res = this.executeHttp(post);
				if(!isJson(res)){
					log.warn("応答がJSON形式ではありません。");
					log.debug(toStringBody(res));
					throw new IOException("Bad response. no json format.");
				}
				JSONObject json = JSONObject.fromObject(toStringBody(res));
				String result = json.getJSONObject("common").getString("result");
				if(!result.equals("PW1000")){
					log.debug(json.toString(2));
					throw new IOException("Bad response "+result);
				}
			}finally{
				post.abort();
			}
		}catch (UnsupportedEncodingException e) {
			log.fatal(e);
		}
	}
	
	/**
	 * 送信するメールの添付ファイルを送信
	 * 
	 * @param fileIdList
	 * @param contentType
	 * @param filename
	 * @param data
	 */
	private synchronized String sendAttachFile(List<String> fileIdList, boolean isInline, String contentType, String filename, byte[] data) throws IOException{
		MultipartEntity multi = new MultipartEntity();
		int i=0;
		for (Iterator<String> iterator = fileIdList.iterator(); iterator.hasNext();i++) {
			String fileId = (String) iterator.next();
			try{
				multi.addPart("tmpfile("+i+").id", new StringBody(fileId,Charset.forName("UTF-8")));
			}catch (Exception e) {
				log.error("sendAttachFile ("+i+")",e);
			}
		}

		multi.addPart("stmpfile.data", new ByteArrayBody(data,contentType,"C:\\"+filename));
		
		String url = null;
		if(isInline){
			url = ImgupUrl;
		}else{
			url = FileupUrl;
		}
			
		HttpPost post = new HttpPost(url+"?pwsp="+this.getPwspQuery());
		try{
			addDumyHeader(post);
			post.setEntity(multi);

			HttpResponse res = this.executeHttp(post);
			if(res.getStatusLine().getStatusCode()!=200){
				log.warn("attachefile error. "+filename+"/"+res.getStatusLine().getStatusCode()+"/"+res.getStatusLine().getReasonPhrase());
				throw new IOException(filename+" error. "+res.getStatusLine().getStatusCode()+"/"+res.getStatusLine().getReasonPhrase());
			}
			if(!isJson(res)){
				log.warn("Fileuploadの応答がJSON形式ではありません。");
				log.debug(toStringBody(res));
				throw new IOException("Bad attached file");
			}
			JSONObject json = JSONObject.fromObject(toStringBody(res));
			String result = json.getJSONObject("common").getString("result");
			if(!result.equals("PW1000")){
				log.debug(json.toString(2));
				throw new IOException("Bad fileupload["+filename+"] response "+result);
			}
			// 応答のファイルIDをリストに追加
			String objName = null;
			if(isInline){
				objName = "listPcimg";
			}else{
				objName = "file";
			}
			
			String fileId = json.getJSONObject("data").getJSONObject(objName).getString("id");
			fileIdList.add(fileId);
			return fileId;
		}finally{
			post.abort();
		}
	}
	
	
	/*
	 * アドレス帳情報を読み込む
	 */
	private void loadAddressBook(){
		AddressBook ab = new AddressBook();
		try{
			this.loadPcAddressBook(ab);
		}catch (Exception e) {
			log.warn("iモード.netのアドレス帳情報が読み込めませんでした。");
		}
		try{
			this.loadDsAddressBook(ab);
		}catch (Exception e) {
			log.warn("ケータイデータお預かりサービスのアドレス帳情報が読み込めませんでした。");
		}
		try{
			this.loadCsvAddressBook(ab);
		}catch (Exception e) {
			log.warn("CSVのアドレス帳情報が読み込めませんでした。");
		}
		try{
			this.loadVcAddressBook(ab);
		}catch (Exception e) {
			log.warn("vCardのアドレス帳情報が読み込めませんでした。");
		}

		this.addressBook = ab;
	}
	/*
	 * iモード.net上で登録したアドレス帳情報を読み込む
	 */
	private void loadPcAddressBook(AddressBook ab) throws IOException{
		log.info("# iモード.netのアドレス帳情報を読み込みます。");
		HttpPost post = new HttpPost(PcAddrListUrl);
		JSONObject json = null;
		try{
			HttpResponse res = this.requestPost(post, null);
			if(res==null){
				this.logined = Boolean.FALSE;
				throw new IOException("Login Error");
			}
			json = JSONObject.fromObject(toStringBody(res));
			
			String result = json.getJSONObject("common").getString("result");
			if(!result.equals("PW1000")){
				log.debug(json.toString(2));
				throw new IOException("Bad response "+result);
			}
		}finally{
			post.abort();
		}
		
		JSONObject nameTbl = json.getJSONObject("data").getJSONObject("pcNameTbl");
		@SuppressWarnings("unchecked")
		Iterator<Object> ite = nameTbl.keys();
		while(ite.hasNext()){
			try{
				String addr = (String)ite.next();
				JSONObject jsonMail = null;
				Object o = nameTbl.get(addr);
				if(o instanceof JSONObject){
					jsonMail = nameTbl.getJSONObject(addr);
				}else if(o instanceof JSONArray){
					// 1つのメールアドレスが複数登録されている場合は最初の
					jsonMail = nameTbl.getJSONArray(addr).getJSONObject(0);
				}else{
					log.warn("unknown Type "+o.getClass()+"/"+o);
					continue;
				}
				ImodeAddress ia = new ImodeAddress();
				ia.setMailAddress(addr);
				ia.setName(jsonMail.getString("value"));
				ia.setId(jsonMail.getString("id"));
				ab.addPcAddr(ia);
				log.debug("ID:"+ia.getId()+" / Name:"+ia.getName()+" / Address:"+ia.getMailAddress());
			}catch (Exception e) {
				log.warn("loadPcAddressBook json error.",e);
			}
		}
	}
	
	/*
	 * ケータイデータお預かりサービスで登録したアドレス帳情報を読み込む
	 */
	private void loadDsAddressBook(AddressBook ab) throws IOException{
		log.info("# ケータイデータお預かりサービスで登録したアドレス帳情報を読み込みます。");
		HttpPost post = new HttpPost(DsAddrListUrl);
		JSONObject json = null;
		try{
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("lastupdate",""));
			
			HttpResponse res = this.requestPost(post, formparams);
			if(res==null){
				this.logined = Boolean.FALSE;
				throw new IOException("Login Error");
			}
			json = JSONObject.fromObject(toStringBody(res));
			
			String result = json.getJSONObject("common").getString("result");
			if(!result.equals("PW1000")){
				log.debug(json.toString(2));
				throw new IOException("Bad response "+result);
			}
		}finally{
			post.abort();
		}
		
		JSONObject nameTbl = json.getJSONObject("data").getJSONObject("dsNameTbl");

		@SuppressWarnings("unchecked")
		Iterator<Object> ite = nameTbl.keys();
		while(ite.hasNext()){
			try{
				String addr = (String)ite.next();
				Object o = nameTbl.get(addr);
				JSONObject jsonMail = null;
				if(o instanceof JSONObject){
					jsonMail = nameTbl.getJSONObject(addr);
				}else if(o instanceof JSONArray){
					// 1つのメールアドレスが複数登録されている場合は最初の
					jsonMail = nameTbl.getJSONArray(addr).getJSONObject(0);
				}else{
					log.warn("unknown Type "+o.getClass()+"/"+o);
					continue;
				}
				ImodeAddress ia = new ImodeAddress();
				ia.setMailAddress(addr);
				ia.setName(jsonMail.getString("value"));
				ia.setId(jsonMail.getString("id"));
				ab.addPcAddr(ia);
				log.debug("ID:"+ia.getId()+" / Name:"+ia.getName()+" / Address:"+ia.getMailAddress());
			}catch (Exception e) {
				log.warn("loadDsAddressBook json error.",e);
			}
		}
	}
	/*
	 * CSVのアドレス帳情報を読み込む
	 */
	private void loadCsvAddressBook(AddressBook ab) throws IOException{
		if(this.csvAddressBook==null){
			return;
		}
		File csvFile = new File(this.csvAddressBook);
		if(!csvFile.exists()){
			log.info("# CSVアドレス帳ファイル("+this.csvAddressBook+")は存在しません。");
			return;
		}
		log.info("# CSVアドレス帳情報を読み込みます。");
		BufferedReader br = null;
		FileReader fr = null;
		try{
			// デフォルトエンコードで読み込まれる
			// wrapper.confで-Dfile.encoding=UTF-8を指定しているのでUTF-8になる
			fr = new FileReader(csvFile);
			br = new BufferedReader(fr);
			int id = 0;

			String line = null;
			while((line = br.readLine()) != null){
				// フォーマット:
				// メールアドレス,ディスプレイネーム
				id++;
				try{
					String[] field = line.split(",");
					if(field.length < 2){
						continue;
					}
					InternetAddress[] addrs = InternetAddress.parse(field[0]);
					if(addrs.length == 0)
						continue;
					ImodeAddress ia = new ImodeAddress();
					ia.setMailAddress(addrs[0].getAddress());
					ia.setName(field[1]);
					ia.setId(String.valueOf(id));
					ab.addCsvAddr(ia);
					log.debug("ID:"+ia.getId()+" / Name:"+ia.getName()+" / Address:"+ia.getMailAddress());
					
				}catch (Exception e) {
					log.warn("CSVファイル("+id+"行目)に問題があります["+line+"]");
				}
			}
			br.close();
		}catch (Exception e){
			log.warn("loadCsvAddressBook "+this.csvAddressBook+" error.",e);
			
		}finally{
			Util.safeclose(br);
			Util.safeclose(fr);
		}
	}

	/*
	 * vCardのアドレス帳情報を読み込む
	 */
	private void loadVcAddressBook(AddressBook ab) throws IOException{
		if(this.vcAddressBook==null){
			return;
		}
		File vcFile = new File(this.vcAddressBook);
		if(!vcFile.exists()){
			log.info("# vCardアドレス帳ファイル("+this.vcAddressBook+")は存在しません。");
			return;
		}
		log.info("# vCardアドレス帳情報を読み込みます。");
		FileInputStream fis = null;
		byte[] vcData = null;
		try{
			fis = new FileInputStream(vcFile);
			vcData = new byte[(int)vcFile.length()];
			fis.read(vcData);
		}catch (Exception e){
			log.warn("loadVcAddressBook "+this.vcAddressBook+" error.",e);
		}finally{
			Util.safeclose(fis);
		}

		int id = 0;
		boolean vcBegin = false;
		String vcName = null;
		String vcEmail = null;

		int lineStart = 0;
		int lineLength = 0;

		for(int i=lineStart; i<=vcFile.length(); i++){
			try{
				if(i == vcFile.length() || vcData[i] == '\n'){
					String line = new String(vcData, lineStart, lineLength);
					int curLineStart = lineStart;
					int curLineLength = lineLength;

					lineStart = i+1;
					lineLength = 0;

					String field[] = line.split(":");
					if(field[0].equalsIgnoreCase("BEGIN")){
						vcBegin = true;
						vcName = null;
						vcEmail = null;
						id++;
					}
					if(vcBegin == true && field[0].equalsIgnoreCase("END")){
						vcBegin = false;

						if (vcName == null || vcEmail == null)
							continue;

						String vcEmails[] = vcEmail.split(";");
						for(int j=0; j<vcEmails.length; j++){
							InternetAddress[] addrs = InternetAddress.parse(vcEmails[j]);
							if(addrs.length == 0)
								continue;
							ImodeAddress ia = new ImodeAddress();
							ia.setMailAddress(addrs[0].getAddress());
							ia.setName(vcName);
							ia.setId(String.valueOf(id+"-"+(j+1)));
							ab.addVcAddr(ia);
							log.debug("ID:"+ia.getId()+" / Name:"+ia.getName()+" / Address:"+ia.getMailAddress());
						}
					}

					if(vcBegin != true || field.length < 2)
						continue;

					String label[] = field[0].split(";");
					String value[] = field[1].split(";");
					// 姓名
					if(label[0].equalsIgnoreCase("FN")){
						vcName = field[1].replace(";"," ").trim();
						if(label.length < 2)
							continue;
						String option[] = label[1].split("=");
						if(option.length < 1 || !option[0].equalsIgnoreCase("CHARSET"))
							continue;
						int valueStart = curLineStart;
						for(int pos=curLineStart; pos<curLineStart+curLineLength; pos++){
							if(vcData[pos] == ':'){
								valueStart = pos+1;
								break;
							}
						}
						vcName = new String(vcData, valueStart, curLineLength-(valueStart-curLineStart), option[1]).replace(";"," ").trim();
					
					}
					// EMAIL
					if(label[0].equalsIgnoreCase("EMAIL")){
						if(vcEmail == null)
							vcEmail = value[0];
						else
							vcEmail = vcEmail + ';' + value[0];
					}

				}else if(vcData[i] != '\r'){
					lineLength++;
				}
			}catch (Exception e) {
				log.warn("vCardファイル("+id+"件目)に問題があります");
			}
		}
	}


	private static boolean isJson(HttpResponse res){
		if(res==null){
			return false;
		}
		Header h = res.getFirstHeader("Content-type");
		String type = h.getValue().toLowerCase();
		if(type.indexOf("json")>=0 || type.indexOf("text/plain")>=0){
			return true;
		}else{
			return false;
		}
	}
	
	private static String toStringBody(HttpResponse res) throws IOException{
		return EntityUtils.toString(res.getEntity(),"Shift_JIS");
	}
	
	/**
	 * POSTは自動でリダイレクトされないので手動でリダイレクト
	 * @param req
	 * @return
	 * @throws IOException
	 */
	private HttpResponse executeHttp(HttpRequestBase req) throws IOException{
		try{
			for(int i=0; i<4; i++){
				HttpResponse res = this.httpClient.execute(req);
				int status = res.getStatusLine().getStatusCode();
				if(300<=status && status<=399){
					req.abort();
					
					URI location = httpClient.getRedirectHandler().getLocationURI(res, new BasicHttpContext());
					
					//System.out.println("Redirect "+location);
					req = new HttpGet(location);
					req.setHeader("User-Agent","Mozilla/4.0 (compatible;MSIE 7.0; Windows NT 6.0;)");
				}else{
					return res;
				}
			}
		}catch (Exception e) {e.printStackTrace();}
		return null;
	}

	/*
	 * IEがAjaxで行うリクエストを行う
	 */
	private HttpResponse requestPost(HttpPost post,List<NameValuePair> formparams) throws IOException{
		addDumyHeader(post);
		
		if(formparams!=null){
			UrlEncodedFormEntity entity = null;
			try{
				entity = new UrlEncodedFormEntity(formparams,"UTF-8");
			}catch (Exception e) {}
			post.setEntity(entity);
		}else{
			post.setHeader("Content-Type","application/x-www-form-urlencoded");
		}
		
		return this.executeHttp(post);
	}
	
	private static void addDumyHeader(HttpEntityEnclosingRequestBase req){
		req.setHeader("Accept", "*/*");
		req.setHeader("Accept-Encoding","gzip, deflate");
		req.setHeader("Cache-Control", "no-cache");
		req.setHeader("User-Agent","Mozilla/4.0 (compatible;MSIE 7.0; Windows NT 6.0;)");
		req.setHeader("x-pw-service","PCMAIL/1.0");
		req.setHeader("Referer","https://imode.net/imail/oexaf/ahtm/index_f.html");
	}
	
	public void setSoTimeout(int millisec){
		this.httpClient.getParams().setIntParameter("http.socket.timeout", millisec);
	}
	
	public void setConnTimeout(int millisec){
		this.httpClient.getParams().setIntParameter("http.connection.timeout", millisec);
	}
	
	public void setMailAddrCharset(String str){
		this.mailAddrCharset = str;
	}
	
	public void setCsvAddressBook(String filename){
		this.csvAddressBook = filename;
	}
	
	public void setVcAddressBook(String filename){
		this.vcAddressBook = filename;
	}
	
	public void clearCookie(){
		this.httpClient.getCookieStore().clear();
	}
	
	/**
	 * クッキーを取得
	 * @return
	 */
	public List<Cookie> getCookies(){
		return this.httpClient.getCookieStore().getCookies();
	}
	
	/**
	 * クッキーを追加する。
	 * 前のログインから時間が経過していなければ、以前使用したクッキーを再設定してログインが省略できる。
	 * @param cookie
	 */
	public void addCookie(Cookie cookie){
		this.httpClient.getCookieStore().addCookie(cookie);
	}
	
	public void close(){
		try{
			this.httpClient.getConnectionManager().shutdown();
		}catch (Exception e) {}
	}
}
