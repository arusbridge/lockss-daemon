/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.net.URL;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.wrapper.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;

public class TestProjectMuseArchivalUnit extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JRNL_KEY = ConfigParamDescr.JOURNAL_DIR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  private MockLockssDaemon theDaemon;

  static final String HTTP_ROOT = "http://muse.jhu.edu/";
  static final String HTTPS_ROOT = "https://muse.jhu.edu/";
  static final String DIR = "american_imago";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL baseUrl, String journalDir, int volume)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (baseUrl!=null) {
      props.setProperty(BASE_URL_KEY, baseUrl.toString());
    }
    if (journalDir!=null) {
      props.setProperty(JRNL_KEY, journalDir);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.projmuse.ProjectMusePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, DIR, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(HTTP_ROOT);
    try {
      makeAu(url, DIR, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNullDir() throws Exception {
    URL url = new URL(HTTP_ROOT);
    try {
      makeAu(url, null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(HTTP_ROOT);
    int volume = 60;
    ArchivalUnit pmAu = makeAu(base, DIR, volume);
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = HTTP_ROOT + "journals/"+DIR+"/";

    // root page
    shouldCacheTest(baseUrl+"v060/", true, pmAu, cus);

    // volume page
    shouldCacheTest(baseUrl+"toc/aim60.1.html", true, pmAu, cus);
    // any other toc in this journal volume
    shouldCacheTest(baseUrl+"toc/sdf60.1.html", true, pmAu, cus);

    // article html
    shouldCacheTest(baseUrl+"v060/60.2zimmerman.html", true, pmAu, cus);
    shouldCacheTest(baseUrl+"summary/v060/60.2zimmerman.html", true, pmAu, cus);

    // article pdf
    shouldCacheTest(baseUrl+"v060/60.2zimmerman.pdf", true, pmAu, cus);

    // images
    shouldCacheTest(HTTP_ROOT+"images/toolbar/barjournal.gif", true, pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"images/journals/banners/aim.gif", true,
                    pmAu, cus);

    // cover material
    shouldCacheTest(baseUrl+"v060/60.1cover_art.html", true, pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"images/journals/covers/aim60.1.gif", true,
                    pmAu, cus);

    // should not cache these
    // index links
    shouldCacheTest("http://www.press.jhu.edu/cgi-bin/redirect.pl", false,
                    pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"journals/indexold.html", false, pmAu, cus);

    // archived root page
    shouldCacheTest(baseUrl+"v059/", false, pmAu, cus);

    // archived volume page
    shouldCacheTest(baseUrl+"toc/aim59.1.html", false, pmAu, cus);

    // button destinations
    shouldCacheTest(HTTP_ROOT+"ordering/index.html", false, pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"journals", false, pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"proj_descrip/contact.html", false, pmAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);

    // substring matches
    shouldCacheTest(baseUrl+"v0601/", false, pmAu, cus);
    shouldCacheTest(baseUrl+"toc/similartoaim60.1.html", true, pmAu, cus);
    shouldCacheTest(baseUrl+"toc/similartoaim.60.1.html", true, pmAu, cus);

    // other site
    shouldCacheTest("http://muse2.jhu.edu/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(HTTP_ROOT);
    String expectedPath;
    DefinableArchivalUnit pmAu;
    
    // 1 digit
    expectedPath = "journals/" + DIR + "/v006/";
    pmAu = makeAu(url, DIR, 6);
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());

    // 2 digits
    expectedPath = "journals/" + DIR + "/v065/";
    pmAu = makeAu(url, DIR, 65);
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());

    // 3 digits
    expectedPath = "journals/" + DIR + "/v654/";
    pmAu = makeAu(url, DIR, 654);
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());

    // 4 digits
    expectedPath = "journals/" + DIR + "/v2015/";
    pmAu = makeAu(url, DIR, 2015);
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());

  }

  public void testGetUrlStems() throws Exception {
    String stem1a = "http://muse.jhu.edu/";
    String stem1b = "https://muse.jhu.edu/";
    DefinableArchivalUnit pmAu1 = makeAu(new URL(stem1a + "foo/"), DIR, 60);
    assertSameElements(Arrays.asList(stem1a, stem1b), pmAu1.getUrlStems());
    String stem2a = "http://muse.jhu.edu:8080/";
    String stem2b = "https://muse.jhu.edu:8080/";
    DefinableArchivalUnit pmAu2 = makeAu(new URL(stem2a), DIR, 60);
    assertSameElements(Arrays.asList(stem2a, stem2b), pmAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), DIR, 60);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), DIR, 60);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), DIR, 60);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(HTTP_ROOT), DIR, 60);
    assertEquals("Project Muse Journals Plugin, Base URL http://muse.jhu.edu/, Journal ID american_imago, Volume 60", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), "bmj", 61);
    assertEquals("Project Muse Journals Plugin, Base URL http://www.bmj.com/, Journal ID bmj, Volume 61", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(HTTP_ROOT), DIR, 60);
    assertTrue(WrapperUtil.unwrap(au.getHashFilterFactory("text/html"))
	       instanceof ProjectMuseHtmlHashFilterFactory);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestProjectMuseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
