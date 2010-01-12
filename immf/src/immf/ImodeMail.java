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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImodeMail {
	private static final Log log = LogFactory.getLog(ImodeMail.class);
	
	private String mailId;
	private int folderId;
	private String subject;
	private String time;
	private boolean decomeFlg;
	private int recvType;
	private String body;
	private String myAddr;
	private String fromAddr;
	private List<String> toAddrList = new ArrayList<String>();
	private List<String> ccAddrList = new ArrayList<String>();;
	
	private List<AttachedFile> attachFileList = new ArrayList<AttachedFile>();
	private List<AttachedFile> inlineFileList = new ArrayList<AttachedFile>();

	
	public String toLoggingString(){
		StrBuilder buf = new StrBuilder();
		buf.appendln("FolderID     "+this.folderId);
		buf.appendln("MailID       "+this.mailId);
		buf.appendln("Subject      "+this.subject);
		buf.appendln("Time         "+this.mailId);
		buf.appendln("Decome       "+this.decomeFlg);
		buf.appendln("RecvType     "+this.recvType);
		buf.appendln("MyAddr       "+this.myAddr);
		for (String to : this.toAddrList) {
			buf.appendln("To           "+to);
		}
		for (String cc : this.toAddrList) {
			buf.appendln("Cc           "+cc);
		}
		for(AttachedFile f : this.attachFileList){
			buf.appendln("AttachFile ---- "+f.getFilename());
			buf.appendln("  ID            "+f.getId());
			buf.appendln("  ContentType   "+f.getContentType());
			buf.appendln("  Size          "+f.getData().length);
		}
		for(AttachedFile f : this.attachFileList){
			buf.appendln("InlineFile ---- "+f.getFilename());
			buf.appendln("  ID            "+f.getId());
			buf.appendln("  ContentType   "+f.getContentType());
			buf.appendln("  Size          "+f.getData().length);
		}
		buf.appendln("Body -----");
		buf.appendln(this.body);
		buf.appendln("----------");
		return buf.toString();
	}
	
	public String getMailId() {
		return mailId;
	}

	public void setMailId(String mailId) {
		this.mailId = mailId;
	}

	public int getFolderId() {
		return folderId;
	}

	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getTimeDate(){
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		try{
			return df.parse(this.time);
		}catch (Exception e) {
			return null;
		}
	}
	
	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public boolean isDecomeFlg() {
		return decomeFlg;
	}

	public void setDecomeFlg(boolean decomeFlg) {
		this.decomeFlg = decomeFlg;
	}

	public int getRecvType() {
		return recvType;
	}

	public void setRecvType(int recvType) {
		this.recvType = recvType;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFromAddr() {
		return fromAddr;
	}

	public void setFromAddr(String fromAddr) {
		this.fromAddr = fromAddr;
	}

	public List<AttachedFile> getAttachFileList() {
		return attachFileList;
	}

	public void setAttachFileList(List<AttachedFile> attachFileList) {
		this.attachFileList = attachFileList;
	}

	public List<String> getToAddrList() {
		return toAddrList;
	}

	public void setToAddrList(List<String> toAddrList) {
		this.toAddrList = toAddrList;
	}

	public List<String> getCcAddrList() {
		return ccAddrList;
	}

	public void setCcAddrList(List<String> ccAddrList) {
		this.ccAddrList = ccAddrList;
	}

	public List<AttachedFile> getInlineFileList() {
		return inlineFileList;
	}

	public void setInlineFileList(List<AttachedFile> inlineFileList) {
		this.inlineFileList = inlineFileList;
	}

	public String getMyAddr() {
		return myAddr;
	}

	public void setMyAddr(String myAddr) {
		this.myAddr = myAddr;
	}
	public String getMyMailAddr(){
		return this.myAddr+"@docomo.ne.jp";
	}
	
}
