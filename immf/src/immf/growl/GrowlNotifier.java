/*
 * imoten - i mode.net mail tensou(forward)
 *
 * Copyright (C) 2011 StarAtlas (http://code.google.com/p/imoten/)
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

package immf.growl;

import immf.Config;
import immf.EmojiUtil;
import immf.ImodeMail;
import immf.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class GrowlNotifier implements Runnable {

	// Logger
	protected static final Log log = LogFactory.getLog(GrowlNotifier.class);

	// GrowlClient
	private GrowlApiClient client = null;

	// Api key
	private List<GrowlKey> growlApiKeyList = new ArrayList<GrowlKey>();

	// forword mail query
	private List<ImodeMail> queue = new LinkedList<ImodeMail>();

	// Maximum query count
	private static final int MaxQueueSize = 10;

	// Maximum discription length
	private static final int MAX_DISCRIPTION_LENGTH = 1000;

	// Loop timeout
	private static final int TIMEOUT_SECTIME = 60;

	// date format
	public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

	// Configインスタンス
	private Config config = null;

	public static GrowlNotifier getInstance(GrowlApiClient clientConcrete, Config config) {
		if (clientConcrete != null && config != null) {

			GrowlNotifier notify = new GrowlNotifier();

			notify.client = clientConcrete;

			notify.config = config;

			String apiKey = clientConcrete.getApiKeyFromConfig(config);

			if(apiKey!=null && apiKey!="") {
				String[] apiKeyArray = apiKey.split(",");
				for (String key : apiKeyArray) {
					notify.growlApiKeyList.add(new GrowlKey(key));
				}

				if (notify.growlApiKeyList.size() > 0) {
					notify.client = clientConcrete;
					Thread t = new Thread(notify);
					t.setName(notify.client.getClass().getName());
					t.setDaemon(true);
					t.start();
				}
			}

			return notify;

		} else {
			return null;
		}
	}

	private GrowlNotifier() {
	}

	public void forward(ImodeMail mail) {
		if (this.growlApiKeyList.size() == 0) {
			return;
		}
		synchronized (this.queue) {
			if (this.queue.size() > MaxQueueSize) {
				log.error("query size " + this.queue + "(max:" + MaxQueueSize + ")");
				log.error("GrowlApi(" + this.client.topLevelTag + ")に問題が発生しています。通知は送信されません");
				return;
			}
			this.queue.add(mail);
			this.queue.notifyAll();
		}
	}

	public void run() {

		log.info("Growl Notifier(" + this.client.topLevelTag + ") Start");

		while (true) {
			ImodeMail mail = null;
			synchronized (this.queue) {
				while (this.queue.isEmpty() || GrowlApiClientState.AVAILABLE != this.client.getState()) {
					try {
						this.queue.wait(1000 * TIMEOUT_SECTIME);
					} catch (Exception e) {
					}
				}
				mail = this.queue.remove(0);
			}

			// 有効性チェック
			for (GrowlKey growlKey : this.growlApiKeyList) {
				if (GrowlKeyState.NOT_CHECKED == growlKey.getState()) {
					this.client.verify(growlKey);
				}
			}

			// 有効なDeviceApiKeyに通知
			if (!this.send(mail)) {
				// 通知出来なかった場合はキューの最後に追加する
				this.queue.add(mail);
			}
		}

	}

	private boolean send(ImodeMail mail) {

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();

		StringBuilder apiStringBuilder = new StringBuilder();
		for (GrowlKey growlKey : this.growlApiKeyList) {
			if (GrowlKeyState.VALID == growlKey.getState()) {
				apiStringBuilder.append(growlKey.getKey());
				apiStringBuilder.append(",");
			}
		}
		if (apiStringBuilder.length() == 0)
			return false;

		String apiKey = apiStringBuilder.substring(0, apiStringBuilder.length() - 1);
		formparams.add(new BasicNameValuePair("apikey", apiKey));
		formparams.add(new BasicNameValuePair("application", "imoten"));

		StringBuilder eventStringBuilder = new StringBuilder("新着メールを受信しました");
		if (mail.getFromAddr().getPersonal() != null && this.config.isForwardPushFrom()) {
			eventStringBuilder.insert(0, "さんから");
			eventStringBuilder.insert(0, mail.getFromAddr().getPersonal());
		} else if ( this.config.isForwordPushNotifyAddress() || this.config.isForwardPushNotifyUnknownAddress() ){
			eventStringBuilder.insert(0, "から");
			eventStringBuilder.insert(0, mail.getFromAddr().getAddress());
		}
		formparams.add(new BasicNameValuePair("event", eventStringBuilder.toString()));

		StringBuilder message = new StringBuilder();
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		if ( mail.getFromAddr().getPersonal() != null && this.config.isForwordPushNotifyAddress() ) {
			message.append("From:").append(mail.getFromAddr().getAddress()).append("\r\n");
		}
		message.append("日時:").append(df.format(mail.getTimeDate())).append("\r\n");
		if(this.config.isForwardPushSubject())
			message.append("件名:").append(EmojiUtil.replaceToLabel(mail.getSubject())).append("\r\n");
		if(this.config.isForwordPushNotifyBody()) {
			message.append("本文:\r\n");
			String messageBody = mail.getBody();
			if (mail.isDecomeFlg()) {
				messageBody = Util.html2text(messageBody);
			}
			message.append(messageBody);
		}

		if (message.length() > MAX_DISCRIPTION_LENGTH) {
			formparams.add(new BasicNameValuePair("description", message.substring(0, MAX_DISCRIPTION_LENGTH - 1)));
		} else {
			formparams.add(new BasicNameValuePair("description", message.toString()));
		}

		return this.client.post(formparams);

	}
}
