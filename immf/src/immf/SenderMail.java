package immf;

import java.util.List;

import javax.mail.internet.InternetAddress;

public class SenderMail {
	private List<InternetAddress> to;
	private List<InternetAddress> cc;
	private List<InternetAddress> bcc;
	private String subject;
	private String content;
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
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	
}
