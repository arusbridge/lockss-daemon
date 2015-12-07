/*
 * $Id:$
 */
/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.medknow;

import java.util.Set;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestMedknowHtmlLinkExtractorFactory extends LockssTestCase {

  private LinkExtractorFactory factJS;
  private LinkExtractorFactory factG;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String BASE_URL = "http://www.medknow.com/";
  Set<String> expectedUrls;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
        
    factJS = new JsoupHtmlLinkExtractor.Factory();
    factG = new GoslingHtmlLinkExtractor.Factory();

    // MODIFY HERE TO CHANGE EXTRACTORS (1 of 2)
    //m_extractor = factJS.createLinkExtractor("html");
    m_extractor = factG.createLinkExtractor("html");

  }

  
  
  
  private static final String toc_url = "http://www.jpgmonline.com/showbackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=1";
  private static final String toc_withApostropheLink = "<html><body>" +
    "<td class=\"other\" style=\"text-align:left;\">" +
    "<a class=\"toc\" href=\"article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=0\" title=\"Click to View ABSTRACT of the article.\">[ABSTRACT]</a>&nbsp;&nbsp;" +
    "<a class=\"toc\" href=\"article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza\" title=\"Click to View Full Text of the article.\">[HTML Full text]</a>&nbsp;&nbsp;" +
    "<a class=\"toc\" href=\"article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=2\" title=\"Click to download PDF version of the article.\">[PDF]</a>&nbsp;&nbsp;" +
    "</td>" +
    "</body></html>";
  
  private static final String abstract_url = "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=0";
   private static final String abstract_withApostropheLink = "<html><body>" +
    "<a href=\"article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza\">FULL TEXT</a>" +
    "<a href=\"article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=2\">PDF</a>" +
    "<br><br>" +
    "</body></html>";



     //This test makes sure other base link extraction continues to work

     public void testTOCHtml() throws Exception {
   
      Set<String> result_strings = parseSingleSource(toc_withApostropheLink, toc_url);
      expectedUrls = SetUtil.set(
      "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=0",
      "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza",
      "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=2"
      );

      assertEquals(3, result_strings.size());
      for (String url : result_strings) {
        log.debug3("URL: " + url);
        assertTrue(expectedUrls.contains(url));
      }
    }
    
   
     public void testAbstractHtml() throws Exception {
       
       Set<String> result_strings = parseSingleSource(abstract_withApostropheLink, abstract_url);
       expectedUrls = SetUtil.set(
       "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza",
       "http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D'Souza;type=2"
       );

       assertEquals(2, result_strings.size());
       for (String url : result_strings) {
         log.debug3("URL: " + url);
         assertTrue(expectedUrls.contains(url));
       }
       
     }
     

  
  /*------------------SUPPORT FUNCTIONS --------------------- */

       private Set<String> parseSingleSource(String source, String srcUrl)
           throws Exception {
      
         MockArchivalUnit m_mau = new MockArchivalUnit();
         // MODIFY HERE TO CHANGE EXTRACTORS (2 of 2)
         //LinkExtractor ue = new JsoupHtmlLinkExtractor();
         LinkExtractor ue = new GoslingHtmlLinkExtractor();
         m_mau.setLinkExtractor("html", ue);
         MockCachedUrl mcu =
             new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
         mcu.setContent(source);

         m_callback.reset();
         m_extractor.extractUrls(m_mau,
             new org.lockss.test.StringInputStream(source), ENC,
             srcUrl, m_callback);
         return m_callback.getFoundUrls();
       }

  private static class MyLinkExtractorCallback implements
  LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }

}
