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
import org.apache.commons.mail.HtmlEmail;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.dea.DEAConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
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
@Designate(ocd = InvokeEmailNotification.Configuration.class)
public class InvokeEmailNotification implements EventHandler {

  /**
   * The logging facility.
   */
  private static final Logger log = LoggerFactory.getLogger(InvokeEmailNotification.class);
  private static final String NOTIFICATION = "Notification";
  private static final String PN_USER_EMAIL = "profile/email";
  private String groupName;
  private String recieverName;
  private String senderName;


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
      ResourceResolver resourceResolver = AssetsHelper.getResourceResolver(resourceResolverFactory);
      // Avoid processing if not a task event
      String topic = event.getTopic();
      if (TaskEvent.TOPIC.equals(topic) && StringUtils
          .equals(event.getProperty("TaskTypeName").toString(), NOTIFICATION)) {
        String taskId = event.getProperty("TaskId").toString();
        Resource taskRes = resourceResolver.getResource("/var/taskmanagement/tasks/" + taskId);
        if (null != taskRes) {
          if (StringUtils.contains(taskId, "assets_about_to_expire")) {
            log.info("Send email notification for Assets is Expiring");
            sendEmailNotification("ASSET_EXPIRY_PRIOR", resourceResolver, taskRes);
          } else if (StringUtils.contains(taskId, "assets_expired")) {
            log.info("Send email notification for Expired assets");
            sendEmailNotification("ASSET_EXPIRY", resourceResolver, taskRes);
          } else if (StringUtils.contains(taskId, "subassets_expired")) {
            log.info("Send email notification for Expired subassets");
            sendEmailNotification("SUBASSET_EXPIRY", resourceResolver, taskRes);
          }
        }
      } else {
        log.debug("skipping non-task event: {}", event.toString());
      }
    } catch (LoginException | IOException | RepositoryException e) {
      log.error("Unable to process the notification event due to {} " , e.getMessage() , e);
    } catch (EmailException | MessagingException e) {
      log.error("Unable to send the mail event due to {} " , e.getMessage() , e);
    }
  }

  @Activate
  @Modified
  protected void activate(InvokeEmailNotification.Configuration schedulerConfiguration) {
    this.groupName = schedulerConfiguration.groupName();
    this.recieverName = schedulerConfiguration.recieverName();
    this.senderName = schedulerConfiguration.senderName();
  }

  @Deactivate
  protected void deactivate(InvokeEmailNotification.Configuration schedulerConfiguration) {
    this.groupName = null;
    this.recieverName = null;
    this.senderName = null;
  }

  private String getEmailId(ResourceResolver resourceResolver) throws RepositoryException {
    UserManager userManager = resourceResolver.adaptTo(UserManager.class);
    String currEmail = "";
    Authorizable authorizable = userManager.getAuthorizable(groupName);
    if (authorizable != null) {
      // check if it is a group
      if (authorizable.isGroup()) {
        currEmail = getAuthorizableEmail(authorizable);
      }
    }
    return currEmail;
  }

  private static String getAuthorizableEmail(Authorizable authorizable) throws RepositoryException {
    if (authorizable.hasProperty(PN_USER_EMAIL)) {
      Value[] emailVal = authorizable.getProperty(PN_USER_EMAIL);
      return emailVal[0].getString();
    }
    return null;
  }

  /* Helper function for sending email notification for asset or reference expiry
   */

  private void sendEmailNotification(String expiringEntity, ResourceResolver notificationResolver, Resource taskRes)
      throws RepositoryException,
      MailingException, IOException, LoginException, EmailException, MessagingException {

    Map<String, String> parameters = new HashMap<String, String>();
    String templateResPath = null;
    String subject = "";
    if (expiringEntity.equals("ASSET_EXPIRY_PRIOR")) {
      templateResPath = getTemplateResourcePath("ASSET_EXPIRY_PRIOR", notificationResolver);
      subject = "Some of your assets are about to expire" ;
    } else if (expiringEntity.equals("ASSET_EXPIRY")) {
      templateResPath = getTemplateResourcePath("ASSET_EXPIRY", notificationResolver);
      subject = "Some of your assets have expired" ;
    } else {
      templateResPath = getTemplateResourcePath("SUBASSET_EXPIRY", notificationResolver);
      subject = "Some of your sub assets have expired" ;
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
    String senderEmail = getEmailId(notificationResolver);
    if (StringUtils.isEmpty(senderEmail)) {
      return;
    }
    parameters.put("hostUserFullName", senderName);
    String assetsLink = externalizer
        .externalLink(notificationResolver, Externalizer.AUTHOR, "http", "/assets.html/content/dam");
    parameters.put("assetlink", assetsLink);
    parameters.put("assetpaths", taskRes.getValueMap().get("description", String.class));
    parameters.put("inviteeFirstName", recieverName);
    parameters.put("time", new Date().toString());

    MailTemplate mailTemplate = MailTemplate.create(templateResPath, notificationResolver.adaptTo(Session.class));
    Email email = mailTemplate.getEmail(StrLookup.mapLookup(parameters), HtmlEmail.class);
    email.setSubject(subject);
    email.setTo(Collections.singleton(new InternetAddress(senderEmail)));
    email.setFrom(senderEmail);
    MessageGateway messageGateway = messageGatewayService.getGateway(email.getClass());
    messageGateway.send(email);
  }

  /*
   * template lookup-helper
   * returns looked-up template resource's path, attempting to locate in legacy location
   * followed by consulting Sling CA Configuration Resolver
   * */
  private String getTemplateResourcePath(String actionIdentifier, ResourceResolver notificationResolver) {
    String templatePath = "";
    if (StringUtils.equals("ASSET_EXPIRY_PRIOR", actionIdentifier)) {
      templatePath = "/apps/llyodassets/emailtemplates/asset-prior-expiry/en.txt";
    } else if (StringUtils.equals("ASSET_EXPIRY", actionIdentifier)) {
      templatePath = "/apps/llyodassets/emailtemplates/assets-expired/en.txt";
    } else if (StringUtils.equals("SUBASSET_EXPIRY", actionIdentifier)) {
      templatePath = "/apps/llyodassets/emailtemplates/subassets-expired/en.txt";
    }
    // locate legacy template first
    Resource resource = notificationResolver.getResource(templatePath);
    return (null != resource) ? resource.getPath() : null;
  }

  @ObjectClassDefinition(name = "Lloyd Assets Asset Expiry/Expired Notification Config")
  public @interface Configuration {

    @AttributeDefinition(name = "Notification Reciever group name", description = "Name of group to recieve notification", type = AttributeType.STRING)
    public String groupName() default "test";

    @AttributeDefinition(name = "Reciever Name", description = "Reciever Name", type = AttributeType.STRING)
    String recieverName() default "Lloyds Asset Admin";

    @AttributeDefinition(name = "Reciever Name", description = "Sender Name", type = AttributeType.STRING)
    String senderName() default "AEM TEAM";

  }

}
