/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.joran.event;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static ch.qos.logback.core.CoreConstants.XML_PARSING;

import ch.qos.logback.core.joran.spi.ElementPath;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.sax2.Driver;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareImpl;
import ch.qos.logback.core.status.Status;

public class SaxEventRecorder extends DefaultHandler implements ContextAware {

  private final ContextAwareImpl cai;

  public SaxEventRecorder() {
    cai = new ContextAwareImpl(null, this);
  }

  public SaxEventRecorder(Context context) {
    cai = new ContextAwareImpl(context, this);
  }

  private List<SaxEvent> saxEventList = new ArrayList<SaxEvent>();
  private Locator locator;
  ElementPath globalElementPath = new ElementPath();

  final public void recordEvents(InputStream inputStream) throws JoranException {
    recordEvents(new InputSource(inputStream));
  }

  public List<SaxEvent> recordEvents(InputSource inputSource)
      throws JoranException {
    Driver parser = buildPullParser();
    try {
      parser.setContentHandler((ContentHandler) this);
      parser.setErrorHandler(this);
      parser.parse(inputSource);
      return saxEventList;
    } catch (EOFException eof) {
      handleError(eof.getLocalizedMessage(),
          new SAXParseException(eof.getLocalizedMessage(), locator, eof));
    } catch (IOException ie) {
      handleError("I/O error occurred while parsing xml file", ie);
    } catch(SAXException se) {
      // Exception added into StatusManager via Sax error handling. No need to add it again
      throw new JoranException("Problem parsing XML document. See previously reported errors.", se);
    } catch (Exception ex) {
      handleError("Unexpected exception while parsing XML document.", ex);
    }
    throw new IllegalStateException("This point can never be reached");
  }

  private void handleError(String errMsg, Throwable t) throws JoranException {
    addError(errMsg, t);
    throw new JoranException(errMsg, t);
  }

  private Driver buildPullParser() throws JoranException {
    try {
      Driver driver = new Driver();
      try {
         driver.setFeature("http://xml.org/sax/features/validation", false);
      } catch (SAXNotSupportedException e) {
         // this is ok...we're trying to disable validation, so if it's not
         // supported, that's even better
      }
      driver.setFeature("http://xml.org/sax/features/namespaces", true);
      return driver;
    } catch (Exception pce) {
      String errMsg = "Parser configuration error occurred";
      addError(errMsg, pce);
      throw new JoranException(errMsg, pce);
    }
  }

  public void startDocument() {
  }

  public Locator getLocator() {
    return locator;
  }

  public void setDocumentLocator(Locator l) {
    locator = l;
  }

  public void startElement(String namespaceURI, String localName, String qName,
      Attributes atts) {

    String q = qName == null || qName.length() == 0 ? localName : qName;
    String tagName = getTagName(localName, q);
    globalElementPath.push(tagName);
    ElementPath current = globalElementPath.duplicate();
    saxEventList.add(new StartEvent(current, namespaceURI, localName, q,
        atts, getLocator()));
  }

  public void characters(char[] ch, int start, int length) {
    String bodyStr = new String(ch, start, length);
    SaxEvent lastEvent = getLastEvent();
    if (lastEvent instanceof BodyEvent) {
      BodyEvent be = (BodyEvent) lastEvent;
      be.append(bodyStr);
    } else {
      // ignore space only text if the previous event is not a BodyEvent
      if (!isSpaceOnly(bodyStr)) {
        saxEventList.add(new BodyEvent(bodyStr, getLocator()));
      }
    }
  }

  boolean isSpaceOnly(String bodyStr) {
    String bodyTrimmed = bodyStr.trim();
    return (bodyTrimmed.length() == 0);
  }

  SaxEvent getLastEvent() {
    if (saxEventList.isEmpty()) {
      return null;
    }
    int size = saxEventList.size();
    return saxEventList.get(size - 1);
  }

  public void endElement(String namespaceURI, String localName, String qName) {
    String q = qName == null || qName.length() == 0 ? localName : qName;
    saxEventList
        .add(new EndEvent(namespaceURI, localName, q, getLocator()));
    globalElementPath.pop();
  }

  String getTagName(String localName, String qName) {
    String tagName = localName;
    if ((tagName == null) || (tagName.length() < 1)) {
      tagName = qName;
    }
    return tagName;
  }

  public void error(SAXParseException spe) throws SAXException {
    addError(XML_PARSING +" - Parsing error on line " + spe.getLineNumber() + " and column "
        + spe.getColumnNumber(), spe);
  }

  public void fatalError(SAXParseException spe) throws SAXException {
    addError(XML_PARSING +" - Parsing fatal error on line " + spe.getLineNumber()
        + " and column " + spe.getColumnNumber(), spe);
  }

  public void warning(SAXParseException spe) throws SAXException {
    addWarn(XML_PARSING +" - Parsing warning on line " + spe.getLineNumber() + " and column "
        + spe.getColumnNumber(), spe);
  }

  public void addError(String msg) {
    cai.addError(msg);
  }

  public void addError(String msg, Throwable ex) {
    cai.addError(msg, ex);
  }

  public void addInfo(String msg) {
    cai.addInfo(msg);
  }

  public void addInfo(String msg, Throwable ex) {
    cai.addInfo(msg, ex);
  }

  public void addStatus(Status status) {
    cai.addStatus(status);
  }

  public void addWarn(String msg) {
    cai.addWarn(msg);
  }

  public void addWarn(String msg, Throwable ex) {
    cai.addWarn(msg, ex);
  }

  public Context getContext() {
    return cai.getContext();
  }

  public void setContext(Context context) {
    cai.setContext(context);
  }

  public List<SaxEvent> getSaxEventList() {
    return saxEventList;
  }

}
