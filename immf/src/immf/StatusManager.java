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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

public class StatusManager {
	private static final Log log = LogFactory.getLog(StatusManager.class);
	private File f;
	
	private List<Cookie> cookies;
	private String lastMailId;
	
	public StatusManager(File f){
		this.f = f;
		this.cookies = new ArrayList<Cookie>();
	}
	
	public void load() throws IOException{
		Properties prop = new Properties();
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(this.f);
			prop.load(fis);
			this.lastMailId = prop.getProperty("lastmailid");
			Enumeration<Object> enu = prop.keys();
			List<Cookie> list = new ArrayList<Cookie>();
			while(enu.hasMoreElements()){
				String key = (String)enu.nextElement();
				
				if(key.startsWith("cookie_")){
					String cookieName = key.substring(7);
					BasicClientCookie c = new BasicClientCookie(cookieName,prop.getProperty(key));
					c.setDomain("imode.net");
					c.setPath("/imail/");
					c.setSecure(true);
					log.debug("Load Cookie ["+c.getName()+"]=["+c.getValue()+"]");
					list.add(c);
				}
			}
			this.cookies = list;
		}finally{
			Util.safeclose(fis);
		}
	}
	public String getLastMailId(){
		return lastMailId;
	}
	public void setLastMailId(String s){
		this.lastMailId = s;
	}
	public List<Cookie> getCookies(){
		return new ArrayList<Cookie>(this.cookies);
	}
	public void setCookies(List<Cookie> cookies){
		this.cookies = new ArrayList<Cookie>(cookies);
	}
	
	public void save() throws IOException{
		Properties prop = new Properties();
		for (Cookie cookie : this.cookies) {
			prop.setProperty("cookie_"+cookie.getName(), cookie.getValue());
		}
		if(this.lastMailId!=null){
			prop.setProperty("lastmailid", this.lastMailId);
		}
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(this.f);
			prop.store(fos, "IMMF cookie info.");
		}finally{
			Util.safeclose(fos);
		}
	}
}
