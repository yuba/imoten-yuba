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

import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MyMimeMessage extends MimeMessage {
	private InternetAddress from;
	private List<InternetAddress> recipients;

	public MyMimeMessage(Session session, InternetAddress from,
			List<InternetAddress> recipients) {
		super(session);
		this.from = from;
		this.recipients = recipients;
	}

	@Override
	public Address[] getFrom() throws MessagingException {
		Address[] froms = new Address[1];
		froms[0] = this.from;
		return froms;
	}

	@Override
	public Address[] getRecipients(javax.mail.Message.RecipientType type)
			throws MessagingException {
		if (type == Message.RecipientType.TO) {
			return this.recipients.toArray(new Address[0]);
		}
		return new Address[0];
	}

}
