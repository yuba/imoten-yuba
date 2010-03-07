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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EmojiUtil {
	private static final Log log = LogFactory.getLog(EmojiUtil.class);
	private static final String URLPrefix = "https://mail.google.com/mail/e/docomo_ne_jp/";
	public static final String UnknownReplace = "[?]";
	private static Map<Character, Emoji> map;
	static{
		map = new TreeMap<Character, Emoji>();
		// 基本絵文字
		add(0xe63e,"晴れ","000");
		add(0xe63f,"曇り","001");
		add(0xe640,"雨","002");
		add(0xe641,"雪","003");
		add(0xe642,"雷","004");
		add(0xe643,"台風","005");
		add(0xe644,"霧","006");
		add(0xe645,"小雨","007");
		add(0xe646,"牡羊座","02B");
		add(0xe647,"牡牛座","02C");
		add(0xe648,"双子座","02D");
		add(0xe649,"蟹座","02E");
		add(0xe64a,"獅子座","02F");
		add(0xe64b,"乙女座","030");
		add(0xe64c,"天秤座","031");
		add(0xe64d,"蠍座","032");
		add(0xe64e,"射手座","033");
		add(0xe64f,"山羊座","034");
		add(0xe650,"水瓶座","035");
		add(0xe651,"魚座","036");
		add(0xe652,"スポーツ","7D0");
		add(0xe653,"野球","7D1");
		add(0xe654,"ゴルフ","7D2");
		add(0xe655,"テニス","7D3");
		add(0xe656,"サッカー","7D4");
		add(0xe657,"スキー","7D5");
		add(0xe658,"バスケットボール","7D6");
		add(0xe659,"モータースポーツ","7D7");
		add(0xe65a,"ポケットベル","522");
		add(0xe65b,"電車","7DF");
		add(0xe65c,"地下鉄","7E1");
		add(0xe65d,"新幹線","7E2");
		add(0xe65e,"車(セダン)","7E4");
		add(0xe65f,"車(RV)","7E5");
		add(0xe660,"バス","7E6");
		add(0xe661,"船","7E8");
		add(0xe662,"飛行機","7E9");
		add(0xe663,"家","4B0");
		add(0xe664,"ビル","4B2");
		add(0xe665,"郵便局","4B3");
		add(0xe666,"病院","4B4");
		add(0xe667,"銀行","4B5");
		add(0xe668,"ATM","4B6");
		add(0xe669,"ホテル","4B7");
		add(0xe66a,"コンビニ","4B9");
		add(0xe66b,"ガソリンスタンド","7F5");
		add(0xe66c,"駐車場","7F6");
		add(0xe66d,"信号","7F7");
		add(0xe66e,"トイレ","506");
		add(0xe66f,"レストラン","980");
		add(0xe670,"喫茶店","981");
		add(0xe671,"バー","982");
		add(0xe672,"ビール","983");
		add(0xe673,"ファーストフード","960");
		add(0xe674,"ブティック","4D6");
		add(0xe675,"美容院","53E");
		add(0xe676,"カラオケ","800");
		add(0xe677,"映画","801");
		add(0xe678,"右斜め上","AF0");
		add(0xe679,"遊園地","7FC");
		add(0xe67a,"音楽","803");
		add(0xe67b,"アート","804");
		add(0xe67c,"演劇","805");
		add(0xe67d,"イベント","806");
		add(0xe67e,"チケット","807");
		add(0xe67f,"喫煙","B1E");
		add(0xe680,"禁煙","B1F");
		add(0xe681,"カメラ","4EF");
		add(0xe682,"かばん","4F0");
		add(0xe683,"本","545");
		add(0xe684,"リボン","50F");
		add(0xe685,"プレゼント","510");
		add(0xe686,"バースデー","511");
		add(0xe687,"電話","523");
		add(0xe688,"携帯電話","525");
		add(0xe689,"メモ","527");
		add(0xe68a,"TV","81C");
		add(0xe68b,"ゲーム","80A");
		add(0xe68c,"CD","81D");
		add(0xe68d,"ハート","B1A");
		add(0xe68e,"スペード","B1B");
		add(0xe68f,"ダイヤ","B1C");
		add(0xe690,"クラブ","B1D");
		add(0xe691,"目","190");
		add(0xe692,"耳","191");
		add(0xe693,"手(グー)","B93");
		add(0xe694,"手(チョキ)","B94");
		add(0xe695,"手(パー)","B95");
		add(0xe696,"右斜め下","AF1");
		add(0xe697,"左斜め上","AF2");
		add(0xe698,"足","1DB");
		add(0xe699,"くつ","4CC");
		add(0xe69a,"眼鏡","4CD");
		add(0xe69b,"車椅子","B20");
		add(0xe69c,"新月","B65");
		add(0xe69d,"やや欠け月","012");
		add(0xe69e,"半月","013");
		add(0xe69f,"三日月","014");
		add(0xe6a0,"満月","015");
		add(0xe6a1,"犬","1D8");
		add(0xe6a2,"猫","1B8");
		add(0xe6a3,"リゾート","7EA");
		add(0xe6a4,"クリスマス","512");
		add(0xe6a5,"左斜め下","AF3");
		add(0xe6ce,"phone to","526");
		add(0xe6cf,"mail to","52B");
		add(0xe6d0,"fax to","528");
		add(0xe6d1,"iモード","E10");
		add(0xe6d2,"iモード(枠付き)","E11");
		add(0xe6d3,"メール","529");
		add(0xe6d4,"ドコモ提供","E12");
		add(0xe6d5,"ドコモポイント","E13");
		add(0xe6d6,"有料","4E2");
		add(0xe6d7,"無料","B21");
		add(0xe6d8,"ID","B81");
		add(0xe6d9,"パスワード","B82");
		add(0xe6da,"次項有","B83");
		add(0xe6db,"クリア","B84");
		add(0xe6dc,"サーチ(調べる)","B85");
		add(0xe6dd,"New","B36");
		add(0xe6de,"位置情報","B22");
		add(0xe6df,"フリーダイヤル","82B");
		add(0xe6e0,"シャープダイヤル","82C");
		add(0xe6e1,"モバQ","82D");
		add(0xe6e2,"1","82E");
		add(0xe6e3,"2","82F");
		add(0xe6e4,"3","830");
		add(0xe6e5,"4","831");
		add(0xe6e6,"5","832");
		add(0xe6e7,"6","833");
		add(0xe6e8,"7","834");
		add(0xe6e9,"8","835");
		add(0xe6ea,"9","836");
		add(0xe6eb,"0","837");
		add(0xe70b,"決定","B27");
		add(0xe6ec,"黒ハート","B0C");
		add(0xe6ed,"揺れるハート","B0D");
		add(0xe6ee,"失恋","B0E");
		add(0xe6ef,"ハートたち(複数ハート)","B0F");
		add(0xe6f0,"わーい(嬉しい顔)","330");
		add(0xe6f1,"ちっ(怒った顔)","320");
		add(0xe6f2,"がく～(落胆した顔)","323");
		add(0xe6f3,"もうやだ～(悲しい顔)","33F");
		add(0xe6f4,"ふらふら","324");
		add(0xe6f5,"グッド(上向き矢印)","AF4");
		add(0xe6f6,"るんるん","813");
		add(0xe6f7,"いい気分(温泉)","7FA");
		add(0xe6f8,"かわいい","B55");
		add(0xe6f9,"キスマーク","823");
		add(0xe6fa,"ぴかぴか(新しい)","B60");
		add(0xe6fb,"ひらめき","B56");
		add(0xe6fc,"むかっ(怒り)","B57");
		add(0xe6fd,"パンチ","B96");
		add(0xe6fe,"爆弾","B58");
		add(0xe6ff,"ムード","814");
		add(0xe700,"バッド(下向き矢印)","AF5");
		add(0xe701,"眠い(睡眠)","B59");
		add(0xe702,"exclamation","B04");
		add(0xe703,"exclamation&question","B05");
		add(0xe704,"exclamationx2","B06");
		add(0xe705,"どんっ(衝撃)","B5A");
		add(0xe706,"あせあせ(飛び散る汗)","B5B");
		add(0xe707,"たらーっ(汗)","B5C");
		add(0xe708,"ダッシュ(走り出すさま)","B5D");
		add(0xe709,"ー(長音記号1)","B07");
		add(0xe70a,"ー(長音記号2)","B08");
		add(0xe6ac,"カチンコ","808");
		add(0xe6ad,"ふくろ","4F1");
		add(0xe6ae,"ペン","536");
		add(0xe6b1,"人影","19A");
		add(0xe6b2,"いす","537");
		add(0xe6b3,"夜","008");
		add(0xe6b7,"soon","018");
		add(0xe6b8,"on","019");
		add(0xe6b9,"end","01A");
		add(0xe6ba,"時計","02A");
		// 拡張絵文字
		add(0xe70c,"iアプリ","E14");
		add(0xe70d,"iアプリ(枠付き)","E15");
		add(0xe70e,"Tシャツ(ボーダー)","4CF");
		add(0xe70f,"がま口財布","4DC");
		add(0xe710,"化粧","195");
		add(0xe711,"ジーンズ","4D0");
		add(0xe712,"スノボ","7D8");
		add(0xe713,"チャペル","4F2");
		add(0xe714,"ドア","4F3");
		add(0xe715,"ドル袋","4DD");
		add(0xe716,"パソコン","538");
		add(0xe717,"ラブレター","824");
		add(0xe718,"レンチ","4C9");
		add(0xe719,"鉛筆","539");
		add(0xe71a,"王冠","4D1");
		add(0xe71b,"指輪","825");
		add(0xe71c,"砂時計","01B");
		add(0xe71d,"自転車","7EB");
		add(0xe71e,"湯のみ","984");
		add(0xe71f,"腕時計","01D");
		add(0xe720,"考えてる顔","340");
		add(0xe721,"ほっとした顔","33E");
		add(0xe722,"冷や汗","331");
		add(0xe723,"冷や汗2","344");
		add(0xe724,"ぷっくっくな顔","33D");
		add(0xe725,"ボケーっとした顔","326");
		add(0xe726,"目がハート","327");
		add(0xe727,"指でOK","B97");
		add(0xe728,"あっかんベー","329");
		add(0xe729,"ウインク","347");
		add(0xe72a,"うれしい顔","332");
		add(0xe72b,"がまん顔","33C");
		add(0xe72c,"猫2","343");
		add(0xe72d,"泣き顔","33A");
		add(0xe72e,"涙","339");
		add(0xe72f,"NG","B28");
		add(0xe730,"クリップ","53A");
		add(0xe731,"コピーライト","B29");
		add(0xe732,"トレードマーク","B2A");
		add(0xe733,"走る人","7D9");
		add(0xe734,"マル秘","B2B");
		add(0xe735,"リサイクル","B2C");
		add(0xe736,"レジスタードトレードマーク","B2D");
		add(0xe737,"危険・警告","B23");
		add(0xe738,"禁止","B2E");
		add(0xe739,"空室・空席・空車","B2F");
		add(0xe73a,"合格マーク","B30");
		add(0xe73b,"満室・満席・満車","B31");
		add(0xe73c,"矢印左右","AF6");
		add(0xe73d,"矢印上下","AF7");
		add(0xe73e,"学校","4BA");
		add(0xe73f,"波","038");
		add(0xe740,"富士山","4C3");
		add(0xe741,"クローバー","03C");
		add(0xe742,"さくらんぼ","04F");
		add(0xe743,"チューリップ","03D");
		add(0xe744,"バナナ","050");
		add(0xe745,"りんご","051");
		add(0xe746,"芽","03E");
		add(0xe747,"もみじ","03F");
		add(0xe748,"桜","040");
		add(0xe749,"おにぎり","961");
		add(0xe74a,"ショートケーキ","962");
		add(0xe74b,"とっくり(おちょこ付き)","985");
		add(0xe74c,"どんぶり","963");
		add(0xe74d,"パン","964");
		add(0xe74e,"かたつむり","1B9");
		add(0xe74f,"ひよこ","1BA");
		add(0xe750,"ペンギン","1BC");
		add(0xe751,"魚","1BD");
		add(0xe752,"うまい!","32B");
		add(0xe753,"ウッシッシ","333");
		add(0xe754,"ウマ","1BE");
		add(0xe755,"ブタ","1BF");
		add(0xe756,"ワイングラス","986");
		add(0xe757,"げっそり","341");
	}
	private static void add(int unicode, String label, String googleImage){
		char c = (char)unicode;
		map.put(Character.valueOf(c), new Emoji(c,label,googleImage));
	}
	
	/**
	 * 絵文字があったら[晴れ]のような説明に置き換える
	 * @param s
	 * @return
	 */
	public static String replaceToLabel(String s){
		StringBuilder buf = new StringBuilder();
		for(char c : s.toCharArray()){
			if(!isEmoji(c)){
				buf.append(c);
				continue;
			}
			Emoji e = map.get(c);
			if(e==null){
				buf.append(UnknownReplace);
			}else{
				buf.append("["+e.getLabel()+"]");
			}
		}
		return buf.toString();
	}
	
	/**
	 * 絵文字があったら<img src="絵文字画像URL">に置き換える
	 * @param s
	 * @return
	 */
	public static String replaceToWebLink(String s){
		StringBuilder buf = new StringBuilder();
		for(char c : s.toCharArray()){
			if(!isEmoji(c)){
				buf.append(c);
				continue;
			}
			Emoji e = map.get(c);
			if(e==null){
				buf.append(UnknownReplace);
			}else{
				buf.append("<img src=\""+URLPrefix+e.getgoogleImage()+"\" style=\"margin: 0pt 0.2ex; vertical-align: middle;\">");
			}
		}
		return buf.toString();
	}
	
	/**
	 * 絵文字の画像ファイルのURL
	 * @param c
	 * @return
	 */
	public static URL emojiToImageUrl(char c){
		Emoji e = map.get(c);
		if(e==null){
			return null;
		}
		try{
			return new URL(URLPrefix+e.getgoogleImage());
		}catch (Exception ex) {
			log.warn(ex);
			return null;
		}
	}
	/**
	 * 文字が絵文字かどうか
	 * @param c
	 * @return
	 */
	public static boolean isEmoji(char c){
		if(Character.UnicodeBlock.of(c) == Character.UnicodeBlock.PRIVATE_USE_AREA){
			return true;
		}else{
			return false;
		}
	}
	
	// 絵文字の一覧を作成
	public static void main(String[] args){
		BufferedWriter br = null;
		try{
			br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./emoji.html"),"UTF-8"));
			
			br.write("<html>" +
					"<head>" +
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"+
					"</head>" +
					"<body>"+ServerMain.Version+"<br><br>"+
					"<table>");
			br.write("<th>Unicode　</th><th>画像　</th><th>ラベル　</th>");
			for (Emoji e : map.values()) {
				br.write("<tr>\n");
				br.write("    <td>"+String.format("0x%x", (int)e.getC())+"</td>" +
						"<td><img src='"+emojiToImageUrl(e.getC())+"'></td>" +
						"<td>"+e.getLabel()+"</td>\n");
				br.write("</tr>\n");
			}
			br.write("</table></body></html>");
			br.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
