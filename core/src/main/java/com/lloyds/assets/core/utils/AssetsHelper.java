package com.lloyds.assets.core.utils;

import com.adobe.granite.asset.api.AssetVersionManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * AssetsHelper
 * <p>
 * Utility Methods for Assets business logics
 * </p>
 *
 * @author sumchakr
 * @since 2020-10-16
 */
public final class AssetsHelper {

  /** LOGGER */
  private static final Logger log = LoggerFactory.getLogger(AssetsHelper.class);
  /** REL_ASSET_METADATA */
  private static final String REL_ASSET_METADATA = "jcr:content/metadata";
  /** REL_ASSET_RENDITIONS */
  private static final String REL_ASSET_RENDITIONS = "jcr:content/renditions";

  /**
   * Constructor.
   */
  private AssetsHelper() {
    // hidden
  }

  /**
   * Creates a new revision of an asset and replaces its renditions (including
   * original).
   *
   * @param resourceResolver
   *            the ResourceResolver object
   * @param assetManager
   *            the AssetManager object
   * @param orginalAssetPath
   *            the orginalAssetPath to create a new version for
   * @param newAsset
   *            the Resource to that will represent the new version
   * @throws PersistenceException
   * @throws RepositoryException
   */
  public static void createRevision(ResourceResolver resourceResolver, com.adobe.granite.asset.api.AssetManager assetManager,
                                    String orginalAssetPath, Resource newAsset, String message)
      throws PersistenceException, RepositoryException {
    Session session = resourceResolver.adaptTo(Session.class);
    if (resourceResolver.hasChanges()) {
      resourceResolver.commit();
    }
    // Create the new version
    AssetVersionManager versionManager = resourceResolver.adaptTo(AssetVersionManager.class);
    versionManager.createVersion(orginalAssetPath, message);

    if (session != null) {
      // Delete the renditions from the old asset
      resourceResolver.delete(
          resourceResolver.getResource(orginalAssetPath + AssetConstants.SLASH + REL_ASSET_RENDITIONS));
      Node originalAssetJcrContentNode = session
          .getNode(orginalAssetPath + AssetConstants.SLASH + JcrConstants.JCR_CONTENT);
      Node newAssetRenditionsNode = session
          .getNode(newAsset.getPath() + AssetConstants.SLASH + REL_ASSET_RENDITIONS);
      JcrUtil.copy(newAssetRenditionsNode, originalAssetJcrContentNode, null);
      JcrUtil.setProperty(originalAssetJcrContentNode, JcrConstants.JCR_LASTMODIFIED, new Date());
      assetManager.removeAsset(newAsset.getPath());
    }

  }

  /**
   * Return resource resolver.
   *
   * @param resolverFactory
   * @return ResourceResolver
   * @throws LoginException
   */
  public static ResourceResolver getResourceResolver(ResourceResolverFactory resolverFactory) throws LoginException {
    Map<String, Object> param = new HashMap<>();
    param.put(ResourceResolverFactory.SUBSERVICE, "lloydsassetservice");
    return resolverFactory.getServiceResourceResolver(param);
  }


}
