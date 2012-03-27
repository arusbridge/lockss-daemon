/*
 * $Id: ConditionalCrawlRateLimiter.java,v 1.1 2012-03-27 20:57:29 tlipkis Exp $
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

package org.lockss.crawler;

import java.util.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * A set of time-specific CrawlRateLimiters.
 */
public class ConditionalCrawlRateLimiter extends BaseCrawlRateLimiter {

  static Logger log = Logger.getLogger("ConditionalCrawlRateLimiter");

  List<Clause> clauses;
  boolean didChangeWindow = false;
  CrawlWindow lastWindow;
  CrawlRateLimiter lastWindowCrl;

  class Clause {
    CrawlWindow window;
    CrawlRateLimiter crl;

    Clause(CrawlWindow window, CrawlRateLimiter crl) {
      this.window = window;
      this.crl = crl;
    }
  }

  public ConditionalCrawlRateLimiter(RateLimiterInfo rli) {
    for (Map.Entry ent : rli.getCond().entrySet()) {
      addClause((CrawlWindow)ent.getKey(),
		CrawlRateLimiter.Util.forRli((RateLimiterInfo)ent.getValue()));
    }
  }

  /** Return the RateLimiter on which to wait for the next fetch
   * @param url the url about to be fetched
   * @param previousContentType the MIME type or Content-Type of the
   * previous file fetched
   * @throws RuntimeException if no window is currently open
   */
  public RateLimiter getRateLimiterFor(String url, String previousContentType) {
    // fetch date once to ensure test same value against all windows
    Date now = TimeBase.nowDate();
    CrawlRateLimiter crl = null;
    if (lastWindow != null && lastWindow.canCrawl(now)) {
      // Keep using current CrawlRateLimiter until its window closes, to
      // minimize switching between rate limiters.
      crl = lastWindowCrl;
    } else {
      for (Clause cl : clauses) {
	if (cl.window.canCrawl(now)) {
	  crl = cl.crl;
	}
      }
    }
    if (crl == null) {
      throw new RuntimeException("No rate limiter windows are open");
    }
    if (crl != lastWindowCrl) {
      if (lastWindowCrl != null) {
	didChangeWindow = true;
      }
      lastWindowCrl = crl;
    }
    return crl.getRateLimiterFor(url, previousContentType);
  }

  /** Pause using the the rate limiter appropriate for the current time
   * window.  If the window closed and we switched to a different window's
   * limiters, pause twice to compensate for the fact that the new rate
   * limiters haven't seen any events recently */
  public void pauseBeforeFetch(String url, String  previousContentType) {
    super.pauseBeforeFetch(url, previousContentType);
    if (didChangeWindow) {
      super.pauseBeforeFetch(url, previousContentType);
    }
    didChangeWindow = false;
  }

  /** Add a conditional clause. */
  private ConditionalCrawlRateLimiter addClause(CrawlWindow window,
						CrawlRateLimiter crl) {
    if (clauses == null) {
      clauses = new ArrayList<Clause>(4);
    }
    clauses.add(new Clause(window, crl));
    return this;
  }

}
