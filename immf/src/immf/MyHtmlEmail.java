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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;

/**
 * An HTML multipart email.
 *
 * <p>This class is used to send HTML formatted email.  A text message
 * can also be set for HTML unaware email clients, such as text-based
 * email clients.
 *
 * <p>This class also inherits from {@link MultiPartEmail}, so it is easy to
 * add attachments to the email.
 *
 * <p>To send an email in HTML, one should create a <code>HtmlEmail</code>, then
 * use the {@link #setFrom(String)}, {@link #addTo(String)} etc. methods.
 * The HTML content can be set with the {@link #setHtmlMsg(String)} method. The
 * alternative text content can be set with {@link #setTextMsg(String)}.
 *
 * <p>Either the text or HTML can be omitted, in which case the "main"
 * part of the multipart becomes whichever is supplied rather than a
 * <code>multipart/alternative</code>.
 *
 * <h3>Embedding Images and Media</h3>
 *
 * <p>It is also possible to embed URLs, files, or arbitrary
 * <code>DataSource</code>s directly into the body of the mail:
 * <pre><code>
 * HtmlEmail he = new HtmlEmail();
 * File img = new File("my/image.gif");
 * PNGDataSource png = new PNGDataSource(decodedPNGOutputStream); // a custom class
 * StringBuffer msg = new StringBuffer();
 * msg.append("&lt;html&gt;&lt;body&gt;");
 * msg.append("&lt;img src=cid:").append(he.embed(img)).append("&gt;");
 * msg.append("&lt;img src=cid:").append(he.embed(png)).append("&gt;");
 * msg.append("&lt;/body&gt;&lt;/html&gt;");
 * he.setHtmlMsg(msg.toString());
 * // code to set the other email fields (not shown)
 * </pre></code>
 *
 * <p>Embedded entities are tracked by their name, which for <code>File</code>s is
 * the filename itself and for <code>URL</code>s is the canonical path. It is
 * an error to bind the same name to more than one entity, and this class will
 * attempt to validate that for <code>File</code>s and <code>URL</code>s. When
 * embedding a <code>DataSource</code>, the code uses the <code>equals()</code>
 * method defined on the <code>DataSource</code>s to make the determination.
 *
 * @since 1.0
 * @author <a href="mailto:unknown">Regis Koenig</a>
 * @author <a href="mailto:sean@informage.net">Sean Legassick</a>
 * @version $Id: HtmlEmail.java 785383 2009-06-16 20:36:22Z sgoeschl $
 */
public class MyHtmlEmail extends MultiPartEmail
{
    /** Definition of the length of generated CID's */
    public static final int CID_LENGTH = 10;

    /** prefix for default HTML mail */
    private static final String HTML_MESSAGE_START = "<html><body><pre>";
    /** suffix for default HTML mail */
    private static final String HTML_MESSAGE_END = "</pre></body></html>";


    /**
     * Text part of the message.  This will be used as alternative text if
     * the email client does not support HTML messages.
     */
    protected String text;

    /** Html part of the message */
    protected String html;

    /**
     * @deprecated As of commons-email 1.1, no longer used. Inline embedded
     * objects are now stored in {@link #inlineEmbeds}.
     */
    //protected List inlineImages;

    /**
     * Embedded images Map<String, InlineImage> where the key is the
     * user-defined image name.
     */
    protected Map<String,InlineImage> inlineEmbeds = new HashMap<String,InlineImage>();
    
    protected String contentTransferEncoding;

    public String getContentTransferEncoding() {
		return contentTransferEncoding;
	}

	public void setContentTransferEncoding(String contentTransferEncoding) {
		this.contentTransferEncoding = contentTransferEncoding;
	}

	/**
     * Set the text content.
     *
     * @param aText A String.
     * @return An HtmlEmail.
     * @throws EmailException see javax.mail.internet.MimeBodyPart
     *  for definitions
     * @since 1.0
     */
    public MyHtmlEmail setTextMsg(String aText) throws EmailException
    {
        if (StringUtils.isEmpty(aText))
        {
            throw new EmailException("Invalid message supplied");
        }

        this.text = aText;
        return this;
    }

    /**
     * Set the HTML content.
     *
     * @param aHtml A String.
     * @return An HtmlEmail.
     * @throws EmailException see javax.mail.internet.MimeBodyPart
     *  for definitions
     * @since 1.0
     */
    public MyHtmlEmail setHtmlMsg(String aHtml) throws EmailException
    {
        if (StringUtils.isEmpty(aHtml))
        {
            throw new EmailException("Invalid message supplied");
        }

        this.html = aHtml;
        return this;
    }

    /**
     * Set the message.
     *
     * <p>This method overrides {@link MultiPartEmail#setMsg(String)} in
     * order to send an HTML message instead of a plain text message in
     * the mail body. The message is formatted in HTML for the HTML
     * part of the message; it is left as is in the alternate text
     * part.
     *
     * @param msg the message text to use
     * @return this <code>HtmlEmail</code>
     * @throws EmailException if msg is null or empty;
     * see javax.mail.internet.MimeBodyPart for definitions
     * @since 1.0
     */
    public Email setMsg(String msg) throws EmailException
    {
        if (StringUtils.isEmpty(msg))
        {
            throw new EmailException("Invalid message supplied");
        }

        setTextMsg(msg);

        StringBuffer htmlMsgBuf = new StringBuffer(
            msg.length()
            + HTML_MESSAGE_START.length()
            + HTML_MESSAGE_END.length()
        );

        htmlMsgBuf.append(HTML_MESSAGE_START)
            .append(msg)
            .append(HTML_MESSAGE_END);

        setHtmlMsg(htmlMsgBuf.toString());

        return this;
    }

    /**
     * Attempts to parse the specified <code>String</code> as a URL that will
     * then be embedded in the message.
     *
     * @param urlString String representation of the URL.
     * @param name The name that will be set in the filename header field.
     * @return A String with the Content-ID of the URL.
     * @throws EmailException when URL supplied is invalid or if <code> is null
     * or empty; also see {@link javax.mail.internet.MimeBodyPart} for definitions
     *
     * @see #embed(URL, String)
     * @since 1.1
     */
    public String embed(String urlString, String name) throws EmailException
    {
        try
        {
            return embed(new URL(urlString), name);
        }
        catch (MalformedURLException e)
        {
            throw new EmailException("Invalid URL", e);
        }
    }

    /**
     * Embeds an URL in the HTML.
     *
     * <p>This method embeds a file located by an URL into
     * the mail body. It allows, for instance, to add inline images
     * to the email.  Inline files may be referenced with a
     * <code>cid:xxxxxx</code> URL, where xxxxxx is the Content-ID
     * returned by the embed function. It is an error to bind the same name
     * to more than one URL; if the same URL is embedded multiple times, the
     * same Content-ID is guaranteed to be returned.
     *
     * <p>While functionally the same as passing <code>URLDataSource</code> to
     * {@link #embed(DataSource, String, String)}, this method attempts
     * to validate the URL before embedding it in the message and will throw
     * <code>EmailException</code> if the validation fails. In this case, the
     * <code>HtmlEmail</code> object will not be changed.
     *
     * <p>
     * NOTE: Clients should take care to ensure that different URLs are bound to
     * different names. This implementation tries to detect this and throw
     * <code>EmailException</code>. However, it is not guaranteed to catch
     * all cases, especially when the URL refers to a remote HTTP host that
     * may be part of a virtual host cluster.
     *
     * @param url The URL of the file.
     * @param name The name that will be set in the filename header
     * field.
     * @return A String with the Content-ID of the file.
     * @throws EmailException when URL supplied is invalid or if <code> is null
     * or empty; also see {@link javax.mail.internet.MimeBodyPart} for definitions
     * @since 1.0
     */
    public String embed(URL url, String name) throws EmailException
    {
        if (StringUtils.isEmpty(name))
        {
            throw new EmailException("name cannot be null or empty");
        }

        // check if a URLDataSource for this name has already been attached;
        // if so, return the cached CID value.
        if (inlineEmbeds.containsKey(name))
        {
            InlineImage ii = (InlineImage) inlineEmbeds.get(name);
            URLDataSource urlDataSource = (URLDataSource) ii.getDataSource();
            // make sure the supplied URL points to the same thing
            // as the one already associated with this name.
            // NOTE: Comparing URLs with URL.equals() is a blocking operation
            // in the case of a network failure therefore we use
            // url.toExternalForm().equals() here.
            if (url.toExternalForm().equals(urlDataSource.getURL().toExternalForm()))
            {
                return ii.getCid();
            }
            else
            {
                throw new EmailException("embedded name '" + name
                    + "' is already bound to URL " + urlDataSource.getURL()
                    + "; existing names cannot be rebound");
            }
        }

        // verify that the URL is valid
        InputStream is = null;
        try
        {
            is = url.openStream();
        }
        catch (IOException e)
        {
            throw new EmailException("Invalid URL", e);
        }
        finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            }
            catch (IOException ioe)
            { /* sigh */ }
        }

        return embed(new URLDataSource(url), name);
    }

    /**
     * Embeds a file in the HTML. This implementation delegates to
     * {@link #embed(File, String)}.
     *
     * @param file The <code>File</code> object to embed
     * @return A String with the Content-ID of the file.
     * @throws EmailException when the supplied <code>File</code> cannot be
     * used; also see {@link javax.mail.internet.MimeBodyPart} for definitions
     *
     * @see #embed(File, String)
     * @since 1.1
     */
    public String embed(File file) throws EmailException
    {
        String cid = randomAlphabetic(HtmlEmail.CID_LENGTH).toLowerCase();
        return embed(file, cid);
    }

    /**
     * Embeds a file in the HTML.
     *
     * <p>This method embeds a file located by an URL into
     * the mail body. It allows, for instance, to add inline images
     * to the email.  Inline files may be referenced with a
     * <code>cid:xxxxxx</code> URL, where xxxxxx is the Content-ID
     * returned by the embed function. Files are bound to their names, which is
     * the value returned by {@link java.io.File#getName()}. If the same file
     * is embedded multiple times, the same CID is guaranteed to be returned.
     *
     * <p>While functionally the same as passing <code>FileDataSource</code> to
     * {@link #embed(DataSource, String, String)}, this method attempts
     * to validate the file before embedding it in the message and will throw
     * <code>EmailException</code> if the validation fails. In this case, the
     * <code>HtmlEmail</code> object will not be changed.
     *
     * @param file The <code>File</code> to embed
     * @param cid the Content-ID to use for the embedded <code>File</code>
     * @return A String with the Content-ID of the file.
     * @throws EmailException when the supplied <code>File</code> cannot be used
     *  or if the file has already been embedded;
     *  also see {@link javax.mail.internet.MimeBodyPart} for definitions
     * @since 1.1
     */
    public String embed(File file, String cid) throws EmailException
    {
        if (StringUtils.isEmpty(file.getName()))
        {
            throw new EmailException("file name cannot be null or empty");
        }

        // verify that the File can provide a canonical path
        String filePath = null;
        try
        {
            filePath = file.getCanonicalPath();
        }
        catch (IOException ioe)
        {
            throw new EmailException("couldn't get canonical path for "
                    + file.getName(), ioe);
        }

        // check if a FileDataSource for this name has already been attached;
        // if so, return the cached CID value.
        if (inlineEmbeds.containsKey(file.getName()))
        {
            InlineImage ii = (InlineImage) inlineEmbeds.get(file.getName());
            FileDataSource fileDataSource = (FileDataSource) ii.getDataSource();
            // make sure the supplied file has the same canonical path
            // as the one already associated with this name.
            String existingFilePath = null;
            try
            {
                existingFilePath = fileDataSource.getFile().getCanonicalPath();
            }
            catch (IOException ioe)
            {
                throw new EmailException("couldn't get canonical path for file "
                        + fileDataSource.getFile().getName()
                        + "which has already been embedded", ioe);
            }
            if (filePath.equals(existingFilePath))
            {
                return ii.getCid();
            }
            else
            {
                throw new EmailException("embedded name '" + file.getName()
                    + "' is already bound to file " + existingFilePath
                    + "; existing names cannot be rebound");
            }
        }

        // verify that the file is valid
        if (!file.exists())
        {
            throw new EmailException("file " + filePath + " doesn't exist");
        }
        if (!file.isFile())
        {
            throw new EmailException("file " + filePath + " isn't a normal file");
        }
        if (!file.canRead())
        {
            throw new EmailException("file " + filePath + " isn't readable");
        }

        return embed(new FileDataSource(file), file.getName());
    }

    /**
     * Embeds the specified <code>DataSource</code> in the HTML using a
     * randomly generated Content-ID. Returns the generated Content-ID string.
     *
     * @param dataSource the <code>DataSource</code> to embed
     * @param name the name that will be set in the filename header field
     * @return the generated Content-ID for this <code>DataSource</code>
     * @throws EmailException if the embedding fails or if <code>name</code> is
     * null or empty
     * @see #embed(DataSource, String, String)
     * @since 1.1
     */
    public String embed(DataSource dataSource, String name) throws EmailException
    {
        // check if the DataSource has already been attached;
        // if so, return the cached CID value.
        if (inlineEmbeds.containsKey(name))
        {
            InlineImage ii = (InlineImage) inlineEmbeds.get(name);
            // make sure the supplied URL points to the same thing
            // as the one already associated with this name.
            if (dataSource.equals(ii.getDataSource()))
            {
                return ii.getCid();
            }
            else
            {
                throw new EmailException("embedded DataSource '" + name
                    + "' is already bound to name " + ii.getDataSource().toString()
                    + "; existing names cannot be rebound");
            }
        }

        String cid = randomAlphabetic(HtmlEmail.CID_LENGTH).toLowerCase();
        return embed(dataSource, name, cid);
    }

    /**
     * Embeds the specified <code>DataSource</code> in the HTML using the
     * specified Content-ID. Returns the specified Content-ID string.
     *
     * @param dataSource the <code>DataSource</code> to embed
     * @param name the name that will be set in the filename header field
     * @param cid the Content-ID to use for this <code>DataSource</code>
     * @return the supplied Content-ID for this <code>DataSource</code>
     * @throws EmailException if the embedding fails or if <code>name</code> is
     * null or empty
     * @since 1.1
     */
    public String embed(DataSource dataSource, String name, String cid)
    throws EmailException
    {
    	if (StringUtils.isEmpty(name))
    	{
    		throw new EmailException("name cannot be null or empty");
    	}

    	MimeBodyPart mbp = new MimeBodyPart();

    	try
    	{
    		mbp.setDataHandler(new DataHandler(dataSource));
    		mbp.setFileName(name);
    		mbp.setDisposition("inline");
    		mbp.setContentID("<" + cid + ">");
    		
    		InlineImage ii = new InlineImage(cid, dataSource, mbp);
    		this.inlineEmbeds.put(name, ii);

    		return cid;
    	}
    	catch (MessagingException me)
    	{
    		throw new EmailException(me);
    	}
    }

    public String embed(DataSource dataSource, String name, String nameCharset, String cid)
        throws EmailException
    {
        if (StringUtils.isEmpty(name))
        {
            throw new EmailException("name cannot be null or empty");
        }

        MimeBodyPart mbp = new MimeBodyPart();

        try
        {
            mbp.setDataHandler(new DataHandler(dataSource));
            Util.setFileName(mbp, name, nameCharset, null);
            //mbp.setFileName(name);
            mbp.setDisposition("inline");
            mbp.setContentID("<" + cid + ">");
            
            InlineImage ii = new InlineImage(cid, dataSource, mbp);
            this.inlineEmbeds.put(name, ii);

            return cid;
        }
        catch (MessagingException me)
        {
            throw new EmailException(me);
        }
    }

    /**
     * Does the work of actually building the email.
     *
     * @exception EmailException if there was an error.
     * @since 1.0
     */
    public void buildMimeMessage() throws EmailException
    {
        try
        {
            build();
        }
        catch (MessagingException me)
        {
            throw new EmailException(me);
        }
        super.buildMimeMessage();
    }

    /**
     * @throws EmailException EmailException
     * @throws MessagingException MessagingException
     */
    private void build() throws MessagingException, EmailException
    {
        MimeMultipart rootContainer = this.getContainer();
        MimeMultipart bodyEmbedsContainer = rootContainer;
        MimeMultipart bodyContainer = rootContainer;
        BodyPart msgHtml = null;
        BodyPart msgText = null;

        rootContainer.setSubType("mixed");

        // determine how to form multiparts of email

        if (StringUtils.isNotEmpty(this.html) && this.inlineEmbeds.size() > 0)
        {
            //If HTML body and embeds are used, create a related container and add it to the root container
            bodyEmbedsContainer = new MimeMultipart("related");
            bodyContainer = bodyEmbedsContainer;
            this.addPart(bodyEmbedsContainer, 0);

            //If TEXT body was specified, create a alternative container and add it to the embeds container
            if (StringUtils.isNotEmpty(this.text))
            {
                bodyContainer = new MimeMultipart("alternative");
                BodyPart bodyPart = createBodyPart();
                try
                {
                    bodyPart.setContent(bodyContainer);
                    bodyEmbedsContainer.addBodyPart(bodyPart, 0);
                }
                catch (MessagingException me)
                {
                    throw new EmailException(me);
                }
            }
        }
        else if (StringUtils.isNotEmpty(this.text) && StringUtils.isNotEmpty(this.html))
        {
            //If both HTML and TEXT bodies are provided, create a alternative container and add it to the root container
            bodyContainer = new MimeMultipart("alternative");
            this.addPart(bodyContainer, 0);
        }

        if (StringUtils.isNotEmpty(this.html))
        {
            msgHtml = new MimeBodyPart();
            bodyContainer.addBodyPart(msgHtml, 0);

            // apply default charset if one has been set
            if (StringUtils.isNotEmpty(this.charset))
            {
                msgHtml.setContent(
                    this.html,
                    Email.TEXT_HTML + "; charset=" + this.charset);
            }
            else
            {
                msgHtml.setContent(this.html, Email.TEXT_HTML);
            }
    		if(contentTransferEncoding!=null){
    			msgHtml.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
    		}

            Iterator<InlineImage> iter = this.inlineEmbeds.values().iterator();
            while (iter.hasNext())
            {
                InlineImage ii = (InlineImage) iter.next();
                bodyEmbedsContainer.addBodyPart(ii.getMbp());
            }
        }

        if (StringUtils.isNotEmpty(this.text))
        {
            msgText = new MimeBodyPart();
            bodyContainer.addBodyPart(msgText, 0);

            // apply default charset if one has been set
            if (StringUtils.isNotEmpty(this.charset))
            {
                msgText.setContent(
                    this.text,
                    Email.TEXT_PLAIN + "; charset=" + this.charset);
            }
            else
            {
                msgText.setContent(this.text, Email.TEXT_PLAIN);
            }
    		if(contentTransferEncoding!=null){
    			msgText.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
    		}
        }
    }

    /**
     * Private bean class that encapsulates data about URL contents
     * that are embedded in the final email.
     * @since 1.1
     */
    private static class InlineImage
    {
        /** content id */
        private String cid;
        /** <code>DataSource</code> for the content */
        private DataSource dataSource;
        /** the <code>MimeBodyPart</code> that contains the encoded data */
        private MimeBodyPart mbp;

        /**
         * Creates an InlineImage object to represent the
         * specified content ID and <code>MimeBodyPart</code>.
         * @param cid the generated content ID
         * @param dataSource the <code>DataSource</code> that represents the content
         * @param mbp the <code>MimeBodyPart</code> that contains the encoded
         * data
         */
        public InlineImage(String cid, DataSource dataSource, MimeBodyPart mbp)
        {
            this.cid = cid;
            this.dataSource = dataSource;
            this.mbp = mbp;
        }

        /**
         * Returns the unique content ID of this InlineImage.
         * @return the unique content ID of this InlineImage
         */
        public String getCid()
        {
            return cid;
        }

        /**
         * Returns the <code>DataSource</code> that represents the encoded content.
         * @return the <code>DataSource</code> representing the encoded content
         */
        public DataSource getDataSource()
        {
            return dataSource;
        }

        /**
         * Returns the <code>MimeBodyPart</code> that contains the
         * encoded InlineImage data.
         * @return the <code>MimeBodyPart</code> containing the encoded
         * InlineImage data
         */
        public MimeBodyPart getMbp()
        {
            return mbp;
        }

        // equals()/hashCode() implementations, since this class
        // is stored as a entry in a Map.
        /**
         * {@inheritDoc}
         * @return true if the other object is also an InlineImage with the same cid.
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof InlineImage))
            {
                return false;
            }

            InlineImage that = (InlineImage) obj;

            return this.cid.equals(that.cid);
        }

        /**
         * {@inheritDoc}
         * @return the cid hashCode.
         */
        public int hashCode()
        {
            return cid.hashCode();
        }
    }
    
    private static final Random RANDOM = new Random();
    public static String randomAlphabetic(int count)
    {
        return random(count, 0, 0, true, false, null, RANDOM);
    }
    private static String random(int count, int start, int end, boolean letters, boolean numbers, char chars[], Random random)
    {
        if(count == 0)
            return "";
        if(count < 0)
            throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
        if(start == 0 && end == 0)
        {
            end = 123;
            start = 32;
            if(!letters && !numbers)
            {
                start = 0;
                end = 2147483647;
            }
        }
        StringBuffer buffer = new StringBuffer();
        int gap = end - start;
        while(count-- != 0) 
        {
            char ch;
            if(chars == null)
                ch = (char)(random.nextInt(gap) + start);
            else
                ch = chars[random.nextInt(gap) + start];
            if(letters && numbers && Character.isLetterOrDigit(ch) || letters && Character.isLetter(ch) || numbers && Character.isDigit(ch) || !letters && !numbers)
                buffer.append(ch);
            else
                count++;
        }
        return buffer.toString();
    }
    
    @Override
    public void setCharset(String newCharset)
    {
    	this.charset = newCharset;
    }
}
