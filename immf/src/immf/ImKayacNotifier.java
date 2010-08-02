package immf;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class ImKayacNotifier implements Runnable{
  private static final String ImKayacUrl = "http://im.kayac.com/api/post/";
	private static final Log log = LogFactory.getLog(ImKayacNotifier.class);
	private static final int MaxQueueLength = 10;
	private String username;
	private String secret;
	private DefaultHttpClient httpClient;

	private List<ImodeMail> queue = new LinkedList<ImodeMail>();

	public ImKayacNotifier(String username,String secret){
		this.username = username;
		this.secret = secret;
		this.httpClient = new DefaultHttpClient();

		if(this.username!="" && this.secret!=""){
			Thread t = new Thread(this);
			t.setName("ImKayacNotifier");
			t.setDaemon(true);
			t.start();
		}
	}

	public void forward(ImodeMail mail){
		if(this.username=="" || this.secret==""){
			return;
		}
		synchronized (this.queue) {
			if(this.queue.size()>MaxQueueLength){
				log.error("im.kayac.com Queue size "+this.queue);
				log.error("おそらく im.kayac.com がフリーズしています。メールはim.kayac.comに転送されません。");
				return;
			}
			this.queue.add(mail);
			this.queue.notifyAll();
		}
	}

	public void run() {
		log.info("ImKayac Forwarder Start");
/*		try{
			log.info("Skype Version "+Skype.getVersion());
		}catch (Exception e) {
			log.error("Skype getVersion Error.",e);
		}*/
		while(true){
			ImodeMail mail = null;
			synchronized (this.queue) {
				while(this.queue.isEmpty()){
					try{
						this.queue.wait(1000*60);
					}catch (Exception e) {}
				}
				mail = this.queue.remove(0);
			}
			if(this.username!="" && this.secret!=""){
				this.sendToImKayac(mail);
			}
		}
	}

	private void sendToImKayac(ImodeMail mail){
		String text=mail.getBody();
		if(mail.isDecomeFlg()){
			text = Util.html2text(text);
		}
		text = imKayacHeader(mail) + text;

		if (text.length() > 512) {
		  text = text.substring(0,511);//512文字以上の場合は切ってしまおう
		}

		log.info("Try to sending IM to ["+this.username+"] via im.kayac.com");
		try{
      HttpPost post = new HttpPost(ImKayacUrl+this.username);
      String digest = encrypt(text+this.secret);


      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("message",text));
			formparams.add(new BasicNameValuePair("sig",digest));
			UrlEncodedFormEntity entity = null;
			try{
				entity = new UrlEncodedFormEntity(formparams,"UTF-8");
			}catch (Exception e) {}
			post.setHeader("User-Agent","imoten/1.5 (immfImKayacNotifier 0.1;)");
			post.setEntity(entity);
			try{
				HttpResponse res = this.httpClient.execute(post);
				if(res==null){
          log.info("im.kayac.com error - response is null");
				}
				if(res.getStatusLine().getStatusCode()!=200){
					log.info("im.kayac.com error - bad status code "+res.getStatusLine().getStatusCode());
				}

  			JSONObject json = JSONObject.fromObject(EntityUtils.toString(res.getEntity(),"UTF-8"));

  			String result = json.getString("result");
  			if(!result.equals("posted")){
  				log.debug(json.toString(2));
    			log.info("im.kayac.com error");
				} else {
				  log.info("Send IM to ["+this.username+"] via im.kayac.com complete!");
				}
			}finally{
				post.abort();
			}
		}catch (Exception e) {
			log.error("im.kayac Error.",e);
		}
	}

	private static String imKayacHeader(ImodeMail mail){
		StringBuilder buf = new StringBuilder();
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		buf.append("From:").append(mail.getFromAddr()).append("\r\n");
		buf.append("Date:").append(df.format(mail.getTimeDate())).append("\r\n");
		buf.append("Subject:").append(EmojiUtil.replaceToLabel(mail.getSubject())).append("\r\n");
		buf.append("\r\n");
		return buf.toString();
	}

  public static String encrypt(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update(input.getBytes("UTF-8"));
      return toHexString(md.digest());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static String toHexString(byte[] b) {
    StringBuffer hexString = new StringBuffer();
    String plainText=null;
    for (int i = 0; i < b.length; i++) {
      plainText = Integer.toHexString(0xFF & b[i]);
      if (plainText.length() < 2) {
        plainText = "0" + plainText;
      }
      hexString.append(plainText);
    }
    return new String(hexString);
  }
}
