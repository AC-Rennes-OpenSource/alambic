/*******************************************************************************
 * Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.jobs.load;

import fr.gouv.education.acrennes.alambic.audit.persistence.AuditEntity;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class XmlToEmail extends AbstractDestination {

    protected static final Log log = LogFactory.getLog(XmlToEmail.class);

    private final String DEFAULT_SMTP_PORT = "25";
    // private final String DEFAULT_SMTP_CHARSET = Charsets.UTF_8.toString();
    private final Session session;
    private final Element pivot;
    private List<Element> attachments;
    private int count;
    private EntityManager em;

    public XmlToEmail(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
        super(context, job, jobActivity);

        String host = job.getChildText("mail.smtp.host");
        if (StringUtils.isBlank(host)) {
            throw new AlambicException("the smtp host MUST be specified");
        } else {
            host = context.resolveString(host);
        }

        String port = job.getChildText("mail.smtp.port");
        if (StringUtils.isBlank(port)) {
            throw new AlambicException("le port n'est pas precise");
        } else {
            port = context.resolveString(port);
        }

        String pivot = job.getChildText("pivot");
        if (pivot == null) {
            throw new AlambicException("le pivot n'est pas precise");
        } else {
            pivot = context.resolvePath(pivot);
        }

        InputSource fPivot = new InputSource(pivot);
        try {
            this.pivot = (new SAXBuilder()).build(fPivot).getRootElement();
        } catch (JDOMException | IOException e) {
            throw new AlambicException(e.getMessage());
        }

        attachments = null;
        Element attmts = job.getChild("attachments");
        if (null != attmts) {
            List<Element> includes = attmts.getChildren("include");
            if (null != includes && 0 < includes.size()) {
                attachments = new ArrayList<>();
                attachments.addAll(includes);
            }
        }

        count = 0;
        em = EntityManagerHelper.getEntityManager();

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", (StringUtils.isNotBlank(port)) ? port : DEFAULT_SMTP_PORT);
        // properties.setProperty("mail.mime.charset", DEFAULT_SMTP_CHARSET);
        session = Session.getDefaultInstance(properties);
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    @Override
    public void execute() {
        List<Element> messagesNode = pivot.getChild("messages").getChildren();
        for (Element messageNode : messagesNode) {
            // activity monitoring
            jobActivity.setProgress(((count + 1) * 100) / messagesNode.size());
            jobActivity.setProcessing("processing entry " + (count + 1) + "/" + messagesNode.size());

            String notificationlog = messageNode.getChild("log").getChildText("value");
            MimeMessage message = new MimeMessage(session);

            try {
                // from
                String from = messageNode.getChild("from").getChildText("value");
                message.setFrom(new InternetAddress(from));

                // multiple recipients with different types (TO, CC, BCC)
                Element recipientsNode = messageNode.getChild("recipients");
                for (Element itemNode : recipientsNode.getChildren("item")) {
                    message.addRecipient(getRecipientType(itemNode), new InternetAddress(itemNode.getChildText("value")));
                }

                // subject
                String subject = messageNode.getChild("subject").getChildText("value");
                message.setSubject(subject);

                // content
                Element content = messageNode.getChild("content");
                if (null != attachments && 0 < attachments.size()) {
                    MimeMultipart multipart = new MimeMultipart("related");

                    // Add the email content
                    BodyPart messageBodyPart = new MimeBodyPart();
                    String contentvalue = Functions.getInstance().executeAllFunctions(context.resolveString(content.getChildText("value")));
                    if (StringUtils.isBlank(contentvalue)) {
                        throw new AlambicException("Message content cannot be empty");
                    }
                    messageBodyPart.setContent(contentvalue, content.getAttributeValue("type"));
                    multipart.addBodyPart(messageBodyPart);

                    // Add attachements
                    for (Element attachement : attachments) {
                        messageBodyPart = new MimeBodyPart();
                        DataSource fds = new FileDataSource(attachement.getTextTrim());
                        messageBodyPart.setDataHandler(new DataHandler(fds));
                        messageBodyPart.setHeader("Content-ID", attachement.getAttributeValue("id"));
                        multipart.addBodyPart(messageBodyPart);
                    }

                    // put everything together
                    message.setContent(multipart);
                } else {
                    // content with type
                    String contentvalue = Functions.getInstance().executeAllFunctions(context.resolveString(content.getChildText("value")));
                    if (StringUtils.isBlank(contentvalue)) {
                        throw new AlambicException("Message content cannot be empty");
                    }
                    message.setContent(contentvalue, content.getAttributeValue("type"));
                }
            } catch (Exception e) {
                jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                log.error("Notification has failed (build error), error: " + e.getMessage() + ", purpose: " + notificationlog);
                break;
            }

            // send
            try {
                Transport.send(message);
                log.info("Notification was sent successfully, recipients : " + getRecipients(message) + ", purpose : " + notificationlog);

                // persist sending operation into audit logs
                audit(message);
            } catch (MessagingException e) {
                jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                log.error("Notification has failed (send error), error: " + e.getMessage() + ", purpose: " + notificationlog);
            }

            setCount(count + 1);
        }
    }

    private void audit(final MimeMessage message) throws MessagingException {
        /* prepare fulltext audit log */
        String fulltext = getRecipients(message);
        fulltext = fulltext.concat(",\"Subject\":\"" + message.getSubject() + "\"");

        /* persist */
        EntityTransaction transac = em.getTransaction();
        transac.begin();
        em.persist(new AuditEntity(getType(), "{" + fulltext.replaceFirst("^,", "") + "}", job));
        transac.commit();
    }

    private Message.RecipientType getRecipientType(final Element item) {
        Message.RecipientType recipientType = Message.RecipientType.TO;

        String type = item.getAttributeValue("type");
        if (type.equalsIgnoreCase(Message.RecipientType.TO.toString())) {
            recipientType = Message.RecipientType.TO;
        } else if (type.equalsIgnoreCase(Message.RecipientType.CC.toString())) {
            recipientType = Message.RecipientType.CC;
        } else if (type.equalsIgnoreCase(Message.RecipientType.BCC.toString())) {
            recipientType = Message.RecipientType.BCC;
        } else {
            log.warn("Notification: unknown recipient type '" + type + "', used the default type 'To' instead.");
        }

        return recipientType;
    }

    private String getRecipients(final MimeMessage message) throws MessagingException {
        String recipientsJson = "";

        List<String> recipientsList = new ArrayList<>();
        Address[] recipients = message.getRecipients(RecipientType.TO);
        if (null != recipients && 0 < recipients.length) {
            for (Address recipient : recipients) {
                recipientsList.add(recipient.toString());
            }
            recipientsJson = recipientsJson.concat(",\"To\":[\"" + StringUtils.join(recipientsList, "\",\"") + "\"]");
        }

        recipients = message.getRecipients(RecipientType.CC);
        if (null != recipients && 0 < recipients.length) {
            for (Address recipient : recipients) {
                recipientsList.add(recipient.toString());
            }
            recipientsJson = recipientsJson.concat(",\"Cc\":[\"" + StringUtils.join(recipientsList, "\",\"") + "\"]");
        }

        recipients = message.getRecipients(RecipientType.BCC);
        if (null != recipients && 0 < recipients.length) {
            for (Address recipient : recipients) {
                recipientsList.add(recipient.toString());
            }
            recipientsJson = recipientsJson.concat(",\"Bcc\":[\"" + StringUtils.join(recipientsList, "\",\"") + "\"]");
        }

        return recipientsJson.replaceFirst("^,", "");
    }

    @Override
    public void close() throws AlambicException {
        super.close();
        if (null != em) {
            em.close();
            em = null;
        }
    }

}
