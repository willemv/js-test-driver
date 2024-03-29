/*
 * Copyright 2010 Google Inc.
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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.jstestdriver.BrowserInfo;
import com.google.jstestdriver.BrowserPanic;
import com.google.jstestdriver.CapturedBrowsers;
import com.google.jstestdriver.Response;
import com.google.jstestdriver.SlaveBrowser;
import com.google.jstestdriver.StreamMessage;
import com.google.jstestdriver.requesthandlers.RequestHandler;
import com.google.jstestdriver.runner.RunnerType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
class CommandGetHandler implements RequestHandler {
  private static Logger logger = LoggerFactory.getLogger(CommandGetHandler.class);

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final Gson gson;
  private final CapturedBrowsers capturedBrowsers;

  @Inject
  public CommandGetHandler(
      HttpServletRequest request,
      HttpServletResponse response,
      Gson gson,
      CapturedBrowsers capturedBrowsers) {
    this.request = request;
    this.response = response;
    this.gson = gson;
    this.capturedBrowsers = capturedBrowsers;
  }

  public void handleIt() throws IOException {
    // TODO(corysmith): move these command to their own handler.
    if (request.getParameter("listBrowsers") != null) {
      response.getWriter().write(listBrowsers());
    } else if (request.getParameter("nextBrowserId") != null) {
      response.getWriter().write(capturedBrowsers.getUniqueId());
    } else {
      streamResponse(request.getParameter("id"), response.getWriter());
    }
    response.getWriter().flush();
  }

  public String listBrowsers() {
    List<BrowserInfo> browsers = Lists.newArrayList();
    for (SlaveBrowser browser : capturedBrowsers.getSlaveBrowsers()) {
      if (browser.getRunnerType() != RunnerType.BROWSER) {
        browsers.add(browser.getBrowserInfo());
      }
    }
    return gson.toJson(browsers);
  }

  private void streamResponse(String id, PrintWriter writer) {
    SlaveBrowser browser = capturedBrowsers.getBrowser(id);
    writer.write(gson.toJson(getResponse(browser)));
  }

  private StreamMessage getResponse(SlaveBrowser browser) {
    StreamMessage cmdResponse = null;

    while (cmdResponse == null) {
      if (!browser.isAlive()) {
        SlaveBrowser deadBrowser = capturedBrowsers.getBrowser(browser.getId());
        capturedBrowsers.removeSlave(browser.getId());
        Response response = new Response();

        BrowserInfo browserInfo = deadBrowser.getBrowserInfo();
        response.setBrowser(browserInfo);
        response.setResponse(
            gson.toJson(
                new BrowserPanic(browserInfo,
                    String.format("Browser unresponsive since %s during %s",
                        browser.getLastHeartbeat(),
                        browser.getCommandRunning()))));
        response.setType(BrowserPanic.TYPE_NAME);
        return new StreamMessage(true, response);
      }
      cmdResponse = substituteBrowserInfo(browser.getResponse());
    }
    return cmdResponse;
  }

  private StreamMessage substituteBrowserInfo(StreamMessage cmdResponse) {
    Response response = cmdResponse.getResponse();

      SlaveBrowser slaveBrowser =
          capturedBrowsers.getBrowser(response.getBrowser().getId().toString());
    if (slaveBrowser != null) {
      response.setBrowser(slaveBrowser.getBrowserInfo());
    } else {
      BrowserInfo nullBrowserInfo = new BrowserInfo();
      nullBrowserInfo.setId(response.getBrowser().getId());
      nullBrowserInfo.setName("unknown browser");
      nullBrowserInfo.setVersion("unknown version");
      nullBrowserInfo.setOs("unknown os");
      response.setBrowser(nullBrowserInfo);
    }
    return new StreamMessage(cmdResponse.isLast(), response);
  }
}
