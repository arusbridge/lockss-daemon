package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import junit.framework.TestCase;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends TestCase {
  private static String[] rooturls = {"http://www.test.org",
    "http://www.test1.org", "http://www.test2.org"};
  private static String regexp = "*.doc";
  private static long testduration = 5 * 60 *60 *1000; /* 5 min */

  private static String[] testentries = {"test1.doc", "test2.doc", "test3.doc"};
  protected static ArchivalUnit testau;
  static {
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    org.lockss.plugin.Plugin.registerArchivalUnit(testau);
  }

  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage[] testmsg;
  protected Poll[] testpolls;

  public TestPoll(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = LcapIdentity.getIdentity(testaddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        testmsg[i] =  LcapMessage.makeRequestMsg(
        rooturls[i],
        regexp,
        testentries,
        testaddr,
        (byte)5,
        PollManager.generateRandomBytes(),
        PollManager.generateRandomBytes(),
        LcapMessage.NAME_POLL_REQ + (i * 2),
        testduration,
        testID);
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }

    try {
      testpolls = new Poll[3];
      for(int i=0; i< 3; i++) {
        testpolls[i] = PollManager.makePoll(testmsg[i]);
      }
    }
    catch (IOException ex) {
      fail("can't create test poll" + ex.toString());
    }
  }

  /** tearDown method for test case */
  protected void tearDown() {
    for(int i= 0; i< 3; i++) {
      PollManager.removePoll(testpolls[i].m_key);
    }
  }


  /** test for method scheduleVote(..) */
  public void testScheduleVote() {
    Poll p = testpolls[1];
    p.scheduleVote();
    assertNotNull(p.m_voteTime);
    assertTrue(p.m_voteTime.getRemainingTime()
               < p.m_deadline.getRemainingTime());
  }

  /** test for method checkVote(..) */
  public void testCheckVote() {
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    LcapIdentity id = msg.getOriginID();
    int rep = id.getReputation();

    // good vote check
    try {
      p.checkVote(msg.getHashed(), msg);
    }
    catch(IllegalStateException ex) {
      // unitialized comm
    }

    assertEquals(1, p.m_tally.numYes);
    assertEquals(rep, p.m_tally.wtYes);
    assertTrue(rep <= id.getReputation());

    rep = id.getReputation();
    // bad vote check
    try {
      p.checkVote(PollManager.generateRandomBytes(), msg);
    }
    catch(IllegalStateException ex) {
      // unitialized comm
    }
    assertEquals(1, p.m_tally.numNo);
    assertEquals(rep, p.m_tally.wtNo);
    assertTrue(rep >= id.getReputation());
  }


  /** test for method tally(..) */
  public void testTally() {
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    p.m_tally.addVote(new Poll.Vote(msg,false));
    p.m_tally.addVote(new Poll.Vote(msg,false));
    p.m_tally.addVote(new Poll.Vote(msg,false));
    assertEquals(0, p.m_tally.numYes);
    assertEquals(0, p.m_tally.wtYes);
    assertEquals(3, p.m_tally.numNo);
    assertEquals(1500, p.m_tally.wtNo);

    p = testpolls[1];
    msg = p.getMessage();
    p.m_tally.addVote(new Poll.Vote(msg,true));
    p.m_tally.addVote(new Poll.Vote(msg,true));
    p.m_tally.addVote(new Poll.Vote(msg,true));
    assertEquals(3, p.m_tally.numYes);
    assertEquals(1500, p.m_tally.wtYes);
    assertEquals(0, p.m_tally.numNo);
    assertEquals(0, p.m_tally.wtNo);
  }

  /** test for method vote(..) */
  public void testVote() {
    Poll p = testpolls[1];
    p.m_hash = PollManager.generateRandomBytes();
    try {
      p.vote();
    }
    catch(IllegalStateException e) {
      // the socket isn't inited and should squack
    }
  }

  /** test for method startPoll(..) */
  public void testStartPoll() {
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    Poll p = testpolls[0];
    p.m_tally.quorum = 10;
    p.m_tally.numYes = 5;
    p.m_tally.numNo = 2;
    p.m_tally.wtYes = 2000;
    p.m_tally.wtNo = 200;
    p.m_hash = PollManager.generateRandomBytes();
    try {
      p.voteInPoll();
    }
    catch(IllegalStateException e) {
      // the socket isn't inited and should squack
    }

    p.m_tally.numYes = 20;
    try {
      p.voteInPoll();
    }
    catch(NullPointerException npe) {
      // the socket isn't inited and should squack
    }

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    Poll p = testpolls[1];
    p.m_pollstate = Poll.PS_WAIT_TALLY;
    p.stopPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
    p.startPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
  }

  /** test for method startVote(..) */
  public void testStartVote() {
    Poll p = testpolls[0];
    p.m_pendingVotes = 3;
    p.startVote();
    assertEquals(4, p.m_pendingVotes);
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    Poll p = testpolls[1];
    p.m_pendingVotes = 3;
    p.stopVote();
    assertEquals(2,p.m_pendingVotes);
  }

  public void testVerifyPoll() {

  }


  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}