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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.auth.PlainAuthenticationHandlerFactory;
import org.subethamail.smtp.auth.UsernamePasswordValidator;
import org.subethamail.smtp.server.SMTPServer;

public class MyWiser implements MyMessageListener,MessageHandlerFactory {
	/** */
	private final static Log log = LogFactory.getLog(MyWiser.class);

	/** */
	SMTPServer server;

	/** */
	//List<MyWiserMessage> messages = Collections.synchronizedList(new ArrayList<MyWiserMessage>());
	
	private MyWiserMailListener listener;

	/**
	 * Create a new SMTP server with this class as the listener.
	 * The default port is 25. Call setPort()/setHostname() before
	 * calling start().
	 */
	public MyWiser(UsernamePasswordValidator userPass, int port, MyWiserMailListener listener)
	{
		this.server = new SMTPServer(this, new PlainAuthenticationHandlerFactory(userPass));
		this.server.setPort(port);
		this.listener = listener;
	}


	/**
	 * The hostname that the server should listen on.
	 * @param hostname
	 */
	public void setHostname(String hostname)
	{
		this.server.setHostName(hostname);
	}

	/** Starts the SMTP Server */
	public void start()
	{
		this.server.start();
	}

	/** Stops the SMTP Server */
	public void stop()
	{
		this.server.stop();
	}

	/** Always accept everything */
	public boolean accept(String from, String recipient)
	{
		if (log.isDebugEnabled())
			log.debug("Accepting mail from " + from + " to " + recipient);

		return true;
	}

	/** Cache the messages in memory */
	public void deliver(String from, List<String> recipient, InputStream data) throws TooMuchDataException,RejectException, IOException
	{
		if (log.isDebugEnabled())
			log.debug("Delivering mail from " + from + " to " + recipient);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		data = new BufferedInputStream(data);

		// read the data from the stream
		int current;
		while ((current = data.read()) >= 0)
		{
			out.write(current);
		}

		byte[] bytes = out.toByteArray();

		if (log.isDebugEnabled())
			log.debug("Creating message from data with " + bytes.length + " bytes");

		// create a new WiserMessage.
		try{
			
			this.listener.receiveMail(new MyWiserMessage(this, from, recipient, bytes));
		}catch (IOException e) {
			throw new RejectException(e.getMessage());
		}
		//this.messages.add(new MyWiserMessage(this, from, recipient, bytes));
	}

	/**
	 * Creates the JavaMail Session object for use in WiserMessage
	 */
	protected Session getSession()
	{
		return Session.getDefaultInstance(new Properties());
	}

	/**
	 * @return the list of WiserMessages
	 */
	/*
	public List<MyWiserMessage> getMessages()
	{
		return this.messages;
	}*/

	/**
	 * @return the server implementation
	 */
	public SMTPServer getServer()
	{
		return this.server;
	}

	/**
	 * For debugging purposes, dumps a rough outline of the messages to the output stream.
	 */
	/*
	public void dumpMessages(PrintStream out) throws MessagingException
	{
		out.println("----- Start printing messages -----");

		for (MyWiserMessage wmsg: this.getMessages())
			wmsg.dumpMessage(out);

		out.println("----- End printing messages -----");
	}
	*/
	
	public MessageHandler create(MessageContext messagecontext) {
		return new Handler(messagecontext);
	}

	class Handler implements MessageHandler
	{
		MessageContext ctx;
		String from;
		List<String> recipients = new ArrayList<String>();

		/** */
		public Handler(MessageContext ctx)
		{
			this.ctx = ctx;
		}

		/** */
		public void from(String from) throws RejectException
		{
			this.from = from;
		}

		/** */
		public void recipient(String recipient) throws RejectException
		{
			this.recipients.add(recipient);
		}

		/** */
		public void data(InputStream data) throws TooMuchDataException,RejectException, IOException
		{

			deliver(this.from, this.recipients, data);
		}

		/** */
		public void done()
		{
		}
	}
	
}
