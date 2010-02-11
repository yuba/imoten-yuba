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
	private List<SenderAttachment> attachments = new ArrayList<SenderAttachment>();
	
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
	public String getHtmlContent() {
		return htmlContent;
	}
	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}
	public void addAttachmentFileIdList(SenderAttachment file) {
		this.attachments.add(file);
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
