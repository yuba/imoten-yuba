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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

public class ImodeForwardMail extends MyHtmlEmail {
	private static final Log log = LogFactory.getLog(ImodeForwardMail.class);
	private ImodeMail imm;
	private Config conf;

	public ImodeForwardMail(ImodeMail imm, Config conf) throws EmailException{
		this.imm = imm;
		this.conf = conf;
		
		this.setDebug(conf.isMailDebugEnable());
		this.setCharset(this.conf.getMailEncode());
		this.setContentTransferEncoding(this.conf.getContentTransferEncoding());
		
		// SMTP Server
		this.setHostName(conf.getSmtpServer());
		this.setSmtpPort(conf.getSmtpPort());
		this.setSocketConnectionTimeout(conf.getSmtpConnectTimeoutSec()*1000);
		this.setSocketTimeout(conf.getSmtpTimeoutSec()*1000);
		this.setTLS(conf.isSmtpTls());
		
		if(!StringUtils.isBlank(conf.getSmtpUser())){
			this.setAuthentication(conf.getSmtpUser(), conf.getSmtpPasswd());
		}

		if(!StringUtils.isBlank(conf.getPopServer())
				&& !StringUtils.isBlank(conf.getPopUser())){
			// POP before SMTP
			this.setPopBeforeSmtp(true, conf.getPopServer(), conf.getPopUser(), conf.getPopPasswd());
		}
		
		this.setFrom(conf.getSmtpMailAddress());

		if(!conf.getForwardReplyTo().isEmpty()){
			this.setReplyTo(conf.getForwardReplyTo());
		}
		
		String subject = conf.getSubjectAppendPrefix()+imm.getSubject();
		if(conf.isSubjectEmojiReplace()){
			this.setSubject(EmojiUtil.replaceToLabel(subject));
		}else{
			this.setSubject(subject);
		}
		
		List<String> list = conf.getForwardTo();
		for (String addr : list) {
			this.addTo(addr);
		}
		
		list = conf.getForwardCc();
		for (String addr : list) {
			this.addCc(addr);
		}
		
		list = conf.getForwardBcc();
		for (String addr : list) {
			this.addBcc(addr);
		}
		
		Config.BodyEmojiReplace emojiReplace = conf.getBodyEmojiReplace();
		if(emojiReplace==Config.BodyEmojiReplace.DontReplace){
			this.setBodyDontReplace();
		}else if(emojiReplace==Config.BodyEmojiReplace.ToInlineImage){
			this.setBodyToInlineImage();
		}else if(emojiReplace==Config.BodyEmojiReplace.ToWebLink){
			this.setBodyToWebLink();
		}else if(emojiReplace==Config.BodyEmojiReplace.ToLabel){
			this.setBodyToLabel();
		}
		// 添付ファイル
		this.attacheFile();
		this.attacheInline();
	}
	
	/*
	 * デコメールで<img src=".....">でインライン表示させるが、
	 * imode.netのhtmlでは[cid:]という部分が抜けているので付加する。
	 */
	private static String cidAddedBody(String html, List<AttachedFile> inlines){
		for (AttachedFile f : inlines) {
			html = StringUtils.replace(html, f.getId(), "cid:"+f.getId());
		}
		return html;
	}
	
	/*
	 * 絵文字の置き換えは行わない
	 */
	private void setBodyDontReplace() throws EmailException{
		this.setBodyDontReplace(this.imm.getBody(),this.imm.isDecomeFlg(),this.imm.getInlineFileList());
	}
	private void setBodyDontReplace(String str, boolean isHtml, List<AttachedFile> inlineFiles) throws EmailException{
		String html = null;
		if(isHtml){
			// htmlメール
			html = cidAddedBody(str,inlineFiles);
			if(conf.isHeaderToBody()){
				html = html.replaceAll("(<body[^>]*>)", "$1"+this.getHeaderInfo(true));
			}
		}else{
			// テキスト
			String text = str;
			if(conf.isHeaderToBody()){
				text = this.getHeaderInfo(false)+text;
			}
			html = "<body><pre>"+Util.easyEscapeHtml(text)+"</pre></body>";
		}
		html = "<html>"+html+"</html>";
		try{
			this.setHtmlMsg(html);
		}catch (Exception e) {
			throw new EmailException(e);
		}
	}
	private void setBodyToInlineImage() throws EmailException{
		String html = this.imm.getBody();
		if(!this.imm.isDecomeFlg()){
			html = "<body><pre>"+Util.easyEscapeHtml(html)+"</pre></body>";
		}
		Map<URL, String> emojiToCid = new HashMap<URL, String>();
		StringBuilder buf = new StringBuilder();
		for(char c : html.toCharArray()){
			if(!EmojiUtil.isEmoji(c)){
				buf.append(c);
				continue;
			}
			try{
				URL emojiUrl = EmojiUtil.emojiToImageUrl(c);
				if(emojiUrl==null){
					buf.append(EmojiUtil.UnknownReplace);
				}else{
					String cid = emojiToCid.get(emojiUrl);
					if(cid==null){
						cid = this.embed(emojiUrl, "emoji"+((int)c));
						emojiToCid.put(emojiUrl, cid);
					}
					buf.append("<img src=\"cid:"+cid+"\">");
				}
			}catch (Exception e) {
				log.warn("Emoji to inline image Error.",e);
				buf.append(EmojiUtil.UnknownReplace);
			}
		}
		this.setBodyDontReplace(buf.toString(),true,this.imm.getInlineFileList());
		
	}
	private void setBodyToWebLink() throws EmailException{
		String html = this.imm.getBody();
		if(!this.imm.isDecomeFlg()){
			html = "<body><pre>"+Util.easyEscapeHtml(html)+"</pre></body>";
		}
		html = EmojiUtil.replaceToWebLink(html);
		this.setBodyDontReplace(html,true,this.imm.getInlineFileList());
	}
	private void setBodyToLabel() throws EmailException{
		String body = this.imm.getBody();
		body = EmojiUtil.replaceToLabel(body);
		this.setBodyDontReplace(body,this.imm.isDecomeFlg(),this.imm.getInlineFileList());
	}
	
	/*
	 * 添付ファイルを追加する
	 */
	private void attacheFile() throws EmailException{
		try{
			List<AttachedFile> files = this.imm.getAttachFileList();
			for (AttachedFile f : files) {
				BodyPart part = createBodyPart();
				part.setDataHandler(new DataHandler(new ByteArrayDataSource(f.getData(),f.getContentType())));
				Util.setFileName(part, f.getFilename(), this.charset, null);
				part.setDisposition(BodyPart.ATTACHMENT);
				getContainer().addBodyPart(part);
			}
		}catch (Exception e) {
			throw new EmailException(e);
		}
	}
	/*
	 * 添付ファイルをInlineで追加する
	 */
	private void attacheInline() throws EmailException{
		try{
			List<AttachedFile> files = this.imm.getInlineFileList();
			for (AttachedFile f : files) {
				this.embed(new ByteArrayDataSource(f.getData(),f.getContentType()), f.getFilename(), this.charset, f.getId());
			}
		}catch (Exception e) {
			throw new EmailException(e);
		}

	}
	
	
	/*
	 * From,CCなどの情報をBodyの先頭に付加する場合の文字列
	 */
	private String getHeaderInfo(boolean isHtml){
		StringBuilder buf = new StringBuilder();
		StringBuilder header = new StringBuilder();

		header.append(" From:    ").append(this.imm.getFromAddr().toUnicodeString()).append("\r\n");
		header.append(" To:      ").append(this.imm.getMyMailAddr()).append("\r\n");
		for(InternetAddress addr : imm.getToAddrList()){
			header.append("          ").append(addr.toUnicodeString()).append("\r\n");
		}
		String prefix = " Cc:";
		for(InternetAddress addr : imm.getCcAddrList()){
			header.append(prefix+"      ").append(addr.toUnicodeString()).append("\r\n");
			prefix = "    ";
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd (EEE) HH:mm:ss");
		header.append(" Date:    ").append(df.format(this.imm.getTimeDate())).append("\r\n");
		String subject = this.imm.getSubject();
		if(conf.isSubjectEmojiReplace()){
			subject = EmojiUtil.replaceToLabel(subject);
		}
		header.append(" Subject: ").append(subject).append("\r\n");
		
		
		if(isHtml){
			buf.append("<pre>");
		}
		buf.append("----").append("\r\n");
		if(isHtml){
			buf.append(Util.easyEscapeHtml(header.toString()));
		}else{
			buf.append(header.toString());
		}
		buf.append("----").append("\r\n");
		if(isHtml){
			buf.append("</pre>");
		}
		buf.append("\r\n");
		return buf.toString();
	}
	
	@Override
	public void buildMimeMessage() throws EmailException {
		super.buildMimeMessage();
		MimeMessage msg = this.getMimeMessage();
		try{
			msg.setHeader("X-Mailer", ServerMain.Version);
			
			if(!this.conf.isRewriteAddress()){
				// もとのimodeメールの送信元送信先に置き換える
				msg.setHeader("Resent-From",this.conf.getSmtpMailAddress());
				if(!this.conf.getForwardTo().isEmpty()){
					msg.setHeader("Resent-To", StringUtils.join(this.conf.getForwardTo(), ","));
				}
				if(!this.conf.getForwardCc().isEmpty()){
					msg.setHeader("Resent-Cc", StringUtils.join(this.conf.getForwardCc(), ","));
				}
				if(!this.conf.getForwardBcc().isEmpty()){
					msg.setHeader("Resent-Bcc", StringUtils.join(this.conf.getForwardBcc(), ","));
				}
				SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z (z)",Locale.US);
				msg.setHeader("Resent-Date", df.format(new Date()));
				msg.setHeader("Date", df.format(this.imm.getTimeDate()));
				
				msg.removeHeader("To");
				msg.removeHeader("Cc");
				msg.removeHeader("Bcc");

				List<InternetAddress> list = new ArrayList<InternetAddress>();
				list.add(this.imm.getMyInternetAddress());
				list.addAll(this.imm.getToAddrList());
				msg.setHeader("To", InternetAddress.toString(list.toArray(new InternetAddress[0])));
				
				if(this.imm.getCcAddrList().size()>0){
					msg.setHeader("Cc", InternetAddress.toString(this.imm.getCcAddrList().toArray(new InternetAddress[0])));
				}
				
				msg.setFrom(this.imm.getFromAddr());
			}
			
			String subject = conf.getSubjectAppendPrefix()+imm.getSubject();
			if(conf.isSubjectEmojiReplace()){
				subject = EmojiUtil.replaceToLabel(subject);
			}
			msg.setSubject(MimeUtility.encodeText(subject,this.charset,"B"));			
			
			if(this.conf.getContentTransferEncoding()!=null){
				msg.setHeader("Content-Transfer-Encoding", this.conf.getContentTransferEncoding());
			}
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

	@Override
	public MyHtmlEmail setHtmlMsg(String html) throws EmailException {
		html = Util.replaceUnicodeMapping(html);
		html += "\n";
		try{
			html = new String(html.getBytes(this.charset));
		}catch (Exception e) {
			log.error("setHtmlMsg",e);
		}
		return super.setHtmlMsg(html);
	}

	@Override
	public Email setSubject(String subject) {
		return super.setSubject(Util.replaceUnicodeMapping(subject));
	}
	
	
}
