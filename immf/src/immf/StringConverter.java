/*
 * imoten - i mode.net mail tensou(forward)
 *
 * Copyright (C) 2011 ryu aka 508.P905 (http://code.google.com/p/imoten/)
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
// CharacterConverter.javaをベースに作成

package immf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringConverter {
	private static final Log log = LogFactory.getLog(StringConverter.class);
	private Map<String,String> replaceMap;
	
	public StringConverter(){
		this.replaceMap = new HashMap<String, String>();			
	}
	
	public void load(File f) throws IOException{
		FileReader fr = null;
		BufferedReader br = null;
		try{
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line=null;
			int lineCount=0;
			int keys=0;
			while((line=br.readLine())!=null){
				lineCount++;
				if(line.startsWith("#") || StringUtils.isBlank(line)){
					// [#]で始まる行はコメント
					continue;
				}
				//if(line.toLowerCase().startsWith("http")){
					//自由度を持たせるためURLに限らず任意の文字列変換を許容する。但し、ASCII文字以外は未テスト。
					String[] vals = line.split("\\s",2);
					if(vals.length!=2){
						log.warn("文字列変換表 "+f.getName()+"("+lineCount+"行目)が不完全です。");
						continue;
					}
					String from = vals[0];
					// 前後のスペースの除外
					String to = vals[1].replaceAll("^\\s*(\\S+)\\s?.*$","$1");
					if(from!=null&&!from.isEmpty()&&to!=null&&!to.isEmpty()){
						keys++;
						this.replaceMap.put(from, to);
						log.info("StrConv From ["+from+"], to ["+to+"]");
					}else{
						log.warn("文字列変換表 "+f.getName()+"("+lineCount+"行目)が不完全です。");
					}
				//}
			}
			if(keys>replaceMap.size()){
				log.warn("文字列変換表 "+f.getName()+" に"+(keys-replaceMap.size()+"個のキー重複があります。"));
			}
		}finally{
			Util.safeclose(br);
			Util.safeclose(fr);
		}
		
	}
	
	public String convert(String str){
		if(this.replaceMap.isEmpty()){
			return str;
		}
		String s = str;
		for (Object key : this.replaceMap.keySet().toArray()){
			String from = (String)key;
			String to = this.replaceMap.get(from);
			Matcher m = Pattern.compile(from).matcher(s);
			if(m.find()){
				log.info("文字列置換を行いました ["+m.group()+"]");
				s = m.replaceAll(to);
			}
		}
		return s;
	}
	
}
