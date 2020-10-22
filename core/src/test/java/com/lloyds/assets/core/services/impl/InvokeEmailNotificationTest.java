package com.lloyds.assets.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.adobe.granite.taskmanagement.TaskEvent;
import com.day.cq.commons.Externalizer;
import com.day.cq.mailer.MessageGatewayService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.lucene.queries.function.valuesource.MultiFunction.Values;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Value;

/**
 * InvokeEmailNotificationTest
 * <p>
 * Test Class for InvokeEmailNotification
 * </p>
 *
 * @author sumchakr
 * @since 2020-10-22
 */
@ExtendWith(AemContextExtension.class)
public class InvokeEmailNotificationTest {

  private final AemContext context = new AemContextBuilder(ResourceResolverType.RESOURCERESOLVER_MOCK).build();

  @InjectMocks
  private InvokeEmailNotification invokeEmailNotification;

  private Event event;

  @Mock
  ResourceResolverFactory resolverFactory;

  @Mock
  private Externalizer externalizer;

  @Mock
  private MessageGatewayService messageGatewayService;

  @Mock
  Resource resource;

  @Mock
  ResourceResolver resourceResolver;

  @Mock
  UserManager userManager;

  @Mock
  Authorizable authorizable;

   Externalizer externalizerMock;
  MessageGatewayService messageGatewayServiceMock;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    context.registerService(ResourceResolverFactory.class, resolverFactory);
    when(resolverFactory.getServiceResourceResolver(org.mockito.ArgumentMatchers.anyMap()))
        .thenReturn(resourceResolver);
    when(resolverFactory.getServiceResourceResolver(Mockito.anyMap())).thenReturn(resourceResolver);
    when(resourceResolver.getResource(Mockito.any(String.class))).thenReturn(resource);
    when(resourceResolver.adaptTo(UserManager.class)).thenReturn(userManager);
    when(resource.getPath()).thenReturn("templatePath");

    when(userManager.getAuthorizable(Mockito.anyString())).thenReturn(authorizable);
    when(authorizable.isGroup()).thenReturn(true);
    when(authorizable.hasProperty(Mockito.anyString())).thenReturn(false);



  }

  @Test
  void testHandleEvent_to_Expire() {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("TaskTypeName", "Notification");
    properties.put("TaskId", "2020-10-20/assets_about_to_expire");
    event = new Event(TaskEvent.TOPIC, properties);
    invokeEmailNotification.handleEvent(event);
  }


  @Test
  void testHandleEvent_Expired() {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("TaskTypeName", "Notification");
    properties.put("TaskId", "2020-10-20/assets_expired");
    event = new Event(TaskEvent.TOPIC, properties);
    invokeEmailNotification.handleEvent(event);
  }

  @Test
  void testHandleEvent_Subassets() {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("TaskTypeName", "Notification");
    properties.put("TaskId", "2020-10-20/subassets_expired");
    event = new Event(TaskEvent.TOPIC, properties);
    invokeEmailNotification.handleEvent(event);
  }

}
