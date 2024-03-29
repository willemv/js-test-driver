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
package com.google.jstestdriver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Provider;
import com.google.jstestdriver.browser.BrowserFileSet;
import com.google.jstestdriver.hooks.FileInfoScheme;
import com.google.jstestdriver.model.JstdTestCase;
import com.google.jstestdriver.model.NullPathPrefix;
import com.google.jstestdriver.util.NullStopWatch;

/**
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
public class JsTestDriverClientTest extends TestCase {
  private final Gson gson = new Gson();

  public static class ResponseStreamFactoryStub implements ResponseStreamFactory {

    private ResponseStream stream;

    public void setResponseStream(ResponseStream stream) {
      this.stream = stream;
    }

    @Override
    public ResponseStream getEvalActionResponseStream() {
      return stream;
    }

    public ResponseStream getLoadLibrariesActionResponseStream() {
      return stream;
    }

    public ResponseStream getLoadTestsActionResponseStream() {
      return stream;
    }

    @Override
    public ResponseStream getResetActionResponseStream() {
      return stream;
    }

    @Override
    public ResponseStream getDryRunActionResponseStream() {
      return stream;
    }

    @Override
    public ResponseStream getRunTestsActionResponseStream(String testId) {
      return stream;
    }

    @Override
    public ResponseStream getVisitActionResponseStream(String url) {
      return stream;
    }
  }

  public static class FakeResponseStream implements ResponseStream {

    private Response response;

    @Override
    public void finish() {
    }

    @Override
    public void stream(Response response) {
      this.response = response;
    }

    public Response getResponse() {
      return response;
    }
  }

  public void testSendCommand() throws Exception {
    MockServer server = new MockServer();
    server.expect("http://localhost/heartbeat?id=1", "OK");
    server.expect("http://localhost/fileSet?POST?{id=1, data=[], action=browserFileCheck}",
        gson.toJson(new BrowserFileSet()));
    server.expect("http://localhost/fileSet?POST?{data=[], action=serverFileCheck}", "[]");
    server.expect("http://localhost/cmd?POST?{data={\"command\":\"execute\","
        + "\"parameters\":[\"cmd\"]}, id=1}", "");
    server.expect("http://localhost/cmd?id=1", "{\"response\":"
        + "{\"response\":\"1\",\"browser\":{\"name\":\"browser1\"},"
        + "\"error\":\"error1\",\"executionTime\":3},\"last\":true}");

    server.expect("http://localhost/heartbeat?id=2", "OK");
    server.expect("http://localhost/fileSet?POST?{id=2, fileSet=[]}", "");
    server.expect("http://localhost/cmd?POST?{data={\"command\":\"execute\","
        + "\"parameters\":[\"cmd\"]}, id=2}", "");
    server.expect("http://localhost/cmd?id=2", "{\"response\":"
        + "{\"response\":\"2\",\"browser\":{\"name\":\"browser2\"},"
        + "\"error\":\"error2\",\"executionTime\":6},\"last\":true}");

    final NullStopWatch stopWatch = new NullStopWatch();
    CommandTaskFactory commandTaskFactory =
        new CommandTaskFactory(
            new DefaultFileFilter(),
            null,
            null,
            stopWatch,
            ImmutableSet.<FileInfoScheme>of(new HttpFileInfoScheme()),
            new NullPathPrefix());
    JsTestDriverClient client = new JsTestDriverClientImpl(commandTaskFactory, "http://localhost",
        server, false, null, new NullStopWatch());
    FakeResponseStream stream = new FakeResponseStream();

    client.eval("1", stream, "cmd",
        new JstdTestCase(Collections.<FileInfo>emptyList(),
            Collections.<FileInfo>emptyList(), java.util.Collections.<FileInfo> emptyList(), null));

    Response response = stream.getResponse();

    assertEquals("1", response.getResponse());
    assertEquals("browser1", response.getBrowser().getName());
    assertEquals("error1", response.getError());
    assertEquals(3L, response.getExecutionTime());

    client.eval("2", stream, "cmd",
        new JstdTestCase(Collections.<FileInfo>emptyList(), Collections.<FileInfo>emptyList(), java.util.Collections.<FileInfo> emptyList(), null));
    response = stream.getResponse();
    assertEquals("2", stream.getResponse().getResponse());
    assertEquals("browser2", response.getBrowser().getName());
    assertEquals("error2", response.getError());
    assertEquals(6L, response.getExecutionTime());
  }

  public void testGetListOfClients() throws Exception {
    MockServer server = new MockServer();
    server.expect("http://localhost/cmd?listBrowsers", "["
        + "{\"id\":0, \"name\":\"name0\", \"version\":\"ver0\", \"os\":\"os0\"},"
        + "{\"id\":1, \"name\":\"name1\", \"version\":\"ver1\", \"os\":\"os1\"}]");
    final NullStopWatch stopWatch = new NullStopWatch();
    CommandTaskFactory commandTaskFactory =
        new CommandTaskFactory(new DefaultFileFilter(), null, new Provider<HeartBeatManager>() {
          @Override
          public HeartBeatManager get() {
            return new HeartBeatManagerStub();
          }
        },
        stopWatch,
        ImmutableSet.<FileInfoScheme>of(new HttpFileInfoScheme()),
        new NullPathPrefix());
    JsTestDriverClient client = new JsTestDriverClientImpl(commandTaskFactory, "http://localhost",
        server, false, null, new NullStopWatch());
    Collection<BrowserInfo> browsersCollection = client.listBrowsers();
    List<BrowserInfo> browsers = new ArrayList<BrowserInfo>(browsersCollection);

    assertEquals(2, browsers.size());
    BrowserInfo browser0 = browsers.get(0);

    assertEquals(new Long(0), browser0.getId());
    assertEquals("name0", browser0.getName());
    assertEquals("ver0", browser0.getVersion());
    assertEquals("os0", browser0.getOs());

    BrowserInfo browser1 = browsers.get(1);

    assertEquals(new Long(1), browser1.getId());
    assertEquals("name1", browser1.getName());
    assertEquals("ver1", browser1.getVersion());
    assertEquals("os1", browser1.getOs());
  }

  public void testRunAllTest() throws Exception {
    MockServer server = new MockServer();
    String id = "1";
    JstdTestCase testCase =
        new JstdTestCase(Collections.<FileInfo>emptyList(), Collections.<FileInfo>emptyList(),
            java.util.Collections.<FileInfo>emptyList(), null);

    server.expect("http://localhost/heartbeat?id=" + id, "OK");
    server.expect(
        "http://localhost/fileSet?POST?{id=" + id + ", data=" + gson.toJson(testCase) + ", action=browserFileCheck}",
        gson.toJson(new BrowserFileSet()));

    server.expect("http://localhost/fileSet?POST?{data=[], action=serverFileCheck}", "[]");

    BrowserInfo browserInfo = new BrowserInfo();
    browserInfo.setId(Long.parseLong(id));
    browserInfo.setUploadSize(10);
    server.expect("http://localhost/cmd?listBrowsers", gson.toJson(Lists.newArrayList(browserInfo)));

    server.expect("http://localhost/cmd?POST?{data={\"command\":\"runTests\","
        + "\"parameters\":[\"[\\\"all\\\"]\",\"false\",\"\"]}, id=" + id + "}", "");
    server.expect("http://localhost/cmd?id=1", "{\"response\":{\"response\":\"PASSED\","
        + "\"browser\":{\"name\":\"browser\"},\"error\":\"error2\",\"executionTime\":123},"
        + "\"last\":true}");
    final NullStopWatch stopWatch = new NullStopWatch();
    CommandTaskFactory commandTaskFactory =
        new CommandTaskFactory(new DefaultFileFilter(), new MockFileLoader(), new Provider<HeartBeatManager>() {
          @Override
          public HeartBeatManager get() {
            return new HeartBeatManagerStub();
          }
        }, stopWatch, ImmutableSet.<FileInfoScheme>of(new HttpFileInfoScheme()), new NullPathPrefix());
    JsTestDriverClient client = new JsTestDriverClientImpl(commandTaskFactory, "http://localhost",
        server, false, null, new NullStopWatch());
    FakeResponseStream stream = new FakeResponseStream();

    client.runAllTests("1", stream, false,
        testCase);

    assertEquals("PASSED", stream.getResponse().getResponse());
  }

  public void testRunOneTest() throws Exception {
    JstdTestCase testCase = new JstdTestCase(Collections.<FileInfo>emptyList(), Collections.<FileInfo>emptyList(), java.util.Collections.<FileInfo> emptyList(), null);
    String id = "1";
    MockServer server = new MockServer();

    server.expect("http://localhost/heartbeat?id=1", "OK");
    server.expect("http://localhost/fileSet?POST?{id=1, data=" + gson.toJson(testCase) + ", action=browserFileCheck}",
        gson.toJson(new BrowserFileSet()));
    server.expect("http://localhost/fileSet?POST?{data=[], action=serverFileCheck}", "[]");
    server.expect(
            "http://localhost/cmd?POST?{data={\"command\":\"runTests\","
                + "\"parameters\":[\"[\\\"testCase.testFoo\\\",\\\"testCase.testBar\\\"]\",\"false\",\"\"]}, id=1}", "");
    BrowserInfo browserInfo = new BrowserInfo();
    browserInfo.setId(Long.parseLong(id));
    browserInfo.setUploadSize(10);
    server.expect("http://localhost/cmd?listBrowsers", gson.toJson(Lists.newArrayList(browserInfo)));
    server.expect("http://localhost/cmd?id=1", "{\"response\":{\"response\":\"PASSED\","
        + "\"browser\":{\"name\":\"browser\"},\"error\":\"error2\",\"executionTime\":123},"
        + "\"last\":true}");
    final NullStopWatch stopWatch = new NullStopWatch();
    DefaultFileFilter filter = new DefaultFileFilter();
    ImmutableSet<FileInfoScheme> schemes = ImmutableSet.<FileInfoScheme>of(new HttpFileInfoScheme());
    CommandTaskFactory commandTaskFactory =
        new CommandTaskFactory(filter,
            new MockFileLoader(),
            new Provider<HeartBeatManager>() {
          @Override
          public HeartBeatManager get() {
            return new HeartBeatManagerStub();
          }
        },
        stopWatch,
        schemes,
        new NullPathPrefix());
    JsTestDriverClient client = new JsTestDriverClientImpl(commandTaskFactory, "http://localhost",
        server, false, null, new NullStopWatch());
    FakeResponseStream stream = new FakeResponseStream();

    ArrayList<String> tests = new ArrayList<String>();

    tests.add("testCase.testFoo");
    tests.add("testCase.testBar");
    client.runTests("1", stream, tests, false,
        testCase);

    assertEquals("PASSED", stream.getResponse().getResponse());
  }
}
