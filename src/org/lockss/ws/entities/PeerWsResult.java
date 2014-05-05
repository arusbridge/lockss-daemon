/*
 * $Id: PeerWsResult.java,v 1.1.2.2 2014-05-05 17:32:30 wkwilson Exp $
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Container for the information related to a peer that is the result of a
 * query.
 */
package org.lockss.ws.entities;

import java.util.List;

public class PeerWsResult {
  private String peerId;
  private Long lastMessage;
  private String messageType;
  private Long messageCount;
  private Long lastPoll;
  private Long lastVote;
  private Long lastInvitation;
  private Long invitationCount;
  private Long pollsCalled;
  private Long votesCast;
  private Long pollsRejected;
  private String nakReason;
  private List<String> groups;
  private Boolean platformGroupMatch;

  public String getPeerId() {
    return peerId;
  }
  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }
  public Long getLastMessage() {
    return lastMessage;
  }
  public void setLastMessage(Long lastMessage) {
    this.lastMessage = lastMessage;
  }
  public String getMessageType() {
    return messageType;
  }
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }
  public Long getMessageCount() {
    return messageCount;
  }
  public void setMessageCount(Long messageCount) {
    this.messageCount = messageCount;
  }
  public Long getLastPoll() {
    return lastPoll;
  }
  public void setLastPoll(Long lastPoll) {
    this.lastPoll = lastPoll;
  }
  public Long getLastVote() {
    return lastVote;
  }
  public void setLastVote(Long lastVote) {
    this.lastVote = lastVote;
  }
  public Long getLastInvitation() {
    return lastInvitation;
  }
  public void setLastInvitation(Long lastInvitation) {
    this.lastInvitation = lastInvitation;
  }
  public Long getInvitationCount() {
    return invitationCount;
  }
  public void setInvitationCount(Long invitationCount) {
    this.invitationCount = invitationCount;
  }
  public Long getPollsCalled() {
    return pollsCalled;
  }
  public void setPollsCalled(Long pollsCalled) {
    this.pollsCalled = pollsCalled;
  }
  public Long getVotesCast() {
    return votesCast;
  }
  public void setVotesCast(Long votesCast) {
    this.votesCast = votesCast;
  }
  public Long getPollsRejected() {
    return pollsRejected;
  }
  public void setPollsRejected(Long pollsRejected) {
    this.pollsRejected = pollsRejected;
  }
  public String getNakReason() {
    return nakReason;
  }
  public void setNakReason(String nakReason) {
    this.nakReason = nakReason;
  }
  public List<String> getGroups() {
    return groups;
  }
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }
  public Boolean getPlatformGroupMatch() {
    return platformGroupMatch;
  }
  public void setPlatformGroupMatch(Boolean platformGroupMatch) {
    this.platformGroupMatch = platformGroupMatch;
  }

  @Override
  public String toString() {
    return "PeerWsResult [peerId=" + peerId + ", lastMessage=" + lastMessage
	+ ", messageType=" + messageType + ", messageCount=" + messageCount
	+ ", lastPoll=" + lastPoll + ", lastVote=" + lastVote
	+ ", lastInvitation=" + lastInvitation + ", invitationCount="
	+ invitationCount + ", pollsCalled=" + pollsCalled + ", votesCast="
	+ votesCast + ", pollsRejected=" + pollsRejected + ", nakReason="
	+ nakReason + ", groups=" + groups + ", platformGroupMatch="
	+ platformGroupMatch + "]";
  }
}
