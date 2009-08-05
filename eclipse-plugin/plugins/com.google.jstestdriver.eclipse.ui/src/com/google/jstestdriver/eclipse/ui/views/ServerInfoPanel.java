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
package com.google.jstestdriver.eclipse.ui.views;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.jstestdriver.eclipse.core.SlaveBrowserRootData;
import com.google.jstestdriver.eclipse.core.SlaveBrowserSet;
import com.google.jstestdriver.eclipse.ui.icon.Icons;

/**
 * Panel which displays info about the server, incuding status, capture url and
 * browsers captured.
 * 
 * @author shyamseshadri@google.com (Shyam Seshadri)
 */
public class ServerInfoPanel extends Composite implements Observer {

  public static final String SERVER_DOWN = "NOT RUNNING";
  private Text serverUrlText;
  private Label safariIcon;
  private Label chromeIcon;
  private Label ieIcon;
  private Label ffIcon;
  private Label operaIcon;
  private Icons icons;
  private static final Color NOT_RUNNING = new Color(Display.getCurrent(), 255, 102, 102);
  private static final Color NO_BROWSERS = new Color(Display.getCurrent(), 255, 255, 102);
  private static final Color READY = new Color(Display.getCurrent(), 102, 204, 102);

  public ServerInfoPanel(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(5, false));
    icons = new Icons();
    
    GridData textGridData = new GridData();
    textGridData.horizontalSpan = 5;
    textGridData.grabExcessHorizontalSpace = true;
    textGridData.horizontalAlignment = SWT.FILL;
    serverUrlText = new Text(this, SWT.CENTER);
    serverUrlText.setText(SERVER_DOWN);
    serverUrlText.setBackground(NOT_RUNNING);
    serverUrlText.setLayoutData(textGridData);
    serverUrlText.setEditable(false);
    serverUrlText.setOrientation(SWT.HORIZONTAL);
    GridData imgGridData = new GridData();
    imgGridData.horizontalSpan = 1;
    imgGridData.minimumHeight = 48;
    imgGridData.minimumWidth = 48;
    ffIcon = new Label(this, SWT.NONE);
    
    ffIcon.setLayoutData(imgGridData);
    ffIcon.setImage(icons.getFirefoxDisabledIcon());
    chromeIcon = new Label(this, SWT.NONE);
    chromeIcon.setLayoutData(imgGridData);
    chromeIcon.setImage(icons.getChromeDisabledIcon());
    safariIcon = new Label(this, SWT.NONE);
    safariIcon.setLayoutData(imgGridData);
    safariIcon.setImage(icons.getSafariDisabledIcon());
    ieIcon = new Label(this, SWT.NONE);
    ieIcon.setLayoutData(imgGridData);
    ieIcon.setImage(icons.getIEDisabledIcon());
    operaIcon = new Label(this, SWT.NONE);
    operaIcon.setLayoutData(imgGridData);
    operaIcon.setImage(icons.getOperaDisabledIcon());
  }

  public void update(Observable o, final Object arg) {
    final SlaveBrowserRootData data = (SlaveBrowserRootData) arg;
    new Thread(new Runnable() {
      public void run() {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            ffIcon.setImage(getImage(data.getFirefoxSlaves()));
            chromeIcon.setImage(getImage(data.getChromeSlaves()));
            ieIcon.setImage(getImage(data.getIeSlaves()));
            safariIcon.setImage(getImage(data.getSafariSlaves()));
            operaIcon.setImage(getImage(data.getOperaSlaves()));
            if (data.hasSlaves()) {
              serverUrlText.setBackground(READY);
            }
          }
        });
      }
    }).start();
  }
  
  public Image getImage(SlaveBrowserSet slave) {
    Image colored = icons.getImage(slave.getImagePath());
    if (slave.hasSlaves()) {
      return colored;
    } else {
      return new Image(Display.getCurrent(), colored, SWT.IMAGE_GRAY);
    }
  }

  public void setServerStarted(String serverUrl) {
    serverUrlText.setText(serverUrl);
    serverUrlText.setBackground(NO_BROWSERS);
  }

  public void setServerStopped() {
    serverUrlText.setText(SERVER_DOWN);
    serverUrlText.setBackground(NOT_RUNNING);
  }
}
