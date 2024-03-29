/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.jstestdriver.server.handlers;

import static org.easymock.EasyMock.expect;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.Instant;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.jstestdriver.BrowserInfo;
import com.google.jstestdriver.CapturedBrowsers;
import com.google.jstestdriver.MockTime;
import com.google.jstestdriver.Response;
import com.google.jstestdriver.Response.ResponseType;
import com.google.jstestdriver.SlaveBrowser;
import com.google.jstestdriver.SlaveBrowser.BrowserState;
import com.google.jstestdriver.StreamMessage;
import com.google.jstestdriver.Time;
import com.google.jstestdriver.TimeImpl;
import com.google.jstestdriver.browser.BrowserIdStrategy;
import com.google.jstestdriver.runner.RunnerType;

/**
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
public class CommandGetHandlerTest extends TestCase {

  public void testListBrowsers() throws Exception {
    CapturedBrowsers capturedBrowsers = new CapturedBrowsers(new BrowserIdStrategy(new MockTime(0)));
    BrowserInfo browserInfo = new BrowserInfo();
    browserInfo.setId(1L);
    SlaveBrowser slave =
        new SlaveBrowser(new TimeImpl(), "1", browserInfo, 20, null, CaptureHandler.QUIRKS,
            RunnerType.CLIENT, BrowserState.CAPTURED, new Instant(0));

    capturedBrowsers.addSlave(slave);
    CommandGetHandler handler = new CommandGetHandler(null, null, new Gson(), capturedBrowsers);

    assertEquals(new Gson().toJson(Lists.newArrayList(slave.getBrowserInfo())),
      handler.listBrowsers());
  }

  public void testListBrowsersWithBrowserRunnerType() throws Exception {
    CapturedBrowsers capturedBrowsers = new CapturedBrowsers(new BrowserIdStrategy(new MockTime(0)));
    BrowserInfo browserInfo1 = new BrowserInfo();
    browserInfo1.setId(1L);
    SlaveBrowser slave1 =
        new SlaveBrowser(new TimeImpl(), "1", browserInfo1, 20, null, CaptureHandler.QUIRKS,
            RunnerType.CLIENT, BrowserState.CAPTURED, new Instant(0));
    capturedBrowsers.addSlave(slave1);

    BrowserInfo browserInfo2 = new BrowserInfo();
    browserInfo1.setId(2L);
    SlaveBrowser slave2 =
        new SlaveBrowser(new TimeImpl(), "2", browserInfo2, 20, null, CaptureHandler.QUIRKS,
            RunnerType.BROWSER, BrowserState.CAPTURED, new Instant(0));
    capturedBrowsers.addSlave(slave2);

    CommandGetHandler handler = new CommandGetHandler(null, null, new Gson(), capturedBrowsers);

    assertEquals(new Gson().toJson(Lists.newArrayList(slave1.getBrowserInfo())),
      handler.listBrowsers());
  }

  public void testBrowserPanic() throws Exception {
    CapturedBrowsers capturedBrowsers = new CapturedBrowsers(new BrowserIdStrategy(new MockTime(0)));
    BrowserInfo browserInfo = new BrowserInfo();
    browserInfo.setId(1L);

    CharArrayWriter out = new CharArrayWriter();
    PrintWriter writer = new PrintWriter(out);

    IMocksControl control = EasyMock.createControl();

    HttpServletResponse response = control.createMock(HttpServletResponse.class);
    expect(response.getWriter()).andReturn(writer).anyTimes();
    HttpServletRequest request = control.createMock(HttpServletRequest.class);
    expect(request.getParameter("listBrowsers")).andReturn(null);
    expect(request.getParameter("nextBrowserId")).andReturn(null);
    expect(request.getParameter("id")).andReturn(browserInfo.getId().toString());
    control.replay();

    SlaveBrowser slave = new SlaveBrowser(new Time() {
      int i = 0;

      public Instant now() {
        i += 100;
        return new Instant(i);
      }
    }, "1", browserInfo, 0, null, CaptureHandler.QUIRKS, RunnerType.CLIENT, BrowserState.CAPTURED, new Instant(0));
    slave.addResponse(new Response(ResponseType.LOG.name(), "", browserInfo, "", -1), true);
    capturedBrowsers.addSlave(slave);
    Gson gson = new Gson();
    CommandGetHandler handler = new CommandGetHandler(request, response, gson, capturedBrowsers);
    handler.handleIt();
    assertEquals(ResponseType.BROWSER_PANIC, gson.fromJson(out.toString(), StreamMessage.class)
        .getResponse().getResponseType());
  }
}
