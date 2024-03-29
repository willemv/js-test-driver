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
/**
 * @fileoverview A series of constants to correlate to the type of responses
 * between the browser and the server.
 * 
 * See com.google.jstestdriver.Response.GSON_TYPES.
 * @author corbinrsmith@gmail.com (Cory Smith)
 */

/**
 * @enum {string}
 */
jstestdriver.RESPONSE_TYPES = {
  FILE_LOAD_RESULT: 'FILE_LOAD_RESULT',
  REGISTER_RESULT: 'REGISTER_RESULT',
  TEST_RESULT: 'TEST_RESULT',
  TEST_QUERY_RESULT: 'TEST_QUERY_RESULT',
  RESET_RESULT: 'RESET_RESULT',
  COMMAND_RESULT: 'COMMAND_RESULT',
  BROWSER_READY: 'BROWSER_READY',
  BROWSER_PANIC: 'BROWSER_PANIC',
  NOOP: 'NOOP',
  LOG: 'LOG',
  VISIT: 'VISIT'
};


/**
 * Contains the state of a response.
 * This is the javascript twin to com.google.jstestdriver.Response.
 * 
 * @param {jstestdriver.RESPONSE_TYPES} type The type of the response.
 * @param {String} response The serialized contents of the response.
 * @param {jstestdriver.BrowserInfo} browser The browser information. 
 * @param {Boolean} start Is this the first response from the browser.
 * @constructor
 */
jstestdriver.Response = function(type, response, browser, start) {
  this.type = type;
  this.response = response;
  this.browser = browser;
  if (start) {
    this.start = true;
  }
};


jstestdriver.Response.prototype.toString = function() {
  return 'Response(\nresponse=' + this.response + ',\ntype' + this.type + ',\n browser=' + this.browser + ')';
};


/**
 * @param {String} done Indicates if this is the last streamed message.
 * @param {jstestdriver.Response} response The response.
 * @constructor
 */
jstestdriver.CommandResponse = function (done, response) {
  this.done = done;
  this.response = response;
};


/**
 * Represents the information about the browser.
 * @param {Number} id The unique id of this browser.
 * @constructor
 */
jstestdriver.BrowserInfo = function(id) {
  this.id = id;
};
