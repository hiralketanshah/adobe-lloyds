package com.lloyds.assets.core.workflows;

import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.lloyds.assets.core.utils.AssetsHelper;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * CreateAssetVersionImpl
 * <p>
 * This implementation will aid in creation of Asset versioning of multiple assets version of same files.
 * </p>
 *
 * @author sumchakr
 * @since 2020-10-16
 */

@Component(service = WorkflowProcess.class, property = {"process.label=LBG Create Versioning"})
public class CreateAssetVersionImpl implements WorkflowProcess {

  /**
   * LOGGER
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateAssetVersionImpl.class);

  /**
   * The resource resolver factory.
   */
  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
      throws WorkflowException {

    String payload = workItem.getWorkflowData().getPayload().toString();
    Session session = workflowSession.adaptTo(Session.class);
    try (ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(
        Collections.singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session))) {
      Resource payloadRes = resourceResolver.getResource(payload);
      Resource parentFolderRes = payloadRes.getParent();
      List<String> versionResList = new ArrayList<>();
      if(parentFolderRes.getValueMap().get("jcr:primaryType", String.class).equals("sling:Folder")){
        Iterator<Resource> itr = parentFolderRes.listChildren();
        while (itr.hasNext()){
          Resource childRes = itr.next();
          if (!childRes.getName().equals("jcr:content")){
            versionResList.add(childRes.getName());
          }
        }
        Collections.sort(versionResList, Collections.reverseOrder());
        AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
        for(int i = 0; i< versionResList.size() -1 ; i++){
          Resource newerAssetRes = resourceResolver.getResource(parentFolderRes.getPath() + "/" + versionResList.get(i+1));
          AssetsHelper.createRevision(resourceResolver, assetManager, parentFolderRes.getPath() + "/" + versionResList.get(i), newerAssetRes,
                                      "Versioning  POC");
        }
        if (resourceResolver.hasChanges()) {
          resourceResolver.commit();
        }
      }

    } catch (LoginException | PersistenceException | RepositoryException e) {
      LOGGER.error("Exception while creation of Versioning");
      throw new WorkflowException("Exception while creation of Versioning", e);
    }
  }
}
