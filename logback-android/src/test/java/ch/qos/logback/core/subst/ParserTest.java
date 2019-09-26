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
package ch.qos.logback.core.subst;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import ch.qos.logback.core.spi.ScanException;

/**
 * Created with IntelliJ IDEA.
 * User: ceki
 * Date: 05.08.12
 * Time: 00:15
 * To change this template use File | Settings | File Templates.
 */
public class ParserTest {


  @Test
  public void literal() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("abc");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "abc");
    assertEquals(witness, node);
  }

  @Test
  public void literalWithAccolade0() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("{}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "{");
    witness.next = new Node(Node.Type.LITERAL, "}");
    assertEquals(witness, node);
  }

  @Test
  public void literalWithAccolade1() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("%x{a}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "%x");
    Node t = witness.next = new Node(Node.Type.LITERAL, "{");
    t.next = new Node(Node.Type.LITERAL, "a");
    t = t.next;
    t.next = new Node(Node.Type.LITERAL, "}");
    assertEquals(witness, node);
  }

  @Test
  public void literalWithTwoAccolades() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("%x{y} %a{b} c");

    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "%x");

    Node t = witness.next = new Node(Node.Type.LITERAL, "{");
    t.next = new Node(Node.Type.LITERAL, "y");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "}");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, " %a");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "{");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "b");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "}");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, " c");

    node.dump();
    System.out.println("");
    assertEquals(witness, node);
  }

  @Test
  public void variable() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("${abc}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.VARIABLE, new Node(Node.Type.LITERAL, "abc"));
    assertEquals(witness, node);
  }

  @Test
  public void literalVariableLiteral() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("a${b}c");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "a");
    witness.next = new Node(Node.Type.VARIABLE, new Node(Node.Type.LITERAL, "b"));
    witness.next.next = new Node(Node.Type.LITERAL, "c");
    assertEquals(witness, node);
  }


  // /LOGBACK-744
  @Test
  public void withColon() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("a:${b}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "a");
    Node t = witness.next = new Node(Node.Type.LITERAL, ":");
    t.next = new Node(Node.Type.VARIABLE, new Node(Node.Type.LITERAL, "b"));
    assertEquals(witness, node);
  }

  @Test
  public void nested() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("a${b${c}}d");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.LITERAL, "a");
    Node bLiteralNode = new Node(Node.Type.LITERAL, "b");
    Node cLiteralNode = new Node(Node.Type.LITERAL, "c");
    Node bVariableNode = new Node(Node.Type.VARIABLE, bLiteralNode);
    Node cVariableNode = new Node(Node.Type.VARIABLE, cLiteralNode);
    bLiteralNode.next = cVariableNode;

    witness.next = bVariableNode;
    witness.next.next = new Node(Node.Type.LITERAL, "d");
    assertEquals(witness, node);
  }

  @Test
  public void withDefault() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("${b:-c}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();
    Node witness = new Node(Node.Type.VARIABLE, new Node(Node.Type.LITERAL, "b"));
    witness.defaultPart = new Node(Node.Type.LITERAL, "c");
    assertEquals(witness, node);
  }

  @Test
  public void defaultSeparatorOutsideOfAVariable() throws ScanException {
    Tokenizer tokenizer = new Tokenizer("{a:-b}");
    Parser parser = new Parser(tokenizer.tokenize());
    Node node = parser.parse();

    dump(node);
    Node witness = new Node(Node.Type.LITERAL, "{");
    Node t = witness.next = new Node(Node.Type.LITERAL, "a");


    t.next = new Node(Node.Type.LITERAL, ":-");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "b");
    t = t.next;

    t.next = new Node(Node.Type.LITERAL, "}");

    assertEquals(witness, node);
  }

  @Test
  public void emptyTokenListDoesNotThrowNullPointerException() throws ScanException {
    // An empty token list would be returned from Tokenizer.tokenize()
    // if it were constructed with an empty string. The parser should
    // be able to handle this.
    Parser parser = new Parser(new ArrayList<Token>());
    parser.parse();
  }

  private void dump(Node node) {
    while (node != null) {
      System.out.println(node.toString());
      node = node.next;
    }
  }


}