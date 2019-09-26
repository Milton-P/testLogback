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
package ch.qos.logback.core.pattern.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.FormatInfo;
import ch.qos.logback.core.pattern.IdentityCompositeConverter;
import ch.qos.logback.core.pattern.ReplacingCompositeConverter;
import ch.qos.logback.core.pattern.util.IEscapeUtil;
import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ScanException;

// ~=lamda
// E = TE|T

// Left factorization
// E = T(E|~)
// Eopt = E|~
// replace E|~ with Eopt in E
// E = TEopt

// T = LITERAL | '%' C | '%' FORMAT_MODIFIER C
// C = SIMPLE_KEYWORD OPTION | COMPOSITE_KEYWORD COMPOSITE
// OPTION = {...} | ~
// COMPOSITE = E ')' OPTION



public class Parser<E> extends ContextAwareBase {

  public final static String MISSING_RIGHT_PARENTHESIS = CoreConstants.CODES_URL+"#missingRightParenthesis";
  public final static Map<String, String> DEFAULT_COMPOSITE_CONVERTER_MAP = new HashMap<String, String>();
  public final static String REPLACE_CONVERTER_WORD = "replace";
  static {
    DEFAULT_COMPOSITE_CONVERTER_MAP.put(Token.BARE_COMPOSITE_KEYWORD_TOKEN.getValue().toString(),
            IdentityCompositeConverter.class.getName());
    DEFAULT_COMPOSITE_CONVERTER_MAP.put(REPLACE_CONVERTER_WORD,
             ReplacingCompositeConverter.class.getName());
  }

  final List<Token> tokenList;
  int pointer = 0;

  Parser(TokenStream ts) throws ScanException {
    this.tokenList = ts.tokenize();
  }

  public Parser(String pattern) throws ScanException {
    this(pattern, new RegularEscapeUtil());
  }

  public Parser(String pattern, IEscapeUtil escapeUtil) throws ScanException {
    try {
      TokenStream ts = new TokenStream(pattern, escapeUtil);
      this.tokenList = ts.tokenize();
    } catch (IllegalArgumentException npe) {
      throw new ScanException("Failed to initialize Parser", npe);
    }
  }

  /**
   * When the parsing step is done, the Node list can be transformed into a
   * converter chain.
   *
   * @param top the top node
   * @param converterMap converter map
   * @return the converter chain
   */
  public Converter<E> compile(final Node top, Map<String, String> converterMap) {
    Compiler<E> compiler = new Compiler<E>(top, converterMap);
    compiler.setContext(context);
    //compiler.setStatusManager(statusManager);
    return compiler.compile();
  }

  public Node parse() throws ScanException {
    return E();
  }

  // E = TEopt
  Node E() throws ScanException {
    Node t = T();
    if (t == null) {
      return null;
    }
    Node eOpt = Eopt();
    if (eOpt != null) {
      t.setNext(eOpt);
    }
    return t;
  }

  // Eopt = E|~
  Node Eopt() throws ScanException {
    // System.out.println("in Eopt()");
    Token next = getCurentToken();
    // System.out.println("Current token is " + next);
    if (next == null) {
      return null;
    } else {
      return E();
    }
  }

  // T = LITERAL | '%' C | '%' FORMAT_MODIFIER C
  Node T() throws ScanException {
    Token t = getCurentToken();
    expectNotNull(t, "a LITERAL or '%'");

    switch (t.getType()) {
    case Token.LITERAL:
      advanceTokenPointer();
      return new Node(Node.LITERAL, t.getValue());
    case Token.PERCENT:
      advanceTokenPointer();
      // System.out.println("% token found");
      FormatInfo fi;
      Token u = getCurentToken();
      FormattingNode c;
      expectNotNull(u, "a FORMAT_MODIFIER, SIMPLE_KEYWORD or COMPOUND_KEYWORD");
      if (u.getType() == Token.FORMAT_MODIFIER) {
        fi = FormatInfo.valueOf((String) u.getValue());
        advanceTokenPointer();
        c = C();
        c.setFormatInfo(fi);
      } else {
        c = C();
      }
      return c;

    default:
      return null;

    }

  }

  FormattingNode C() throws ScanException {
    Token t = getCurentToken();
    // System.out.println("in C()");
    // System.out.println("Current token is " + t);
    expectNotNull(t, "a LEFT_PARENTHESIS or KEYWORD");
    int type = t.getType();
    switch (type) {
    case Token.SIMPLE_KEYWORD:
      return SINGLE();
    case Token.COMPOSITE_KEYWORD:
      advanceTokenPointer();
      return COMPOSITE(t.getValue().toString());
    default:
      throw new IllegalStateException("Unexpected token " + t);
    }
  }

  @SuppressWarnings("unchecked")
  FormattingNode SINGLE() throws ScanException {
    // System.out.println("in SINGLE()");
    Token t = getNextToken();
    // System.out.println("==" + t);
    SimpleKeywordNode keywordNode = new SimpleKeywordNode(t.getValue());

    Token ot = getCurentToken();
    if (ot != null && ot.getType() == Token.OPTION) {
      List<String> optionList = ot.getOptionsList();
      keywordNode.setOptions(optionList);
      advanceTokenPointer();
    }
    return keywordNode;
  }

  @SuppressWarnings("unchecked")
  FormattingNode COMPOSITE(String keyword) throws ScanException {
    CompositeNode compositeNode = new CompositeNode(keyword);

    Node childNode = E();
    compositeNode.setChildNode(childNode);

    Token t = getNextToken();

    if (t == null || t.getType() != Token.RIGHT_PARENTHESIS) {
        String msg = "Expecting RIGHT_PARENTHESIS token but got " + t;
        addError(msg);
        addError("See also "+MISSING_RIGHT_PARENTHESIS);
        throw new ScanException(msg);
    }
    Token ot = getCurentToken();
    if (ot != null && ot.getType() == Token.OPTION) {
      List<String> optionList = ot.getOptionsList();
      compositeNode.setOptions(optionList);
      advanceTokenPointer();
    }
    return compositeNode;
  }

  Token getNextToken() {
    if (pointer < tokenList.size()) {
      return (Token) tokenList.get(pointer++);
    }
    return null;
  }

  Token getCurentToken() {
    if (pointer < tokenList.size()) {
      return (Token) tokenList.get(pointer);
    }
    return null;
  }

  void advanceTokenPointer() {
    pointer++;
  }

  void expectNotNull(Token t, String expected) {
    if (t == null) {
      throw new IllegalStateException("All tokens consumed but was expecting "
          + expected);
    }
  }

//  public void setStatusManager(StatusManager statusManager) {
//    this.statusManager = statusManager;
//  }
}