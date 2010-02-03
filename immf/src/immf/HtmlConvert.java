package immf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class HtmlConvert {
	// デコメールで使用可能なタグとプロパティ
	private static Map<String, Set<String>> availableProperties;
	static{
		availableProperties = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("bgcolor");
		availableProperties.put("body", set);

		set = new HashSet<String>();
		set.add("align");
		availableProperties.put("div", set);
	
		/* タグが入れ子になるとエラーになるので使用しない
		set = new HashSet<String>();
		set.add("color");
		set.add("size");
		availableProperties.put("font", set);
		*/
		
		set = new HashSet<String>();
		set.add("color");
		availableProperties.put("hr", set);
		
		set = new HashSet<String>();
		set.add("src");
		availableProperties.put("img", set);
		
		/*
		set = new HashSet<String>();
		set.add("behavior");
		availableProperties.put("marquee", set);
		*/
	}
	
	public static String toDecomeHtml(String s){
		// html,headerタグはimode.net側で付加されるので削除
		s = s.replaceAll("[\\r\\n]", "");	// 改行削除　したがってpreタグは未対応
		
		s = replaceAllCaseInsenstive(s,".*<html[^>]*>","");
		s = replaceAllCaseInsenstive(s,".*</head>","");
		s = replaceAllCaseInsenstive(s,"</html>.*","");

		s = replaceAllCaseInsenstive(s,"(<body[^>]*>)","$1\n");
		s = replaceAllCaseInsenstive(s,"(</body[^>]*>)","\n$1");
		
		//s = replaceAllCaseInsenstive(s,"(<body[^>]*>)","$1<div>");
		//s = replaceAllCaseInsenstive(s,"(</body[^>]*>)","</div>$1");

		s = replaceAllCaseInsenstive(s, "<br[^>]*>", "\n");
		
		s = replaceAllCaseInsenstive(s, "<[^<>]+/>", "");
		
		// ブロック要素は改行に置き換える
		// 入れ子のdivはエラーになるので、最後に各行を<div>で囲んで入れ子を防ぐ
		s = replaceAllCaseInsenstive(s, "</?p[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?h\\d[^>]*>", "\n");
//		s = replaceAllCaseInsenstive(s, "<h\\d>", "\n<font size=\"4\">");
//		s = replaceAllCaseInsenstive(s, "</h\\d>", "</font>\n");
		s = replaceAllCaseInsenstive(s, "<li[^>]*>", "\n&nbsp;&nbsp;o&nbsp;");
		s = replaceAllCaseInsenstive(s, "</li[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?ol[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?ul[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?dir[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?menu[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?pre[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?dl[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?backquote[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?tr[^>]*>", "\n");
		s = replaceAllCaseInsenstive(s, "</?div[^>]*>", "\n");
		
		
		
		
		StringBuilder buf = new StringBuilder();
		
		int i=0;
		int start=-1,end=-1; // < と > の位置
		
		while(true){
			start = s.indexOf('<',i);
			if(start<0){
				buf.append(s.substring(i));
				return line2div(buf.toString());
			}
			end = s.indexOf('>',start);
			if(end<0){
				buf.append(s.substring(i));
				return line2div(buf.toString());
			}
			// タグの前まで
			buf.append(s.substring(i, start));
			i=end+1;
			String tag = s.substring(start,end+1);
			tag = tag.toLowerCase();	// タグはすべて小文字に変換
			tag = tag.replaceAll("[\\r\\n]", "");
			
			//System.out.println("("+tag+")");
			Matcher tagNameMatcher = Pattern.compile("</?(\\w+)([^/>]*)/?>").matcher(tag);
			if(!tagNameMatcher.matches()){
				// タグ名が無い場合は削除
				continue;
			}
			String tagName = tagNameMatcher.group(1);
			//System.out.println("Tag["+tagName+"]");
			
			Set<String> ap = availableProperties.get(tagName);
			if(ap==null){
				// 未対応のタグは削除
				continue;
			}else{
				// 対応タグ
				if(tag.startsWith("</")){
					// 終了タグは属性チェックは要らない
					buf.append(tag);
					continue;
				}
				buf.append("<"+tagName);
				// タグ名はOK
				String prop = tagNameMatcher.group(2);
				if(!StringUtils.isBlank(prop)){
					// プロパティがある
					//System.out.println("  >>>["+prop+"]");
					while(true){
						Matcher propMatcher = Pattern.compile("\\s+(\\w+)=\"([^\"]*)\"").matcher(prop);
						if(!propMatcher.find() || propMatcher.groupCount()!=2){
							break;
						}
						//System.out.println("     ["+propMatcher.group(1)+"] ["+propMatcher.group(2)+"]");
						prop = prop.substring(propMatcher.end());
						if(ap.contains(propMatcher.group(1))){
							// 対応タグ
						//	System.out.println("       Found "+propMatcher.group(1));
							buf.append(" "+propMatcher.group(1)+"=\""+propMatcher.group(2)+"\"");
						}
					}
				}
				buf.append(">");
			}			
		}
	}
	
	/*
	 * 各行をdivで囲む
	 */
	private static String line2div(String s){
		//System.out.println("["+s+"]");
		StringBuilder buf = new StringBuilder();
		String[] lines = s.split("\n");
		buf.append(lines[0]);
		for(int i=1; i<lines.length-1; i++){
			//System.out.println(">>>>>>>>>>"+lines[i]);
			buf.append("<div>"+lines[i]+"</div>");
		}
		buf.append(lines[lines.length-1]);
		return buf.toString();
		
	}
	
	private static String replaceAllCaseInsenstive(String str, String regex, String repl){
		return Pattern.compile(regex,Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(str).replaceAll(repl);
	}
	
	public static void main(String[] args){
		String html = "<html><head>aa</head>" +
				"<Body link=\"aaa\">" +
				"<font>Top</font>"+
				"<H1>タイトル</H1><font color=\"red\" size=\"5\">RED</FONT><hoge>ZAK</hoge><BR>" +
				"aaaaa" +
				"<div>DIVER</div>" +
				"<div>" +
				"  <div>innere</div>" +
				"</div>" +
				"</body></html>";
		System.out.println(html);
		System.out.println(toDecomeHtml(html));
		System.out.println();
	}
}
