/*
 * $Id: UserDataTally.java,v 1.1 2012-03-13 18:29:17 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.util.*;

import org.lockss.protocol.PeerIdentity;

/**
 * A tally of the voters in realtion to the ParticipantUserData, used
 * in calculating the agreement between the poller and each peer. The
 * only tallied agreement between the poller and a voter is if they
 * both have the URL, and agree on the content of some version.
 */
class UserDataTally implements BlockTally.VoteTally {

  // package level, for testing, and access by BlockTally.
  final Collection<PeerIdentity> talliedVoters =
    new ArrayList<PeerIdentity>();
  final Collection<PeerIdentity> talliedAgreeVoters =
    new ArrayList<PeerIdentity>();
    
  public void voteSpoiled(PeerIdentity id) {}
  public void voteAgreed(PeerIdentity id) {
    addTalliedAgreeVoter(id);
  }
  public void voteDisagreed(PeerIdentity id) {
    addTalliedDisagreeVoter(id);
  }
  public void voteVoterOnly(PeerIdentity id) {
    addTalliedDisagreeVoter(id);
  }
  public void votePollerOnly(PeerIdentity id) {
    addTalliedDisagreeVoter(id);
  }
  public void voteNeither(PeerIdentity id) {}

  void addTalliedAgreeVoter(PeerIdentity id) {
    talliedVoters.add(id);
    talliedAgreeVoters.add(id);
  }

  void addTalliedDisagreeVoter(PeerIdentity id) {
    talliedVoters.add(id);
  }
}
