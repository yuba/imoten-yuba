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

package immf.growl.concrete;

import immf.Config;
import immf.growl.GrowlApiClient;

public class NMAClient extends GrowlApiClient {

	//シングルトンインスタンス
	private static GrowlApiClient instance = new NMAClient();

	//シングルトンインスタンスの取得
	public static GrowlApiClient getInstance() {
		return instance;
	}

	private NMAClient() {

		super();

		this.topLevelTag = "nma";

		this.verifyUrl = "https://www.notifymyandroid.com/publicapi/verify?apikey=";

		this.postUrl = "https://www.notifymyandroid.com/publicapi/notify";
	}

	@Override
	public String getApiKeyFromConfig(Config config) {
		return config.getForwardNMAKeys();
	}

}
