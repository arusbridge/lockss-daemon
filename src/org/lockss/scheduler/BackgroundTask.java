/*
 * $Id: BackgroundTask.java,v 1.4 2003-11-20 01:08:16 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.scheduler;

import java.util.*;
import org.lockss.util.*;

/** Description of a background task that reduces the CPU time available to
 * a foreground (stepped) task. */
public class BackgroundTask extends SchedulableTask {
  final double loadFactor;
  final TaskCallback callback;
  TaskRunner runner = null;

  public BackgroundTask(Deadline start,
			Deadline stop,
			double loadFactor,
			TaskCallback callback) {

    super(start, stop, stop.minus(start), null, null);
    this.loadFactor = loadFactor;
    this.callback = callback;
  }

  public boolean isBackgroundTask() {
    return true;
  }

  public Deadline getStart() {
    return getEarlistStart();
  }

  public Deadline getFinish() {
    return getLatestFinish();
  }

  public double getLoadFactor() {
    return loadFactor;
  }

  void setTaskRunner(TaskRunner runner) {
    this.runner = runner;
  }

  public void taskIsFinished() {
    if (runner != null) {
      runner.backgroundTaskIsFinished(this);
    }
  }

  public String getShortText() {
    return "Bckgnd";
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[BTask: ");
    sb.append(loadFactor);
    sb.append(" ");
    toStringCommon(sb);
    sb.append("]");
    return sb.toString();
  }

}


