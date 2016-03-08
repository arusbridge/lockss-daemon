package org.lockss.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import org.mortbay.html.*;
import java.net.URLDecoder;

import org.lockss.util.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.lockss.account.*;
import org.lockss.config.Configuration;

/**
 * Handle Ediauth authentication
 *
 * Listen to localhost for ediauth login If authentication successful create
 * User in order to keep information in the session.
 *
 */
@SuppressWarnings("serial")
public class EdiauthLogin extends LockssServlet {

  private static final Logger log = Logger.getLogger(DisplayContentStatus.class);

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "ediauth.";

  /** Ediauth configuration **/
  public static final String PARAM_EDIAUTH_IP = PREFIX + "ediauthUrl";
  public static final String PARAM_EDIAUTH_REDIRECT_URL = PREFIX + "returnURL";
  /* List of IdPs that are allowed to access the service without presenting a shibbAccountable parameter
   * This should only be set for testing purposes */
  public static final String PARAM_EDIAUTH_ALLOWED_NON_ACCOUNTABLE_IDPS = PREFIX + "allowedNonAccountableIdPs";

  private static String ediauthIP;
  private static String ediauthReturnURL;
  private static List<String> allowedNonAccountableIdPs;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config, Configuration oldConfig, Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      ediauthIP = config.get(PARAM_EDIAUTH_IP);
      ediauthReturnURL = config.get(PARAM_EDIAUTH_REDIRECT_URL);
      allowedNonAccountableIdPs = config.getList(PARAM_EDIAUTH_ALLOWED_NON_ACCOUNTABLE_IDPS);
    }
  }

  /**
   * Handle a request
   *
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    log.debug("start EdiauthLogin...");

    if (isLocalhost(req)) {
      // Get parameter
      String context = req.getParameter("ea_context");
      String encodedExtra = req.getParameter("ea_extra");

      // Collect encodedExtra inside a Map
      if (encodedExtra != null) {
        HashMap<String, String> map = new HashMap<String, String>();
        String[] pairs = encodedExtra.split("&");
        for (String p : pairs) {
          String[] pair = p.split("=");
          String key = null;
          if (pair.length > 0) {
            key = URLDecoder.decode(pair[0], "UTF-8");
          }
          String val = null;
          if (pair.length > 1) {
            val = URLDecoder.decode(pair[1], "UTF-8");
          }
          log.debug(key + ": " + val);
          map.put(key, val);
        }

        /*
         * The IdP entityID can be used to make WAYFless URLs if needed (the
         * Ediauth recommended URL would be
         * http://edina.ac.uk/Login/kbplus?idp=<URLENCODEDIDP> note that this is
         * not what shibboleth would say is a WAYFless URL -- it's better def
         * idpEntityId = extraHash.shippIdP
         *
         * accountable is a shibb thing about the eduPersonTargetedID values it
         * should be 1 if you're extra paranoid about user IDs being re-used.
         * see section 3.1.2 of
         * http://www.ukfederation.org.uk/library/uploads/Documents/
         * federation-technical-specifications.pdf and section 6.4 of
         * http://www.ukfederation.org.uk/library/uploads/Documents/
         * rules-of-membership.pdf
         */
        boolean accountable = "1".equals(map.get("shibbAccountable"));
        if (!accountable) {
            String idp = map.get("shibbIdP");
            if (idp != null) {
                accountable = allowedNonAccountableIdPs.contains(idp);
            }
        }
        boolean requireAccountable = true; // this is up to you.

        /*
         * you could set userId to eduPersonPrincipalName if it's available. if
         * not, use eduPersonTargetedId. I recommend just using
         * eduPersonTargetedID (less data protection worry with the logs and it
         * also means the userId won't change if an institution suddenly starts
         * releasing ePPN).
         */
        String userId = (requireAccountable && accountable) ? map.get("eduPersonTargetedID") : null;

        // Return nothing (failure) if there is no userId
        if (userId == null) {
          log.warning("No suitable value for userId\n");
          if (requireAccountable && !accountable) log.warning("IdP doesn't assert accountability\n");
        } else {
          log.debug("Session: " + getSession());

          String token = java.util.UUID.randomUUID().toString();
          getAccountManager().addToMapToken(token, map.get("shibbScope"));

          String eaContext = req.getParameter("ea_context");

          String response_str = "";
          
          if (context != null && context.trim().length() > 0) {
            String separator = eaContext.contains("?") ? "&" : "?";

            log.debug("params.ea_context: " + eaContext);
            response_str = eaContext + separator + "ediauthToken=" + token;
          } else {
            // Just redirect user to serveContent page
            String serverURL = (ediauthReturnURL == null) ? "http://localhost:8082/SafeNetServeContent" : ediauthReturnURL;
            log.debug("ediauth redirect URL: " + serverURL);
            response_str = serverURL + "?ediauthToken=" + token;
          }

          log.debug("Returning the URL we want the user to be redirected to: " + response_str);

          PrintWriter out = this.resp.getWriter();
          out.print(response_str);
        }
      } else {
        log.error("ERROR: encodedExtra missing.");
      }
    } else {
      log.debug("local address: " + req.getLocalAddr());
      log.error("ERROR: Ediauth attempted login from " + req.getRemoteAddr());
      resp.sendError(500);
    }
  }

  protected boolean isLocalhost(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    String localAddress = request.getLocalAddr();

    return remoteAddr.equals(localAddress) || remoteAddr.equals(ediauthIP);
  }
}