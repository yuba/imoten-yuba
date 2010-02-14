package immf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CharacterConverter {
	private static final Log log = LogFactory.getLog(CharacterConverter.class);
	private Map<Character,String> replaceMap;
	
	public CharacterConverter(){
		this.replaceMap = new HashMap<Character, String>();
	}
	
	public void load(File f) throws IOException{
		FileReader fr = null;
		BufferedReader br = null;
		try{
			// デフォルトエンコードで読み込まれる
			// wrapper.confで-Dfile.encoding=UTF-8を指定しているのでUTF-8になる
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line=null;
			int lineCount=0;
			while((line=br.readLine())!=null){
				lineCount++;
				if(line.startsWith("#") || StringUtils.isBlank(line)){
					// [#]で始まる行はコメント
					continue;
				}
				// Unicode,Unicodeもしくは文字列
				String[] vals = line.split(",",2);
				if(vals.length!=2){
					continue;
				}
				try{
					int from = Integer.parseInt(vals[0], 16);
					String to = null;
					if(vals[1].matches("^[0-9a-fA-F]+$")){
						// 16進数の場合はUnicode
						int unicode = Integer.parseInt(vals[1], 16);
						to = Character.toString((char)unicode);
					}else{
						to = vals[1];
					}
					log.debug("CharConv From ["+((char)from)+"("+vals[0]+")], to ["+to+"("+vals[1]+")]");
					this.replaceMap.put(Character.valueOf((char)from), to);
				}catch (Exception e) {
					log.warn("文字変換表 "+f.getName()+"("+lineCount+"行目)に問題があります。");
				}
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
		StringBuilder buf = new StringBuilder();
		char[] cs = str.toCharArray();
		for (char c : cs) {
			String s = this.replaceMap.get(c);
			if(s==null){
				// 置換なし
				buf.append(c);
			}else{
				System.err.println(c+"=>"+s);
				buf.append(s);
			}
		}
		return buf.toString();
	}
}
