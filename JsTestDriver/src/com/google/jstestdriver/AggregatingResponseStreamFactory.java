/*
x * Copyright 2009 Google Inc.
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

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.List;
import java.util.Set;

/**
 * Aggregates a set of {@link ResponseStreamFactory}s by providing an AggregatingResponseStream.
 * @author corysmith@google.com (Cory Smith)
 */
public class AggregatingResponseStreamFactory implements ResponseStreamFactory {

  private final Set<ResponseStreamFactory> factories;

  @Inject
  public AggregatingResponseStreamFactory(
      Set<ResponseStreamFactory> factories) {
    this.factories = factories;
  }

  @Override
  public ResponseStream getDryRunActionResponseStream() {
    List<ResponseStream> streams = defaultStreams();
    for (ResponseStreamFactory factory : factories) {
      streams.add(factory.getDryRunActionResponseStream());
    }
    return new AggregatingResponseStream(streams);
  }

  private List<ResponseStream> defaultStreams() {
    final List<ResponseStream> defaultStreams = Lists.newLinkedList();
    defaultStreams.add(new BrowserPanicResponseStream());
    return defaultStreams;
  }

  @Override
  public ResponseStream getEvalActionResponseStream() {
    List<ResponseStream> streams = defaultStreams();
    for (ResponseStreamFactory factory : factories) {
      streams.add(factory.getEvalActionResponseStream());
    }
    return new AggregatingResponseStream(streams);
  }

  @Override
  public ResponseStream getResetActionResponseStream() {
    List<ResponseStream> streams = defaultStreams();
    for (ResponseStreamFactory factory : factories) {
      streams.add(factory.getResetActionResponseStream());
    }
    return new AggregatingResponseStream(streams);
  }

  @Override
  public ResponseStream getRunTestsActionResponseStream(String browserId) {
    List<ResponseStream> streams = defaultStreams();
    for (ResponseStreamFactory factory : factories) {
      streams.add(factory.getRunTestsActionResponseStream(browserId));
    }
    return new AggregatingResponseStream(streams);
  }

  @Override
  public ResponseStream getVisitActionResponseStream(String url) {
    List<ResponseStream> streams = defaultStreams();
    for (ResponseStreamFactory factory : factories) {
      streams.add(factory.getVisitActionResponseStream(url));
    }
    return new AggregatingResponseStream(streams);
  }

  public static class AggregatingResponseStream implements ResponseStream {

    private final List<ResponseStream> streams;

    public AggregatingResponseStream(List<ResponseStream> streams) {
      this.streams = streams;
    }

    @Override
    public void finish() {
      for (ResponseStream stream : streams) {
        stream.finish();
      }
    }

    @Override
    public void stream(Response response) {
      for (ResponseStream stream : streams) {
        stream.stream(response);
      }
    }

    @Override
    public String toString() {
      return "AggregatingResponseStream [streams=" + streams + "]";
    }
  }
}
