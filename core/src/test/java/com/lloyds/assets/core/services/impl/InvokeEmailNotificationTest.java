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
  private  InvokeEmailNotification invokeEmailNotification;

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

  Externalizer externalizerMock;
  MessageGatewayService messageGatewayServiceMock;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    context.registerService(ResourceResolverFactory.class, resolverFactory);
    when(resolverFactory.getServiceResourceResolver(org.mockito.ArgumentMatchers.anyMap())).thenReturn(resourceResolver);
    Mockito.doReturn(resourceResolver).when(resolverFactory).getServiceResourceResolver(Mockito.anyMap());
    Mockito.when(resourceResolver.getResource(Mockito.any(String.class))).thenReturn(resource);

  }

  @Test
  void testHandleEvent(){
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("TaskTypeName", "Notification");
    properties.put("TaskId", "2020-10-20/assets_about_to_expire");
    event = new Event(TaskEvent.TOPIC, properties);
    invokeEmailNotification.handleEvent(event);
  }

}
