/*
 * $Id: TitleSetActiveAus.java,v 1.1.2.1 2005-01-19 01:37:03 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/** The set of titles configured on the cache */
public class TitleSetActiveAus extends BaseTitleSet {

  /** Create a TitleSet that consists of all configured titles
   * @param daemon used to get list of all known titles
   */
  public TitleSetActiveAus(LockssDaemon daemon) {
    super(daemon, "All active AUs on this cache");
  }

  /** Return the titles in the set.
   * @return a collection of TitleConfig */
  public Collection getTitles() {
    List aus = daemon.getPluginManager().getAllAus();
    List res = new ArrayList(aus.size());
    for (Iterator iter = aus.iterator(); iter.hasNext();) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      res.add(titleConfigFromAu(au));
    }
    return res;
  }

  /** Return a TitleConfig for the AU.  Returns matching entry from the
   * title db if found, else creates one */
  TitleConfig titleConfigFromAu(ArchivalUnit au) {
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      Plugin plugin = au.getPlugin();
      String auname = au.getName();
      tc = new TitleConfig(auname, plugin);
      Configuration auConfig = au.getConfiguration();
      List params = new ArrayList();
      for (Iterator iter = auConfig.keyIterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	String val = auConfig.get(key);
	ConfigParamDescr descr = findParamDescr(plugin, key);
	if (descr != null) {
	  ConfigParamAssignment cpa = new ConfigParamAssignment(descr, val);
	  params.add(cpa);
	} else {
	  log.warning("Unknown parameter key: " + key + " in au: " + auname);
	}
      }
      tc.setParams(params);
    }
    return tc;
  }

  // Copy of code in BasePlugin
  private ConfigParamDescr findParamDescr(Plugin plugin, String key) {
    List descrs = plugin.getAuConfigDescrs();
    for (Iterator iter = descrs.iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.getKey().equals(key)) {
	return descr;
      }
    }
    return null;
  }

  Collection filterTitles(Collection allTitles) {
    return allTitles;
  }

  protected int getActionables() {
    return SET_DELABLE;
  }

  protected int getMajorOrder() {
    return 2;
  }

  public boolean equals(Object o) {
    return (o instanceof TitleSetActiveAus);
  }

  public int hashCode() {
    return 0x272057;
  }

  public String toString() {
    return "[TS.ActiveAus]";
  }
}
