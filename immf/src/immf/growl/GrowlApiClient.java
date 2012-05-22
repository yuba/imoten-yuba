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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class GrowlApiClient {

	// Logger
	private static final Log log = LogFactory.getLog(GrowlApiClient.class);

	// Call Remainging Reset Time
	protected Calendar resetTime = null;

	// Calls Remaining
	protected int callsRemaining = 800;

	// Internal Server Error Count
	protected int internalServerErrorCount = 0;

	// Maximum Internal Server Error Count
	private final int MAX_INTERNAL_SERVER_ERROR_COUNT = 10;

	// HTTP Client
	protected DefaultHttpClient httpClient = new DefaultHttpClient();

	// API URI for check device Key
	protected String verifyUrl = "";

	// API URI
	protected String postUrl = "";

	// Xml Tags
	protected final static String TAG_ERROR = "error";
	protected final static String TAG_SUCCESS = "success";
	protected final static String ATTR_RESETTIMER = "resettimer";
	protected final static String ATTR_RESETDATE = "resetdate";
	protected final static String ATTR_REMAINING = "remaining";

	// Top Level Tags
	protected String topLevelTag = "";

	// Client state
	protected GrowlApiClientState state = GrowlApiClientState.AVAILABLE;

	protected GrowlApiClient() {
		this.resetTime = Calendar.getInstance();
	}

	public abstract String getApiKeyFromConfig(Config config);

	public GrowlApiClientState getState() {

		if (this.callsRemaining > 0 && this.state == GrowlApiClientState.EXCEEDED) {
			this.setState(GrowlApiClientState.AVAILABLE);
		} else if (this.state != GrowlApiClientState.EXCEEDED) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());

			if (this.resetTime.before(cal))
				this.setState(GrowlApiClientState.AVAILABLE);
		}

		return this.state;
	}

	public int getRemaining() {
		return this.callsRemaining;
	}

	public String getResetTimeStamp() {
		SimpleDateFormat df = new SimpleDateFormat(GrowlNotifier.DATE_FORMAT);
		return df.format(this.resetTime.getTime());
	}

	public void verify(GrowlKey apiKey) {
		if (apiKey != null && GrowlKeyState.NOT_CHECKED == apiKey.getState()) {
			HttpPost post = new HttpPost(this.verifyUrl + apiKey.getKey());
			GrowlApiResult result = this.callApi(post);
			if (result == GrowlApiResult.SUCCESS) {
				apiKey.setState(GrowlKeyState.VALID);
			} else if (result == GrowlApiResult.INVALID) {
				apiKey.setState(GrowlKeyState.INVALID);
			}
		}
	}

	public boolean post(List<NameValuePair> formparams) {

		HttpPost post = new HttpPost(this.postUrl);

		UrlEncodedFormEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		} catch (Exception e) {
		}
		post.setEntity(entity);
		post.setHeader("User-Agent", "imoten/1.0 (immfGrowlNotifier 0.1;)");

		GrowlApiResult result = this.callApi(post);

		if (GrowlApiResult.SUCCESS == result) {
			return true;
		} else {
			return false;
		}
	}

	public GrowlApiResult callApi(HttpPost post) {

		if (GrowlApiClientState.EXCEEDED == this.state) {
			return GrowlApiResult.EXCEEDED;
		} else if (GrowlApiClientState.ERROR == this.state) {
			return GrowlApiResult.ERROR;
		}

		try {
			GrowlApiResult result = this.analyzeResponse(this.httpClient.execute(post));
			if (this.internalServerErrorCount > MAX_INTERNAL_SERVER_ERROR_COUNT) {
				this.setState(GrowlApiClientState.ERROR);
				log.error("Internal server error count is exceeded.");
			} else {
				log.info("Remaining:" + this.getRemaining() + ", Reset Date:" + this.getResetTimeStamp());
			}
			return result;
		} catch (ClientProtocolException e) {
			GrowlNotifier.log.error(e.getMessage());
		} catch (IOException e) {
			GrowlNotifier.log.error(e.getMessage());
		} finally {
			post.abort();
		}
		return GrowlApiResult.ERROR;
	}

	private GrowlApiResult analyzeResponse(HttpResponse res) {

		GrowlApiResult result = GrowlApiResult.ERROR;

		if (res == null) {
			GrowlNotifier.log.info("HttpResponse is null");
			this.incrementInternalServerErrorCount();
			return GrowlApiResult.ERROR;
		} else {
			decrementCallsRemaining();
			int status = res.getStatusLine().getStatusCode();
			switch (status) {
			case 200:
				log.info("Api call is success.");
				result = GrowlApiResult.SUCCESS;
				break;
			case 400:
				log.error("The data supplied is in the wrong format, invalid length or null.");
				result = GrowlApiResult.INVALID;
				break;
			case 401:
				log.error("The 'apikey' provided is not valid.");
				result = GrowlApiResult.INVALID;
				break;
			case 402:
			case 406:
				log.error("Maximum number of API calls exceeded.");
				result = GrowlApiResult.EXCEEDED;
				break;
			case 500:
				log.error("Internal server error.");
				this.incrementInternalServerErrorCount();
				result = GrowlApiResult.ERROR;
				break;
			default:
				;
			}

			if (status == 200 || status == 402 || status == 406) {
				HttpEntity entity = res.getEntity();
				try {
					final InputStream in = entity.getContent();
					final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					final Document resDoc = builder.parse(in);

					// ルートノードの取得
					Element rootNode = resDoc.getDocumentElement();
					if (rootNode != null && this.topLevelTag.equals(rootNode.getNodeName())) {
						NodeList rootChildren = rootNode.getChildNodes();
						for (int i = 0, max = rootChildren.getLength(); i < max; i++) {
							Node node = rootChildren.item(i);
							if (TAG_ERROR.equals(node.getNodeName()) || TAG_SUCCESS.equals(node.getNodeName())) {
								NamedNodeMap attrs = node.getAttributes();
								if (attrs != null) {
									Node attrNode = attrs.getNamedItem(ATTR_RESETTIMER);
									if (attrNode != null && attrNode.getNodeValue().length() > 0) {
										setResetTimer(Integer.parseInt(attrNode.getNodeValue()));
									}
									attrNode = attrs.getNamedItem(ATTR_RESETDATE);
									if (attrNode != null && attrNode.getNodeValue().length() > 0) {
										setResetDate(Integer.parseInt(attrNode.getNodeValue()));
									}
									attrNode = attrs.getNamedItem(ATTR_REMAINING);
									if (attrNode != null && attrNode.getNodeValue().length() > 0) {
										int remaining = Integer.parseInt(attrNode.getNodeValue());
										setCallsRemaining(remaining);
									}
								}
							}
						}
					}
				} catch (IllegalStateException e) {
					log.error(e.getMessage());
				} catch (IOException e) {
					log.error(e.getMessage());
				} catch (ParserConfigurationException e) {
					log.error(e.getMessage());
				} catch (SAXException e) {
					log.error(e.getMessage());
				}
			}

			return result;

		}
	}

	private synchronized void setResetTimer(int value) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, value);
		this.resetTime = cal;
	}

	private synchronized void setResetDate(long value) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(value * 1000);
		this.resetTime = cal;
	}

	private synchronized void setCallsRemaining(int value) {
		this.callsRemaining = value;
	}

	private synchronized void decrementCallsRemaining() {
		this.callsRemaining--;
	}

	private synchronized void setState(GrowlApiClientState state) {
		this.state = state;
	}

	private synchronized void incrementInternalServerErrorCount() {
		this.internalServerErrorCount++;
	}
}
