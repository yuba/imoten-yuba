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

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class MyWiserMessage {
	byte[] messageData;
	MyWiser wiser;
	String envelopeSender;
	List<String> envelopeReceiver;

	MyWiserMessage(MyWiser wiser, String envelopeSender, List<String> envelopeReceiver, byte[] messageData)
	{
		this.wiser = wiser;
		this.envelopeSender = envelopeSender;
		this.envelopeReceiver = envelopeReceiver;
		this.messageData = messageData;
	}

	/**
	 * Generate a JavaMail MimeMessage.
	 * @throws MessagingException
	 */
	public MimeMessage getMimeMessage() throws MessagingException
	{
		return new MimeMessage(this.wiser.getSession(), new ByteArrayInputStream(this.messageData));
	}

	/**
	 * Get's the raw message DATA.
	 */
	public byte[] getData()
	{
		return this.messageData;
	}

	/**
	 * Get's the RCPT TO:
	 */
	public List<String> getEnvelopeReceiver()
	{
		return this.envelopeReceiver;
	}

	/**
	 * Get's the MAIL FROM:
	 */
	public String getEnvelopeSender()
	{
		return this.envelopeSender;
	}

	/**
	 * Dumps the rough contents of the message for debugging purposes
	 */
	public void dumpMessage(PrintStream out) throws MessagingException
	{
		out.println("===== Dumping message =====");

		out.println("Envelope sender: " + this.getEnvelopeSender());
		out.println("Envelope recipient: " + this.getEnvelopeReceiver());

		// It should all be convertible with ascii or utf8
		String content = new String(this.getData());
		out.println(content);

		out.println("===== End message dump =====");
	}

	/**
	 * Implementation of toString()
	 *
	 * @return getData() as a string or an empty string if getData is null
	 */
	@Override
	public String toString()
	{
		if (this.getData() == null)
			return "";

		return new String(this.getData());
	}
}
