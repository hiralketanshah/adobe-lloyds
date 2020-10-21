package com.lloyds.assets.core.services.impl;

import com.adobe.granite.taskmanagement.TaskEvent;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MailingException;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.lloyds.assets.core.utils.AssetsHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.dea.DEAConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

/**
 * InvokeEmailNotification
 * <p>
 * This class reads the task notifications and sends email
 * </p>
 *
 * @author sumchakr
 * @since 2020-10-19
 */

@Component(
    immediate = true, service = EventHandler.class,
    property = {
        EventConstants.EVENT_FILTER + "(!(" + DEAConstants.PROPERTY_APPLICATION + "=*))",
        EventConstants.EVENT_TOPIC + "=" + TaskEvent.TOPIC,
    })
public class InvokeEmailNotification implements EventHandler {

  /**
   * The logging facility.
   */
  private static final Logger log = LoggerFactory.getLogger(InvokeEmailNotification.class);
  private static final String NOTIFICATION = "Notification";

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Reference
  private Externalizer externalizer;

  @Reference
  private MessageGatewayService messageGatewayService;

  @Override
  public void handleEvent(Event event) {
    log.info("Entry :: handleEvent() method .......");

    try {
      ResourceResolver notificationResolver = AssetsHelper.getResourceResolver(resourceResolverFactory);
      // Avoid processing if not a task event
      String topic = event.getTopic();
      if (TaskEvent.TOPIC.equals(topic) && StringUtils
          .equals(event.getProperty("TaskTypeName").toString(), NOTIFICATION)) {
        String taskId = event.getProperty("TaskId").toString();
        Resource taskRes = notificationResolver.getResource("/var/taskmanagement/tasks/" + taskId);
        if (null != taskRes) {
          if (StringUtils.contains(taskId, "assets_about_to_expire")) {
            log.info("Send email notification for Assets is Expiring");
            sendEmailNotification("ASSET_EXPIRY_PRIOR", notificationResolver, taskRes);
          } else if (StringUtils.contains(taskId, "assets_expired")) {
            log.info("Send email notification for Expired assets");
            sendEmailNotification("ASSET_EXPIRY", notificationResolver, taskRes);
          } else if (StringUtils.contains(taskId, "subassets_expired")) {
            log.info("Send email notification for Expired subassets");
            sendEmailNotification("SUBASSET_EXPIRY", notificationResolver, taskRes);
          }
        }
      } else {
        log.debug("skipping non-task event: {}", event.toString());
      }
    } catch (LoginException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (EmailException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }



  /* Helper function for sending email notification for asset or reference expiry
   */

  private void sendEmailNotification(String expiringEntity, ResourceResolver notificationResolver, Resource taskRes)
      throws RepositoryException,
      MailingException, IOException, LoginException, EmailException, MessagingException {

    Map<String, String> parameters = new HashMap<String, String>();
    String templateResPath = null;
    if (expiringEntity.equals("ASSET_EXPIRY_PRIOR")) {
      templateResPath = getTemplateResourcePath("ASSET_EXPIRY_PRIOR", notificationResolver);
    } else if (expiringEntity.equals("ASSET_EXPIRY")) {
      templateResPath = getTemplateResourcePath("ASSET_EXPIRY", notificationResolver);
    } else {
      templateResPath = getTemplateResourcePath("SUBASSET_EXPIRY", notificationResolver);
    }
    if (null == templateResPath) {
      if (expiringEntity.equals("ASSET_EXPIRY_PRIOR")) {
        log.warn("Couldn't locate \"Assets About To Expire\" Email notification template, not sending email");
      } else if (expiringEntity.equals("ASSET_EXPIRY")) {
        log.warn("Couldn't locate \"Asset Expiry Email\" notification template, not sending email");
      } else {
        log.warn("Couldn't locate \"Assets Reference Expiry\" Email notification template, not sending email");
      }
      return;
    }
    parameters.put("hostUserFullName", "AEM TEAM");
    String assetsLink = externalizer
        .externalLink(notificationResolver, Externalizer.AUTHOR, "http", "/assets.html/content/dam");
    parameters.put("assetlink", assetsLink);
    parameters.put("assetpaths", taskRes.getValueMap().get("description", String.class));
    parameters.put("inviteeFirstName", "Lloyds Asset Admin");
    parameters.put("time", new Date().toString());

    String senderEmail = "sumchakr@adobe.com";
    if (senderEmail != null && !senderEmail.equals("")) {
      MailTemplate mailTemplate = MailTemplate.create(templateResPath, notificationResolver.adaptTo(Session.class));
      Email email = mailTemplate.getEmail(StrLookup.mapLookup(parameters), SimpleEmail.class);
      email.setSubject("Some of your assets are expired");
      email.setTo(Collections.singleton(new InternetAddress(senderEmail)));
      email.setFrom(senderEmail);
      MessageGateway messageGateway = messageGatewayService.getGateway(email.getClass());
      messageGateway.send(email);
    }
  }

  /*
   * template lookup-helper
   * returns looked-up template resource's path, attempting to locate in legacy location
   * followed by consulting Sling CA Configuration Resolver
   * */
  private String getTemplateResourcePath(String actionIdentifier, ResourceResolver notificationResolver) {
    String templatePath = "";
    if (StringUtils.equals("ASSET_EXPIRY_PRIOR", actionIdentifier)) {
      templatePath = "/content/llyodassets/emailtemplates/asset-prior-expiry/en.txt";
    } else if (StringUtils.equals("ASSET_EXPIRY", actionIdentifier)) {
      templatePath = "/content/llyodassets/emailtemplates/assets-expired/en.txt";
    } else if (StringUtils.equals("SUBASSET_EXPIRY", actionIdentifier)) {
      templatePath = "/content/llyodassets/emailtemplates/subassets-expired/en.txt";
    }
    // locate legacy template first
    Resource resource = notificationResolver.getResource(templatePath);
    return (null != resource) ? resource.getPath() : null;
  }


}
