/*
 * $Id: ScJournalsArticleIteratorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.springer.link;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class SpringerLinkArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%s(book|article)/([^/]+)/([^/]+)$\", base_url";
  
  //http://link.springer.com/book/10.1007/978-4-431-54340-4
  private static final Pattern LANDING_PATTERN = Pattern.compile("/(article|book)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String LANDING_REPLACEMENT = "/$1/$2/$3";

  //http://link.springer.com/article/10.1007/978-4-431-54340-4_1/fulltext.html
  private static final Pattern HTML_PATTERN = Pattern.compile("/(article|book)/([^/]+)/([^/]+)/fulltext\\.html$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/$1/$2/$3/fulltext.html";
  
  //http://link.springer.com/download/epub/10.1007/978-4-431-54340-4.epub
  private static final Pattern EPUB_PATTERN = Pattern.compile("/(download/epub/)([^/]+)/([^/]+)\\.epub$", Pattern.CASE_INSENSITIVE);
  private static final String EPUB_REPLACEMENT = "/download/epub/$2/$3.epub";

  //http://link.springer.com/content/pdf/10.1007%2F978-4-431-54340-4.pdf
  private static final Pattern PDF_PATTERN = Pattern.compile("/(content/pdf/)([^%/]+)%2F([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/content/pdf/$2%2f$3.pdf";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    builder.addAspect(LANDING_PATTERN,
                      LANDING_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(EPUB_PATTERN,
                      EPUB_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_EPUB);

    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_ABSTRACT,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
    							 ArticleFiles.ROLE_FULL_TEXT_PDF,
                                 ArticleFiles.ROLE_FULL_TEXT_EPUB,
                                 ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
