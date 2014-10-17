/*
 * $Id: TdbXml.java,v 1.3 2014-10-17 22:14:57 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.tdb;

import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.tdb.AntlrUtil.SyntaxError;

import com.ibm.icu.text.Transliterator;

public class TdbXml {

  /**
   * <p>
   * Key for the no-pub-down option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_NO_PUB_DOWN = "no-pub-down";

  /**
   * <p>
   * Single letter for the no-pub-down option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_NO_PUB_DOWN = 'd';

  /**
   * <p>
   * The no-pub-down option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_NO_PUB_DOWN =
      OptionBuilder.withLongOpt(KEY_NO_PUB_DOWN)
                   .withDescription("do not include pub_down markers")
                   .create(LETTER_NO_PUB_DOWN);
  
  /**
   * <p>
   * Key for the no-timestamp option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_NO_TIMESTAMP = "no-timestamp";

  /**
   * <p>
   * Single letter for the no-timestamp option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_NO_TIMESTAMP = 't';

  /**
   * <p>
   * The no-timestamp option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_NO_TIMESTAMP =
      OptionBuilder.withLongOpt(KEY_NO_TIMESTAMP)
                   .withDescription("do not include a timestamp")
                   .create(LETTER_NO_TIMESTAMP);
  
  /**
    * <p>
    * A pattern used to match various typical AU name suffices.
    * </p>
    *
    * @since 1.67
    */
  protected static final Pattern AU_SUFFIX =
      Pattern.compile("Volume ([^- ]+(-[^ ]+)?)( \\(([^)]+)\\))?( \\[([^]]+)\\])?$", Pattern.CASE_INSENSITIVE);
  
  /**
   * <p>
   * A pattern used to create a one-word identifier for AU names.
   * </p>
   *
   * @since 1.67
   */
  protected static final Pattern AU_UNDESIRABLE =
      Pattern.compile("(\\s|\\W|\\.)+", Pattern.CASE_INSENSITIVE);

  /**
   * <p>
   * An ICU transliterator that removes accented characters.
   * </p>
   *
   * @since 1.67
   */
  protected static final Transliterator unicodeNormalizer =
      Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC");

  /**
    * <p>
    * A list of well-known definitional parameter names in preferred
    * order.
    * </p>
    *
    * @since 1.67
    */  
  protected static final List<String> IMPLICIT_PARAM_ORDER =
      AppUtil.ul("base_url",
                 "base_url2",
                 "base_url3",
                 "base_url4",
                 "base_url5",
                 "oai_request_url",
                 "publisher_id",
                 "publisher_code",
                 "publisher_name",
                 "journal_issn",
                 "journal_id",
                 "journal_code",
                 "journal_abbr",
                 "journal_dir",
                 "year",
                 "issues",
                 "issue_set",
                 "issue_range",
                 "num_issue_range",
                 "volume_name",
                 "volume_str",
                 "volume");

  /**
    * <p>
    * An unmodifiable list of AU statuses that are defined to need a
    * <code>pub_down</code> marker.
    * </p>
    *
    * @since 1.67
    */  
  protected static final Set<String> PUB_DOWN_STATUSES =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Au.STATUS_FROZEN,
                                                                    Au.STATUS_FINISHED,
                                                                    Au.STATUS_DOWN,
                                                                    Au.STATUS_SUPERSEDED,
                                                                    Au.STATUS_ZAPPED)));

  /**
    * <p>
    * A comparator for parameter strings that ranks the well-known
    * parameters first in their given order, then others
    * alphabetically.
    * </p>
    *
    * @author Thib Guicherd-Callin
    * @since 1.67
    * @see #IMPLICIT_PARAM_ORDER
    */
  public static class ParamComparator implements Comparator<String> {
    @Override
    public int compare(String str1, String str2) {
      int i1 = IMPLICIT_PARAM_ORDER.indexOf(str1);
      int i2 = IMPLICIT_PARAM_ORDER.indexOf(str2);
      return (i1 < 0)
                ? ((i2 < 0) ? str1.compareTo(str2) : 1)
                : ((i2 < 0) ? -1 : i1 - i2);
    }
  }

  /**
    * <p>
    * A TDB query builder.
    * </p>
    *
    * @since 1.67
    */
  protected TdbQueryBuilder tdbQueryBuilder;

   /**
    * <p>
    * Makes a new instance of this class.
    * </p>
    *
    * @since 1.67
    */
  public TdbXml() {
    this.tdbQueryBuilder = new TdbQueryBuilder();
  }
  
  /**
   * <p>
   * Add this module's options to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public void addOptions(Options options) {
    // Options from other modules
    HelpOption.addOptions(options);
    VerboseOption.addOptions(options);
    KeepGoingOption.addOptions(options);
    InputOption.addOptions(options);
    OutputOption.addOptions(options);
    OutputDirectoryOption.addOptions(options);
    tdbQueryBuilder.addOptions(options);
    // Own options
    options.addOption(OPTION_NO_PUB_DOWN);
    options.addOption(OPTION_NO_TIMESTAMP);
  }

  /**
   * <p>
   * Processes a Commons CLI {@link CommandLine} instance and stores appropriate
   * information in the given options map.
   * </p>
   * 
   * @param options
   *          An options map.
   * @param cmd
   *          A Commons CLI {@link CommandLine} instance.
   * @since 1.67
   */
  public Map<String, Object> processCommandLine(Map<String, Object> options,
                                                CommandLine cmd) {
    // Options from other modules
    VerboseOption.processCommandLine(options, cmd);
    KeepGoingOption.processCommandLine(options, cmd);
    InputOption.processCommandLine(options, cmd);
    OutputOption.processCommandLine(options, cmd);
    OutputDirectoryOption.processCommandLine(options, cmd);
    tdbQueryBuilder.processCommandLine(options, cmd);
    // Sanity check
    if (OutputOption.isSingleOutput(options) && OutputDirectoryOption.isMultipleOutput(options)) {
      AppUtil.error("Cannot request both single and directory output");
    }
    // Own options
    options.put(KEY_NO_PUB_DOWN, Boolean.valueOf(cmd.hasOption(KEY_NO_PUB_DOWN)));
    options.put(KEY_NO_TIMESTAMP, Boolean.valueOf(cmd.hasOption(KEY_NO_TIMESTAMP)));
    return options;
  }
    
  /**
   * <p>
   * Produces output from a parsed TDB into the given print stream.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param out
   *          A print stream.
   * @param tdb
   *          A TDB structure.
   * @since 1.67
   */
  public void produceOutput(Map<String, Object> options,
                            PrintStream out,
                            Tdb tdb) {
    Predicate<Au> auPredicate = tdbQueryBuilder.getAuPredicate(options);
    
    preamble(options, out);
    
    Publisher currentPub = null;
    String escapedPublisherName = null;
    String escapedPublisherNameShort = null;
    Title currentTitle = null;
    String escapedTitleName = null;
    String titleIssn = null;
    String titleEissn = null;
    String titleIssnl = null;
    String titleDoi = null;
    String titleType = null;
    for (Au au : tdb.getAus()) {
      /*
       * Maybe skip this AU
       */
      if (!auPredicate.test(au)) {
        continue;
      }
      
      StringBuilder sb = new StringBuilder(4096);
      
      /*
       * Per-publisher stuff
       */
      Publisher pub = au.getTitle().getPublisher();
      if (pub != currentPub) {
        if (currentPub != null) {
          sb.append(" </property>\n");
          sb.append("\n");
        }
        currentPub = pub;
        escapedPublisherName = StringEscapeUtils.escapeXml(currentPub.getName());
        escapedPublisherNameShort = escapedPublisherName.replace(".", "");
        sb.append(" <property name=\"org.lockss.titleSet\">\n");
        sb.append("\n");
        sb.append("  <property name=\""); sb.append(escapedPublisherNameShort); sb.append("\">\n");
        sb.append("   <property name=\"name\" value=\"All "); sb.append(escapedPublisherName); sb.append(" AUs\" />\n");
        sb.append("   <property name=\"class\" value=\"xpath\" />\n");
        sb.append("   <property name=\"xpath\" value=\"[attributes/publisher='"); sb.append(escapedPublisherName); sb.append("']\" />\n");
        sb.append("  </property>\n");
        sb.append("\n");
        sb.append(" </property>\n");
        sb.append("\n");
        sb.append(" <property name=\"org.lockss.title\">\n");
        sb.append("\n");
      }
      
      /*
       * Per-title stuff
       */
      Title title = au.getTitle();
      if (title != currentTitle) {
        currentTitle = title;
        escapedTitleName = StringEscapeUtils.escapeXml(currentTitle.getName());
        titleIssn = title.getIssn();
        titleEissn = title.getEissn();
        titleIssnl = title.getIssnl();
        titleDoi = title.getDoi();
        titleType = title.getType();
      }
      
      /*
       * Per-AU stuff
       */
      // AU name
      String plugin = au.getPlugin();
      String auName = au.getName();
      String escapedAuName = StringEscapeUtils.escapeXml(auName);
      StringBuilder ausb = new StringBuilder();
      computeAuShortName(ausb, plugin, auName);
      String escapedAuNameShort = StringEscapeUtils.escapeXml(ausb.toString());
      ausb = null; // in case ausb accidentally gets re-used below instead of sb
      
      sb.append("  <property name=\""); sb.append(escapedAuNameShort); sb.append("\">\n");
      appendOneAttr(sb, "publisher", escapedPublisherName);
      sb.append("   <property name=\"journalTitle\" value=\""); sb.append(escapedTitleName); sb.append("\" />\n");
      if (titleIssn != null) {
        sb.append("   <property name=\"issn\" value=\""); sb.append(titleIssn); sb.append("\" />\n");
      }
      if (titleEissn != null) {
        sb.append("   <property name=\"eissn\" value=\""); sb.append(titleEissn); sb.append("\" />\n");
      }
      if (titleIssnl != null) {
        sb.append("   <property name=\"issnl\" value=\""); sb.append(titleIssnl); sb.append("\" />\n");
      }
      if (titleDoi != null) {
        sb.append("   <property name=\"journalDoi\" value=\""); sb.append(titleDoi); sb.append("\" />\n");
      }
      if (titleType != null) {
        sb.append("   <property name=\"type\" value=\""); sb.append(titleType); sb.append("\" />\n");
      }
      sb.append("   <property name=\"title\" value=\""); sb.append(escapedAuName); sb.append("\" />\n");
      sb.append("   <property name=\"plugin\" value=\""); sb.append(plugin); sb.append("\" />\n");
      
      // Definitional parameters
      Map<String, String> params = au.getParams();
      List<String> paramNames = new ArrayList<String>(params.keySet());
      Collections.sort(paramNames, new ParamComparator());
      int paramIndex = 1;
      for (String param : paramNames) {
        String val = params.get(param);
        appendOneParam(sb, paramIndex, param, val);
        ++paramIndex;
      }
      // Non-definitional parameters
      for (Map.Entry<String, String> ent : au.getNondefParams().entrySet()) {
        appendOneParam(sb, paramIndex, ent.getKey(), ent.getValue());
        ++paramIndex;
      }

      // au_proxy and pub_down
      String proxy = au.getProxy();
      if (proxy != null) {
        appendOneParam(sb, 98, "crawl_proxy", proxy);
      }
      String status = au.getStatus();
      if (options.get(KEY_NO_PUB_DOWN) == Boolean.FALSE && PUB_DOWN_STATUSES.contains(status)) {
        appendOneParam(sb, 99, "pub_down", "true");
      }
      
      // Attributes
      for (Map.Entry<String, String> ent : au.getAttrs().entrySet()) {
        appendOneAttr(sb, ent.getKey(), ent.getValue());
      }
      
      // Miscellaneous
      appendOneAttr(sb, "edition", au.getEdition());
      appendOneAttr(sb, "eisbn", au.getEisbn());
      appendOneAttr(sb, "isbn", au.getIsbn());
      appendOneAttr(sb, "year", au.getYear());
      appendOneAttr(sb, "volume", au.getVolume());
      if ("openaccess".equals(au.getRights())) {
        appendOneAttr(sb, "rights", "openaccess");
      }
      if (Au.STATUS_ZAPPED.equals(status)) {
        appendOneAttr(sb, "status", Au.STATUS_ZAPPED);
      }

      // Output
      sb.append("  </property>\n");
      out.println(sb.toString());
    }
    
    if (currentPub != null) {
      out.append(" </property>\n");
      out.append("\n");
    }
    
    postamble(options, out);
  }

  /**
    * <p>
    * Computes a single-word identifier for an AU name.
    * </p>
    *
    * @param ausb
    *          A string builder into which the result is output
    * @param plugin
    *          The AU's plugin.
    * @param auName
    *          The AU's name.
    * @since 1.67
    */
  protected void computeAuShortName(StringBuilder ausb,
                                    String plugin,
                                    String auName) {
    ausb.append(plugin.substring(plugin.lastIndexOf('.') + 1));
    Matcher mat = AU_SUFFIX.matcher(auName);
    if (mat.find()) {
      ausb.append(AU_UNDESIRABLE.matcher(unicodeNormalizer.transliterate(auName.substring(0, mat.start()))).replaceAll(""));
      ausb.append(mat.group(1));
      String year = mat.group(4);
      if (year != null) {
        ausb.append("_");
        ausb.append(year);
      }
      String comment = mat.group(6);
      if (comment != null) {
        ausb.append("_");
        ausb.append(comment.replace(" ", ""));
      }
    }
    else {
      ausb.append(AU_UNDESIRABLE.matcher(auName).replaceAll(""));
    }
  }

  /**
    * <p>
    * Appends one parameter (definitional or non-definitional) to a
    * string builder.
    * </p>
    *
    * @param sb
    *          The string builder where the result is output.
    * @param paramIndex
    *          The index of the parameter.
    * @param key
    *          The key (name) of the parameter.
    * @param value
    *          The value of the parameter.
    * @since 1.67
    */
  protected void appendOneParam(StringBuilder sb,
                                int paramIndex,
                                String key,
                                String value) {
    sb.append("   <property name=\"param."); sb.append(paramIndex); sb.append("\">\n");
    sb.append("    <property name=\"key\" value=\""); sb.append(key); sb.append("\" />\n");
    sb.append("    <property name=\"value\" value=\""); sb.append(value); sb.append("\" />\n");
    sb.append("   </property>\n");
  }
  
  /**
    * <p>
    * Appends one attribute to a string builder.
    * </p>
    *
    * @param sb
    *          The string builder where the result is output.
    * @param key
    *          The key (name) of the attribute.
    * @param value
    *          The value of the attribute.
    * @since 1.67
    */
  protected void appendOneAttr(StringBuilder sb,
                               String key,
                               String value) {
    if (value != null) {
      sb.append("   <property name=\"attributes."); sb.append(key); sb.append("\" value=\""); sb.append(value); sb.append("\" />\n");
    }
  }
  
  /**
    * <p>
    * Outputs the XML preamble to a given output channel.
    * </p>
    *
    * @param options
    *          The options map.
    * @param out
    *          The output channel.
    * @since 1.67
    */
  public void preamble(Map<String, Object> options, PrintStream out) {
    StringBuilder sb = new StringBuilder(2048);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<!DOCTYPE lockss-config [\n");
    sb.append("<!ELEMENT lockss-config (if|property)*>\n");
    sb.append("<!ELEMENT property (property|list|value|if)*>\n");
    sb.append("<!ELEMENT list (value)+>\n");
    sb.append("<!ELEMENT value (#PCDATA)>\n");
    sb.append("<!ELEMENT test EMPTY>\n");
    sb.append("<!ELEMENT and (and|or|not|test)*>\n");
    sb.append("<!ELEMENT or (and|or|not|test)*>\n");
    sb.append("<!ELEMENT not (and|or|not|test)*>\n");
    sb.append("<!ELEMENT if (and|or|not|then|else|test|property)*>\n");
    sb.append("<!ELEMENT then (if|property)*>\n");
    sb.append("<!ELEMENT else (if|property)*>\n");
    sb.append("<!ATTLIST property name CDATA #REQUIRED>\n");
    sb.append("<!ATTLIST property value CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test hostname CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test group CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test daemonVersionMin CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test daemonVersionMax CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test daemonVersion CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test platformVersionMin CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test platformVersionMax CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test platformVersion CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST test platformName CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if hostname CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if group CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if daemonVersionMin CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if daemonVersionMax CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if daemonVersion CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if platformVersionMin CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if platformVersionMax CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if platformVersion CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST if platformName CDATA #IMPLIED>\n");
    sb.append("<!ATTLIST list append CDATA #IMPLIED>\n");
    sb.append("]>\n");
    sb.append("\n");
    if (options.get(KEY_NO_TIMESTAMP) == Boolean.FALSE) {
      sb.append("<!-- ");
      sb.append(DateFormat.getDateTimeInstance().format(new Date()));
      sb.append(" -->\n");
      sb.append("\n");
    }
    sb.append("<lockss-config>\n");
    out.println(sb.toString());
  }

  /**
    * <p>
    * Outputs the XML postamble to a given output channel.
    * </p>
    *
    * @param options
    *          The options map.
    * @param out
    *          The output channel.
    * @since 1.67
    */
  public void postamble(Map<String, Object> options, PrintStream out) {
    out.println("</lockss-config>");
  }

  /**
   * <p>
   * Runs the command when directory output has been requested, processing one
   * input file at a time and producing output for it in the corresponding
   * file before moving on to the next.
   * </p>
   * 
   * @param options
   *          The options map.
   * @since 1.67
   */
  public void processMultipleOutput(Map<String, Object> options) {
    List<String> inputFiles = InputOption.getInput(options);
    for (String f : inputFiles) {
      try {
        TdbBuilder tdbBuilder = new TdbBuilder();
        if ("-".equals(f)) {
          AppUtil.warning(options, null, "Cannot process from standard input in output directory mode");
          KeepGoingOption.addError(options, null);
        }
        else {
          tdbBuilder.parse(f);
          PrintStream out = OutputDirectoryOption.getMultipleOutput(options, f, ".xml");
          produceOutput(options, out, tdbBuilder.getTdb());
          out.close();
        }
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.warning(options, fnfe, "%s: file not found", f);
        KeepGoingOption.addError(options, fnfe);
      }
      catch (IOException ioe) {
        AppUtil.warning(options, ioe, "%s: error reading from file", f);
        KeepGoingOption.addError(options, ioe);
      }
      catch (SyntaxError se) {
        AppUtil.warning(options, se, se.getMessage());
        KeepGoingOption.addError(options, se);
      }
    }
    
    List<Exception> errors = KeepGoingOption.getErrors(options);
    int errs = errors.size();
    if (KeepGoingOption.isKeepGoing(options) && errs > 0) {
      AppUtil.error(options, errors, "Encountered %d %s; exiting", errs, errs == 1 ? "error" : "errors");
    }
  }
  
  /**
   * <p>
   * Runs the command when single output has been requested, processing the
   * entire set of input files before producing all output into a single
   * destination.
   * </p>
   * 
   * @param options
   *          The options map.
   * @since 1.67
   */
  public void processSingleOutput(Map<String, Object> options) {
    List<String> inputFiles = InputOption.getInput(options);
    TdbBuilder tdbBuilder = new TdbBuilder();
    for (String f : inputFiles) {
      try {
        if ("-".equals(f)) {
          f = "<stdin>";
          tdbBuilder.parse(f, System.in);
        }
        else {
          tdbBuilder.parse(f);
        }
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.warning(options, fnfe, "%s: file not found", f);
        KeepGoingOption.addError(options, fnfe);
      }
      catch (IOException ioe) {
        AppUtil.warning(options, ioe, "%s: I/O error", f);
        KeepGoingOption.addError(options, ioe);
      }
      catch (SyntaxError se) {
        AppUtil.warning(options, se, se.getMessage());
        KeepGoingOption.addError(options, se);
      }
    }
    
    List<Exception> errors = KeepGoingOption.getErrors(options);
    int errs = errors.size();
    if (KeepGoingOption.isKeepGoing(options) && errs > 0) {
      AppUtil.error(options, errors, "Encountered %d %s; exiting", errs, errs == 1 ? "error" : "errors");
    }
    
    PrintStream out = OutputOption.getSingleOutput(options);
    produceOutput(options, out, tdbBuilder.getTdb());
    out.close();
  }
  
  /**
   * <p>
   * Secondary entry point of this class, after the command line has been
   * parsed.
   * </p>
   * 
   * @param cmd
   *          A parsed command line.
   * @throws IOException
   *           if an I/O error occurs.
   * @since 1.67
   */
  public void run(CommandLine cmd) throws IOException {
    Map<String, Object> options = new HashMap<String, Object>();
    processCommandLine(options, cmd);
    if (OutputDirectoryOption.isMultipleOutput(options)) {
      processMultipleOutput(options);
    }
    else {
      processSingleOutput(options);
    }
  }
  
  /**
   * <p>
   * Primary entry point of this class, before the command line has been parsed.
   * </p>
   * 
   * @param mainArgs
   *          Command line arguments.
   * @throws Exception if any error occurs.
   * @since 1.67
   */
  public void run(String[] mainArgs) throws Exception {
    AppUtil.fixMainArgsForCommonsCli(mainArgs);
    Options options = new Options();
    addOptions(options);
    CommandLine cmd = new GnuParser().parse(options, mainArgs);
    HelpOption.processCommandLine(cmd, options, getClass());
    run(cmd);
  }
  
  /**
   * <p>
   * Creates a {@link TdbXml} instance and calls {@link #run(String[])}.
   * </p>
   * 
   * @param args
   *          Command line arguments.
   * @throws Exception
   *           if any error occurs.
   * @since 1.67
   */
  public static void main(String[] args) throws Exception {
    new TdbXml().run(args);
  }

}
