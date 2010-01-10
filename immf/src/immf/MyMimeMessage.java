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
