package immf;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import net.htmlparser.jericho.Source;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SenderMail {
	private static final Log log = LogFactory.getLog(SenderMail.class);
	private List<InternetAddress> to;
	private List<InternetAddress> cc;
	private List<InternetAddress> bcc;
	private String subject;
	private String plainTextContent;
	private String htmlContent;
	private String htmlWorkingContent;
	private List<SenderAttachment> attachments = new ArrayList<SenderAttachment>();
	
	/*
	 * 添付ファイル：10個、合計2MBまで
	 * デコメ絵文字／デコメピクチャ：最大20種類、合計90KBまで
	 */
	private static final int MaxAttachmentCount = 10;	
	private static final int MaxAttachmentSize = 2000000;
	private static final int MaxInlineAttachmentCount = 20;
	private static final int MaxInlineAttachmentSize = 90000;
	
	public List<InternetAddress> getTo() {
		return to;
	}
	public void setTo(List<InternetAddress> to) {
		this.to = to;
	}
	public List<InternetAddress> getCc() {
		return cc;
	}
	public void setCc(List<InternetAddress> cc) {
		this.cc = cc;
	}
	public List<InternetAddress> getBcc() {
		return bcc;
	}
	public void setBcc(List<InternetAddress> bcc) {
		this.bcc = bcc;
	}
	public String getSubject() {
		if(this.subject==null){
			return "";
		}
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}


	public String getPlainTextContent() {
		return plainTextContent;
	}
	public void setPlainTextContent(String plainTextContent) {
		this.plainTextContent = plainTextContent;
	}
	public void addPlainTextContent(String plainTextContent) {
		this.plainTextContent += plainTextContent;
	}
	public String getHtmlContent() {
		return htmlContent;
	}
	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}
	public void addHtmlContent(String htmlContent) {
		this.htmlContent += htmlContent;
	}
	public String getHtmlWorkingContent() {
		return htmlWorkingContent;
	}
	public void setHtmlWorkingContent(String htmlContent) {
		this.htmlWorkingContent = htmlContent;
	}
	public void addHtmlWorkingContent(String htmlContent) {
		this.htmlWorkingContent += htmlContent;
	}
	public void addAttachmentFileIdList(SenderAttachment file) {
		for (SenderAttachment f : this.attachments) {
			if(!f.isInline()){
				continue;
			}
			if(f.getHash().equals(file.getHash())){
				// すでに同じ内容のファイルが添付予定であれば重複して登録しない
				htmlContent = StringUtils.replace(htmlContent, file.getContentIdWithoutBracket(), f.getContentIdWithoutBracket());
				return;
			}
		}
		this.attachments.add(file);
	}
	public boolean checkAttachmentCapability(SenderAttachment file) {
		boolean inline = file.isInline();
		int count = 1;
		int size = file.getData().length;
		if(inline){
			for (SenderAttachment f : this.attachments){
				if(!f.isInline()){
					continue;
				}
				if(f.getHash().equals(file.getHash())){
					return true;
				}
			}
			for (SenderAttachment f : this.attachments){
				if(f.isInline()){
					count++;
					size += f.getData().length;
				}
			}
			if(count > MaxInlineAttachmentCount){
				return false;
			}
			if(size > MaxInlineAttachmentSize){
				return false;
			}
		}else{
			for (SenderAttachment f : this.attachments){
				if(!f.isInline()){
					count++;
					size += f.getData().length;
				}
			}
			if(count > MaxAttachmentCount){
				return false;
			}
			if(size > MaxAttachmentSize){
				return false;
			}
		}
		return true;
	}
	
	/*
	 * PlainText形式で本文を取得
	 */
	public String getPlainBody(){
		if(this.plainTextContent!=null){
			return this.plainTextContent;
		}
		if(this.htmlContent==null){
			// 本文が無い
			return "";
		}
		// htmlをテキストに変換
		Source src = new Source(this.htmlContent);
		return src.getRenderer().toString();
	}
	
	public String getHtmlBody(boolean replaceCid){
		String r = this.htmlContent;
		if(r==null){
			return null;
		}
		if(replaceCid==false){
			return r;
		}
		List<SenderAttachment> inlines = getInlineFile();
		for (SenderAttachment file : inlines) {
			// 「40_」がつく
			//System.err.println("\"cid:"+file.getContentIdWithoutBracket()+"\"");
			r = StringUtils.replace(r, "\"cid:"+file.getContentIdWithoutBracket()+"\"", "\"40_"+file.getDocomoFileId()+"\"");
		}
		
		r = HtmlConvert.toDecomeHtml(r);
		log.info(r);
		return r;
		//return "<body><font size=\"4\"><font color=\"#333333#\">ABCZZZZ</font></font></body>";
	}

	
	/*
	 * インライン添付ファイルを取得
	 */
	public List<SenderAttachment> getInlineFile(){
		List<SenderAttachment> r = new ArrayList<SenderAttachment>();
		if(this.htmlContent==null){
			return r;
		}

		for (SenderAttachment file : this.attachments) {
			if(!file.isInline()){
				continue;
			}
			// 本当にインラインで参照されているものだけ
			if(this.htmlContent.indexOf("\"cid:"+file.getContentIdWithoutBracket()+"\"")>=0){
				r.add(file);
			}else{
				log.debug("not inline "+file.loggingString());
			}
		}
		return r;
	}
	
	/*
	 * インライン以外の添付ファイルを取得
	 */
	public List<SenderAttachment> getAttachmentFile(){
		List<SenderAttachment> r = new ArrayList<SenderAttachment>();
		r.addAll(this.attachments);
		List<SenderAttachment> inline = getInlineFile();
		r.removeAll(inline);
		return r;
	}
	
}
