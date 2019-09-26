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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.StatusChecker;
import org.junit.Test;

import ch.qos.logback.core.pattern.FormatInfo;

@SuppressWarnings("unchecked")
public class ParserTest {

  String BARE = Token.BARE_COMPOSITE_KEYWORD_TOKEN.getValue().toString();
  Context context = new ContextBase();


  @Test
  public void testBasic() throws Exception {
    Parser<Object> p = new Parser("hello");
    Node t = p.parse();
    assertEquals(Node.LITERAL, t.getType());
    assertEquals("hello", t.getValue());
  }

  @Test
  public void testKeyword() throws Exception {

    {
      Parser<Object> p = new Parser("hello%xyz");
      Node t = p.parse();
      Node witness = new Node(Node.LITERAL, "hello");
      witness.next = new SimpleKeywordNode("xyz");
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("hello%xyz{x}");
      Node t = p.parse();
      Node witness = new Node(Node.LITERAL, "hello");
      SimpleKeywordNode n = new SimpleKeywordNode("xyz");
      List<String> optionList = new ArrayList<String>();
      optionList.add("x");
      n.setOptions(optionList);
      witness.next = n;
      assertEquals(witness, t);
    }
  }

  @Test
  public void testComposite() throws Exception {
    {
      Parser<Object> p = new Parser("hello%(%child)");
      Node t = p.parse();

      Node witness = new Node(Node.LITERAL, "hello");
      CompositeNode composite = new CompositeNode(BARE);
      Node child = new SimpleKeywordNode("child");
      composite.setChildNode(child);
      witness.next = composite;

      // System.out.println("w:" + witness);
      // System.out.println(t);

      assertEquals(witness, t);
    }

    // System.out.println("testRecursive part 2");
    {
      Parser<Object> p = new Parser("hello%(%child )");
      Node t = p.parse();

      Node witness = new Node(Node.LITERAL, "hello");
      CompositeNode composite = new CompositeNode(BARE);
      Node child = new SimpleKeywordNode("child");
      composite.setChildNode(child);
      witness.next = composite;
      child.next = new Node(Node.LITERAL, " ");
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("hello%(%child %h)");
      Node t = p.parse();
      Node witness = new Node(Node.LITERAL, "hello");
      CompositeNode composite = new CompositeNode(BARE);
      Node child = new SimpleKeywordNode("child");
      composite.setChildNode(child);
      child.next = new Node(Node.LITERAL, " ");
      child.next.next = new SimpleKeywordNode("h");
      witness.next = composite;
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("hello%(%child %h) %m");
      Node t = p.parse();
      Node witness = new Node(Node.LITERAL, "hello");
      CompositeNode composite = new CompositeNode(BARE);
      Node child = new SimpleKeywordNode("child");
      composite.setChildNode(child);
      child.next = new Node(Node.LITERAL, " ");
      child.next.next = new SimpleKeywordNode("h");
      witness.next = composite;
      composite.next = new Node(Node.LITERAL, " ");
      composite.next.next = new SimpleKeywordNode("m");
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("hello%( %child \\(%h\\) ) %m");
      Node t = p.parse();
      Node witness = new Node(Node.LITERAL, "hello");
      CompositeNode composite = new CompositeNode(BARE);
      Node child = new Node(Node.LITERAL, " ");
      composite.setChildNode(child);
      Node c = child;
      c = c.next = new SimpleKeywordNode("child");
      c = c.next = new Node(Node.LITERAL, " (");
      c = c.next = new SimpleKeywordNode("h");
      c = c.next = new Node(Node.LITERAL, ") ");
      witness.next = composite;
      composite.next = new Node(Node.LITERAL, " ");
      composite.next.next = new SimpleKeywordNode("m");
      assertEquals(witness, t);
    }


  }

  @Test
  public void testNested() throws Exception {
    {
      Parser<Object> p = new Parser("%top %(%child%(%h))");
      Node t = p.parse();
      Node witness = new SimpleKeywordNode("top");
      Node w = witness.next = new Node(Node.LITERAL, " ");
      CompositeNode composite = new CompositeNode(BARE);
      w = w.next = composite;
      Node child = new SimpleKeywordNode("child");
      composite.setChildNode(child);
      composite = new CompositeNode(BARE);
      child.next = composite;
      composite.setChildNode(new SimpleKeywordNode("h"));

      assertEquals(witness, t);
    }
  }

  @Test
  public void testFormattingInfo() throws Exception {
    {
      Parser<Object> p = new Parser("%45x");
      Node t = p.parse();
      FormattingNode witness = new SimpleKeywordNode("x");
      witness.setFormatInfo(new FormatInfo(45, Integer.MAX_VALUE));
      assertEquals(witness, t);
    }
    {
      Parser<Object> p = new Parser("%4.5x");
      Node t = p.parse();
      FormattingNode witness = new SimpleKeywordNode("x");
      witness.setFormatInfo(new FormatInfo(4, 5));
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("%-4.5x");
      Node t = p.parse();
      FormattingNode witness = new SimpleKeywordNode("x");
      witness.setFormatInfo(new FormatInfo(4, 5, false, true));
      assertEquals(witness, t);
    }
    {
      Parser<Object> p = new Parser("%-4.-5x");
      Node t = p.parse();
      FormattingNode witness = new SimpleKeywordNode("x");
      witness.setFormatInfo(new FormatInfo(4, 5, false, false));
      assertEquals(witness, t);
    }

    {
      Parser<Object> p = new Parser("%-4.5x %12y");
      Node t = p.parse();
      FormattingNode witness = new SimpleKeywordNode("x");
      witness.setFormatInfo(new FormatInfo(4, 5, false, true));
      Node n = witness.next = new Node(Node.LITERAL, " ");
      n = n.next = new SimpleKeywordNode("y");
      ((FormattingNode) n).setFormatInfo(new FormatInfo(12, Integer.MAX_VALUE));
      assertEquals(witness, t);
    }
  }

  @Test
  public void testOptions0() throws Exception {
    Parser<Object> p = new Parser("%45x{'test '}");
    Node t = p.parse();
    SimpleKeywordNode witness = new SimpleKeywordNode("x");
    witness.setFormatInfo(new FormatInfo(45, Integer.MAX_VALUE));
    List<String> ol = new ArrayList<String>();
    ol.add("test ");
    witness.setOptions(ol);
    assertEquals(witness, t);
  }

  @Test
  public void testOptions1() throws Exception {
    Parser<Object> p = new Parser("%45x{a, b}");
    Node t = p.parse();
    SimpleKeywordNode witness = new SimpleKeywordNode("x");
    witness.setFormatInfo(new FormatInfo(45, Integer.MAX_VALUE));
    List<String> ol = new ArrayList<String>();
    ol.add("a");
    ol.add("b");
    witness.setOptions(ol);
    assertEquals(witness, t);
  }

  // see http://jira.qos.ch/browse/LBCORE-180
  @Test
  public void keywordGluedToLitteral() throws Exception {
    Parser<Object> p = new Parser("%x{}a");
    Node t = p.parse();
    SimpleKeywordNode witness = new SimpleKeywordNode("x");
    witness.setOptions(new ArrayList<String>());
    witness.next = new Node(Node.LITERAL, "a");
    assertEquals(witness, t);
  }

  @Test
  public void testCompositeFormatting() throws Exception {
    Parser<Object> p = new Parser("hello%5(XYZ)");
    Node t = p.parse();

    Node witness = new Node(Node.LITERAL, "hello");
    CompositeNode composite = new CompositeNode(BARE);
    composite.setFormatInfo(new FormatInfo(5, Integer.MAX_VALUE));
    Node child = new Node(Node.LITERAL, "XYZ");
    composite.setChildNode(child);
    witness.next = composite;

    assertEquals(witness, t);

  }

  @Test
  public void empty() {
    try {
      Parser<Object> p = new Parser("");
      p.parse();
      fail("");
    } catch (ScanException e) {

    }
  }

  @Test
  public void lbcore193() throws Exception {
    try {
      Parser<Object> p = new Parser("hello%(abc");
      p.setContext(context);
      p.parse();
      fail("where the is exception?");
    } catch (ScanException ise) {
      assertEquals("Expecting RIGHT_PARENTHESIS token but got null", ise.getMessage());
    }
    StatusChecker sc = new StatusChecker(context);
    sc.assertContainsMatch("Expecting RIGHT_PARENTHESIS");
    sc.assertContainsMatch("See also " + Parser.MISSING_RIGHT_PARENTHESIS);
  }

}