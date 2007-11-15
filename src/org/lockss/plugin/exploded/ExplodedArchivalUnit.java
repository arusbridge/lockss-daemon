/*
 * $Id: ExplodedArchivalUnit.java,v 1.4 2007-11-15 23:15:38 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.plugin.exploded;

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;
import org.lockss.state.AuState;

/**
 * <p>ExplodedArchivalUnit: The Archival Unit Class for ExplodedPlugin.
 * This archival unit uses a base url to define an archival unit.
 * @author David Rosenthal
 * @version 1.0
 */

public class ExplodedArchivalUnit extends BaseArchivalUnit {
  private String m_explodedBaseUrl = null;
  protected Logger logger = Logger.getLogger("ExplodedArchivalUnit");

  public ExplodedArchivalUnit(ExplodedPlugin plugin) {
    super(plugin);
  }

  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    this.m_explodedBaseUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
    String pubName = config.get(ConfigParamDescr.PUBLISHER_NAME.getKey());
    if (pubName != null) {
      auName = pubName;
      // We have some config info to work with
      String journalId = config.get(ConfigParamDescr.JOURNAL_ISSN.getKey());
      if (journalId == null) {
	journalId = config.get(ConfigParamDescr.JOURNAL_ABBR.getKey());
	if (journalId == null) {
	  journalId = config.get(ConfigParamDescr.JOURNAL_DIR.getKey());
	}
      }
      if (journalId != null) {
	auName += " " + journalId;
      }
    }
  }


  // Called by RegistryPlugin iff any config below RegistryPlugin.PREFIX
  // has changed
  protected void setConfig(Configuration config,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    // XXX
  }

  /**
   * return a string that represents the plugin registry.  This is
   * just the base URL unless overridden by the config.
   * @return The base URL.
   */
  protected String makeName() {
    return (auName != null ? auName : m_explodedBaseUrl);
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  protected String makeStartUrl() {
    return m_explodedBaseUrl;
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    logger.debug3(url + " vs. " + m_explodedBaseUrl);
    return url.startsWith(m_explodedBaseUrl);
  }

  protected CrawlRule makeRules() {
    return new ExplodedCrawlRule();
  }

  private class ExplodedCrawlRule implements CrawlRule {
    protected ExplodedCrawlRule() {
    }

    public int match(String url) {
      if (url.startsWith(m_explodedBaseUrl)) {
	return INCLUDE;
      }
      return EXCLUDE;
    }
  }
  /**
   * Return a new CrawlSpec with the appropriate collect AND redistribute
   * permissions, and with the maximum refetch depth.
   *
   * @return CrawlSpec
   */
  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {
    ArrayList startUrls = new ArrayList();
    startUrls.add(startUrlString);
    return new ExplodedCrawlSpec(startUrls);
  }

  /**
   * ExplodedArchivalUnits should never be crawled.
   * @param aus ignored
   * @return false
   */
  public boolean shouldCrawlForNewContent(AuState aus) {
    return false;
  }

  // Exploded AU crawl spec implementation
  private class ExplodedCrawlSpec extends BaseCrawlSpec {
    ExplodedCrawlSpec(List startUrls) {
      // null CrawlRule is always INCLUDE
      // null PermissionChecker is XXX
      // null loginPageChecker is always false
      super(startUrls, null, null, null);
    }
  }
    
}
