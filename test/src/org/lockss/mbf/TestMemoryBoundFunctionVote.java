/*
 * $Id: TestMemoryBoundFunctionVote.java,v 1.8 2003-09-05 02:45:20 dshr Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mbf;

import java.net.UnknownHostException;
import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.*;
import org.lockss.protocol.*;

/**
 * JUnitTest case for class: org.lockss.mbf.MemoryBoundFunctionVote and
 * its implementations.
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class TestMemoryBoundFunctionVote extends LockssTestCase {
  private static Logger log = null;
  private static Random rand = null;
  private static File f = null;
  private static byte[] basis;
  private int pathsTried;
  private static String[] MBFnames = {
    "MOCK",  // NB - must be first
    // "MBF1",
    "MBF2",
  };
  private static MemoryBoundFunctionFactory[] MBFfactory = null;
  private static String[] MBFVnames = {
    "MOCK",  // NB - must be first
    "MBFV2",
  };
  private static MemoryBoundFunctionVoteFactory[] MBFVfactory = null;
  private static final int numSteps = 16;
  private static int[] goodProof = {
    1, 2,
  };
  private static int[] badProof = {
    0,
  };
  private static byte[] goodContent = null;
  private static byte[] badContent = null;
  private static byte[] pollID = null;
  private static LcapIdentity voterID = null;

  protected void setUp() throws Exception {
    super.setUp();
    log = Logger.getLogger("TestMemoryBoundFunction");
    if (false)
      rand = new Random(100);
    else 
      rand = new Random(System.currentTimeMillis());
    if (f == null) {
      f = FileUtil.tempFile("mbf1test");
      FileOutputStream fis = new FileOutputStream(f);
      basis = new byte[16*1024*1024 + 1024];
      rand.nextBytes(basis);
      fis.write(basis);
      fis.close();
      log.info(f.getPath() + " bytes " + f.length() + " long created");
    }
    if (goodContent == null) {
      goodContent = new byte[256*1024];
      rand.nextBytes(goodContent);
      log.info(goodContent.length + "bytes of good synthetic content created");
    }
    if (badContent == null) {
      badContent = new byte[256*1024];
      rand.nextBytes(badContent);
      log.info(badContent.length + "bytes of bad synthetic content created");
    }
    if (!f.exists())
      fail(f.getPath() + " doesn't exist");
    MemoryBoundFunction.configure(f);
    if (MBFfactory == null) {
      MBFfactory = new MemoryBoundFunctionFactory[MBFnames.length];
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MBFfactory[i] = new MemoryBoundFunctionFactory(MBFnames[i]);
	} catch (NoSuchAlgorithmException ex) {
	  fail(MBFnames[i] + " threw " + ex.toString());
	}
      }
    }
    if (MBFVfactory == null) {
      MBFVfactory = new MemoryBoundFunctionVoteFactory[MBFVnames.length];
      for (int i = 0; i < MBFVfactory.length; i++) {
	try {
	  MBFVfactory[i] = new MemoryBoundFunctionVoteFactory(MBFVnames[i]);
	} catch (NoSuchAlgorithmException ex) {
	  fail(MBFVnames[i] + " threw " + ex.toString());
	}
      }
    }
    pollID = new byte[20];
    rand.nextBytes(pollID);
    try {
      voterID = new LcapIdentity("127.0.0.1", 1);
    } catch (UnknownHostException ex) {
      fail("LcapIdentity throws: " + ex.toString());
    }
  }

  /** tearDown method for test case
   * @throws Exception if XXX
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /*
   * Test construct
   */
  public void testConstructors() {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus = new MockCachedUrlSet();
    for (int j = 0; j < MBFVfactory.length; j++) {
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MemoryBoundFunctionVote mbfv =
	    MBFVfactory[j].generator(MBFfactory[i],
				     nonce,
				     3,
				     cus,
				     pollID,
				     voterID);
	} catch (MemoryBoundFunctionException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	} catch (NoSuchAlgorithmException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	}
      }
    }
    int[][] sVals = { { 1, 2 } };
    byte[][] hashVals = new byte[1][];
    hashVals[0] = nonce;
    for (int j = 0; j < MBFVfactory.length; j++) {
      for (int i = 0; i < MBFfactory.length; i++) {
	try {
	  MemoryBoundFunctionVote mbfv =
	    MBFVfactory[j].verifier(MBFfactory[i],
				    nonce,
				    3,
				    cus,
				    sVals,
				    hashVals,
				    pollID,
				    voterID);
	} catch (MemoryBoundFunctionException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	} catch (NoSuchAlgorithmException ex) {
	  fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	       " threw " + ex.toString());
	}
      }
    }
  }

  public void testAgreeingVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	agreeingVote(i, j);
  }
    
  public void testDisagreeingVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	disagreeingVote(i, j);
  }

  public void testShortVote() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	shortVote(i, j);
  }
    
  public void testInvalidNonce() {
    for (int j = 0; j < MBFVfactory.length; j++)
      for (int i = 0; i < MBFfactory.length; i++)
	invalidNonce(i, j);
  }
    
  private void agreeingVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = goodCUS(128);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCUS(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, true, true, 4096);
  }

  private void disagreeingVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = badCUS(128);
    if (cus1 == null)
      fail("badCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCUS(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, false, true, 4096);
  }

  private void shortVote(int i, int j) {
    byte[] nonce = new byte[4];
    rand.nextBytes(nonce);
    CachedUrlSet cus1 = goodCUS(128);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = shortCUS(128);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    onePair(i, j, cus1, cus2, nonce, nonce, false, true, 4096);
  }

  private void invalidNonce(int i, int j) {
    byte[] nonce1 = new byte[4];
    rand.nextBytes(nonce1);
    byte[] nonce2 = new byte[4];
    rand.nextBytes(nonce2);
    assertFalse(MessageDigest.isEqual(nonce1,nonce2));
    CachedUrlSet cus1 = goodCUS(50000);
    if (cus1 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    CachedUrlSet cus2 = goodCUS(50000);
    if (cus2 == null)
      fail("goodCUS() returned null " + MBFnames[i] + "," + MBFVnames[j]);
    int fail = 0;
    onePair(i, j, cus1, cus2, nonce1, nonce2, true, false, 1000000);
  }
  
  private void onePair(int i, int j,
		       CachedUrlSet cus1, 
		       CachedUrlSet cus2,
		       byte[] nonce1,
		       byte[] nonce2,
		       boolean shouldBeAgreeing,
		       boolean shouldBeValid,
		       int stepLimit) {
    // Make a generator
    MemoryBoundFunctionVote gen = generator(i, j, nonce1, cus1);
    assertFalse(gen==null);
    if (i == 0) {
      // Set the proof array that the MockMemoryBoundFunction will return
      // for generation and use for verification
      MockMemoryBoundFunction.setProof(goodProof);
    }
    // Generate the vote
    try {
      int s = stepLimit;
      while (s > 0 && gen.computeSteps(numSteps)) {
	assertFalse(gen.finished());
	s -= numSteps;
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("generator.computeSteps() threw " + ex.toString() + " for "  + MBFnames[i] + "," + MBFVnames[j]);
    }
    assertTrue(gen.finished());
    // Recover the vote
    int[][] proofArray = gen.getProofArray();
    if (proofArray == null)
      fail("generated a null proof array " + MBFnames[i] + "," + MBFVnames[j]);
    // Recover the hashes
    byte[][] hashArray = gen.getHashArray();
    if (hashArray == null)
      fail("generated a null proof array " + MBFnames[i] + "," + MBFVnames[j]);
    if (proofArray.length != hashArray.length)
      fail("proof " + proofArray.length + " hash " +
	   hashArray.length + " not equal " + MBFnames[i] + "," + MBFVnames[j]);
    // Make a verifier
    MemoryBoundFunctionVote ver = verifier(i, j, nonce2, cus2,
					   proofArray, hashArray);
    assertFalse(ver==null);
    if (i == 0) {
      // Set the proof array that the MockMemoryBoundFunction will return
      // for generation and use for verification
      MockMemoryBoundFunction.setProof(shouldBeValid ? goodProof : badProof);
    }
    if (j == 0) {
      ((MockMemoryBoundFunctionVote)ver).setValid(shouldBeValid);
      ((MockMemoryBoundFunctionVote)ver).setAgreeing(shouldBeAgreeing);
    }
    // Verify the vote
    try {
      int s = stepLimit;
      while (s > 0 && ver.computeSteps(numSteps)) {
	assertFalse(ver.finished());
	s -= numSteps;
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.computeSteps() threw " + ex.toString() + " for " + MBFnames[i] + "," + MBFVnames[j]);
    }
    assertTrue(ver.finished());
    // Get valid/invalid
    boolean valid = false;
    try {
      valid = ver.valid();
      if (shouldBeValid && !valid)
	fail("verifier declared valid vote invalid " + MBFnames[i] + "," + MBFVnames[j]);
      else if (!shouldBeValid && valid) {
	log.warning("verifier declared invalid vote valid (can happen)" +
		       MBFnames[i] + "," + MBFVnames[j]);
      }
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.valid() threw " + ex.toString());
    }
    // Get agreeing/disagreeing
    if (valid) try {
      boolean agreeing = ver.agreeing();
      if (shouldBeAgreeing && !agreeing)
	fail("verifier declared agreeing vote disgreeing " + MBFnames[i] + "," + MBFVnames[j]);
      else if (!shouldBeAgreeing && agreeing)
	fail("verifier declared disagreeing vote agreeing " + MBFnames[i] + "," + MBFVnames[j]);
    } catch (MemoryBoundFunctionException ex) {
      fail("verifier.agreeing() threw " + ex.toString() + " for " + MBFnames[i] + "," + MBFVnames[j]);
    }
  }

  MemoryBoundFunctionVote generator(int i, int j,
				    byte[] nonce,
				    CachedUrlSet cus) {
    if (cus == null)
      fail("generator() - no CUS");
    MemoryBoundFunctionVote ret = null;
    try {
      ret = MBFVfactory[j].generator(MBFfactory[i],
				     nonce,
				     3,
				     cus,
				     pollID,
				     voterID);
    } catch (MemoryBoundFunctionException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    } catch (NoSuchAlgorithmException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    }
    if (ret instanceof MockMemoryBoundFunctionVote) {
      int[][] prfs = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setProofs(prfs);
      byte[][] hashes = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setHashes(hashes);
      ((MockMemoryBoundFunctionVote)ret).setStepCount(256);
    }
    {
      ArchivalUnit au = cus.getArchivalUnit();
      if (au == null)
	fail("generator() - null AU");
      String auid = au.getAUId();
      if (auid == null)
	fail("generator() - null auID " + MBFnames[i] + "," + MBFVnames[j]);
      log.info("generator() " + MBFVnames[j] + ":" + MBFnames[i] +
	       " for " + auid);
    }
    return ret;
  }

  MemoryBoundFunctionVote verifier(int i, int j,
				   byte[] nonce,
				   CachedUrlSet cus,
				   int[][] proofArray,
				   byte[][] hashArray) {
    MemoryBoundFunctionVote ret = null;
    try {
      ret = MBFVfactory[j].verifier(MBFfactory[i],
				    nonce,
				    3,
				    cus,
				    proofArray,
				    hashArray,
				    pollID,
				    voterID);
    } catch (MemoryBoundFunctionException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    } catch (NoSuchAlgorithmException ex) {
      fail("MBFVfactory for " + MBFVnames[j] + ":" + MBFnames[i] +
	   " threw " + ex.toString());
    }
    if (ret instanceof MockMemoryBoundFunctionVote) {
      int[][] prfs = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setProofs(prfs);
      byte[][] hashes = {
	{1},
	{1,2},
	{1,2,3},
	{1,2,3,4},
	{1,2,3,4,5},
	{1,2,3,4,5,6},
	{1,2,3,4,5,6,7},
	{1,2,3,4,5,6,7,8},
	{1,2,3,4,5,6,7,8,9},
	{1,2,3,4,5,6,7,8,9,10}
      };
      ((MockMemoryBoundFunctionVote)ret).setHashes(hashes);
      ((MockMemoryBoundFunctionVote)ret).setStepCount(256);
    }
    {
      ArchivalUnit au = cus.getArchivalUnit();
      if (au == null)
	fail("verifier() - null AU");
      String auid = au.getAUId();
      if (auid == null)
	fail("verifier() - null auID " + MBFnames[i] + "," + MBFVnames[j]);
      log.info("verifier() " + MBFVnames[j] + ":" + MBFnames[i] +
	       " for " + auid);
    }
    return ret;
  }

  private CachedUrlSet goodCUS(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] content = new byte[bytes];
    for (int i = 0; i < content.length; i++)
      content[i] = goodContent[ i % goodContent.length];
    ret.setContentToBeHashed(content);
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  private CachedUrlSet badCUS(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] content = new byte[bytes];
    for (int i = 0; i < content.length; i++)
      content[i] = badContent[ i % badContent.length];
    ret.setContentToBeHashed(content); 
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  private CachedUrlSet shortCUS(int bytes) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId("TestMemoryBoundFunctionVote:goodAU");  // XXX
    MockCachedUrlSetHasher hash = new MockCachedUrlSetHasher(bytes);
    MockCachedUrlSet ret = new MockCachedUrlSet();
    byte[] nonce = new byte[bytes/2];
    for (int i = 0; i < nonce.length; i++)
      nonce[i] = goodContent[i % goodContent.length];
    ret.setContentToBeHashed(nonce); 
    ret.setArchivalUnit(au);
    ret.setContentHasher(hash);
    return (ret);
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {
        TestMemoryBoundFunctionVote.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
