package com.google.jstestdriver;

import com.google.jstestdriver.model.JstdTestCase;
import com.google.jstestdriver.model.RunData;

import java.io.PrintStream;

/**
 * <strong><font color="#FF0000">TODO JavaDoc.</font></strong>
 */
public class VisitAction implements BrowserAction {
  
  private final String url;
  private final ResponseStreamFactory responseStreamFactory;

  public VisitAction(ResponseStreamFactory responseStreamFactory, String url) {
    this.url = url;
    this.responseStreamFactory = responseStreamFactory;
  }

  @Override
  public ResponseStream run(String browserId, JsTestDriverClient client, RunData runData, JstdTestCase testCase) {
    ResponseStream responseStream = responseStreamFactory.getVisitActionResponseStream(url);
    client.visit(browserId, responseStream, url, testCase);
    return responseStream;
  }

  public static class VisitActionResponseStream implements ResponseStream {
    
    private final PrintStream out;
    private final String url;

    public VisitActionResponseStream(PrintStream out, String url) {
      this.out = out;
      this.url = url;
    }

    @Override
    public void stream(Response response) {
      String browserName = response.getBrowser().getName();
      out.println(String.format("%s: visiting %s", browserName, url));
    }

    @Override
    public void finish() {
    }
  }
}
