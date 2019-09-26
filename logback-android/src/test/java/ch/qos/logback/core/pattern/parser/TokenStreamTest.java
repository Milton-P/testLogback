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

import ch.qos.logback.core.spi.ScanException;
import org.junit.Test;

import ch.qos.logback.core.pattern.util.AlmostAsIsEscapeUtil;

public class TokenStreamTest {

  @Test
  public void testEmpty() throws ScanException {
    try {
      new TokenStream("").tokenize();
      fail("empty string not allowed");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testSingleLiteral() throws ScanException {
    List<Token> tl = new TokenStream("hello").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(new Token(Token.LITERAL, "hello"));
    assertEquals(witness, tl);
  }

  @Test
  public void testLiteralWithPercent() throws ScanException {
    {
      List<Token> tl = new TokenStream("hello\\%world").tokenize();

      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "hello%world"));
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("hello\\%").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "hello%"));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("\\%").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "%"));
      assertEquals(witness, tl);
    }
  }

  @Test
  public void testBasic() throws ScanException {

    // test "%c"
    {
      List<Token> tl = new TokenStream("%c").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "c"));
      assertEquals(witness, tl);
    }

    {
      // test "xyz%-34c"
      List<Token> tl = new TokenStream("%a%b").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "a"));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "b"));
      assertEquals(witness, tl);
    }

    {
      // test "xyz%-34c"
      List<Token> tl = new TokenStream("xyz%-34c").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "xyz"));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.FORMAT_MODIFIER, "-34"));
      witness.add(new Token(Token.SIMPLE_KEYWORD, "c"));
      assertEquals(witness, tl);
    }
  }

  @Test
  public void testComplexNR() throws ScanException {
    List<Token> tl = new TokenStream("%d{1234} [%34.-67toto] %n").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "d"));
    List<String> ol = new ArrayList<String>();
    ol.add("1234");
    witness.add(new Token(Token.OPTION, ol));
    witness.add(new Token(Token.LITERAL, " ["));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.FORMAT_MODIFIER, "34.-67"));
    witness.add(new Token(Token.SIMPLE_KEYWORD, "toto"));
    witness.add(new Token(Token.LITERAL, "] "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "n"));
    assertEquals(witness, tl);
  }

  @Test
  public void testEmptyP() throws ScanException {
    List<Token> tl = new TokenStream("()").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(new Token(Token.LITERAL, "("));
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    assertEquals(witness, tl);
  }

  @Test
  public void testEmptyP2() throws ScanException {
    List<Token> tl = new TokenStream("%()").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(Token.PERCENT_TOKEN);
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    assertEquals(witness, tl);
  }

  @Test
  public void testEscape() throws ScanException {
    {
      List<Token> tl = new TokenStream("\\%").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "%"));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("\\%\\(\\t\\)\\r\\n").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "%(\t)\r\n"));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("\\\\%x").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "\\"));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("%x\\)").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      witness.add(new Token(Token.LITERAL, ")"));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("%x\\_a").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      witness.add(new Token(Token.LITERAL, "a"));
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("%x\\_%b").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "b"));
      assertEquals(witness, tl);
    }
  }

  @Test
  public void testOptions() throws ScanException {
    {
      List<Token> tl = new TokenStream("%x{t}").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      List<String> ol = new ArrayList<String>();
      ol.add("t");
      witness.add(new Token(Token.OPTION, ol));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("%x{t,y}").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      List<String> ol = new ArrayList<String>();
      ol.add("t");
      ol.add("y");
      witness.add(new Token(Token.OPTION, ol));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("%x{\"hello world.\", \"12y  \"}").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      List<String> ol = new ArrayList<String>();
      ol.add("hello world.");
      ol.add("12y  ");
      witness.add(new Token(Token.OPTION, ol));
      assertEquals(witness, tl);
    }

    {
      List<Token> tl = new TokenStream("%x{'opt}'}").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "x"));
      List<String> ol = new ArrayList<String>();
      ol.add("opt}");
      witness.add(new Token(Token.OPTION, ol));
      assertEquals(witness, tl);
    }
  }

  @Test
  public void testSimpleP() throws ScanException {
    List<Token> tl = new TokenStream("%(hello %class{.4?})").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(Token.PERCENT_TOKEN);
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(new Token(Token.LITERAL, "hello "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "class"));
    List<String> ol = new ArrayList<String>();
    ol.add(".4?");
    witness.add(new Token(Token.OPTION, ol));
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    assertEquals(witness, tl);
  }

  @Test
  public void testSimpleP2() throws ScanException {
    List<Token> tl = new TokenStream("X %a %-12.550(hello %class{.4?})").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(new Token(Token.LITERAL, "X "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "a"));
    witness.add(new Token(Token.LITERAL, " "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.FORMAT_MODIFIER, "-12.550"));
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(new Token(Token.LITERAL, "hello "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "class"));
    List<String> ol = new ArrayList<String>();
    ol.add(".4?");
    witness.add(new Token(Token.OPTION, ol));
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    assertEquals(witness, tl);
  }

  @Test
  public void testMultipleRecursion() throws ScanException {
    List<Token> tl = new TokenStream("%-1(%d %45(%class %file))").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.FORMAT_MODIFIER, "-1"));
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "d"));
    witness.add(new Token(Token.LITERAL, " "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.FORMAT_MODIFIER, "45"));
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "class"));
    witness.add(new Token(Token.LITERAL, " "));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "file"));
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);

    assertEquals(witness, tl);
  }

  @Test
  public void testNested() throws ScanException {
    List<Token> tl = new TokenStream("%(%a%(%b))").tokenize();
    List<Token> witness = new ArrayList<Token>();
    witness.add(Token.PERCENT_TOKEN);
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "a"));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "b"));
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
    witness.add(Token.RIGHT_PARENTHESIS_TOKEN);

    assertEquals(witness, tl);

  }

  @Test
  public void testEscapedParanteheses() throws ScanException {
    {
      List<Token> tl = new TokenStream("\\(%h\\)").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "("));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "h"));
      witness.add(new Token(Token.LITERAL, ")"));
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("(%h\\)").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "("));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "h"));
      witness.add(new Token(Token.LITERAL, ")"));
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("%a(x\\)").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.COMPOSITE_KEYWORD, "a"));
      witness.add(new Token(Token.LITERAL, "x)"));
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("%a\\(x)").tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "a"));
      witness.add(new Token(Token.LITERAL, "(x"));
      witness.add(new Token(Token.RIGHT_PARENTHESIS));

      assertEquals(witness, tl);
    }
  }

  @Test
  public void testWindowsLikeBackSlashes() throws ScanException {
    List<Token> tl = new TokenStream("c:\\hello\\world.%i",
            new AlmostAsIsEscapeUtil()).tokenize();

    List<Token> witness = new ArrayList<Token>();
    witness.add(new Token(Token.LITERAL, "c:\\hello\\world."));
    witness.add(Token.PERCENT_TOKEN);
    witness.add(new Token(Token.SIMPLE_KEYWORD, "i"));
    assertEquals(witness, tl);
  }

  @Test
  public void compositedKeyword() throws ScanException {
    {
      List<Token> tl = new TokenStream("%d(A)",
              new AlmostAsIsEscapeUtil()).tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.COMPOSITE_KEYWORD, "d"));
      witness.add(new Token(Token.LITERAL, "A"));
      witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
      assertEquals(witness, tl);
    }
    {
      List<Token> tl = new TokenStream("a %subst(%b C)",
              new AlmostAsIsEscapeUtil()).tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(new Token(Token.LITERAL, "a "));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.COMPOSITE_KEYWORD, "subst"));
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.SIMPLE_KEYWORD, "b"));
      witness.add(new Token(Token.LITERAL, " C"));
      witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
      assertEquals(witness, tl);
    }
  }

  @Test
  public void compositedKeywordFollowedByOptions() throws ScanException {
    {
      List<Token> tl = new TokenStream("%d(A){o}",
              new AlmostAsIsEscapeUtil()).tokenize();
      List<Token> witness = new ArrayList<Token>();
      witness.add(Token.PERCENT_TOKEN);
      witness.add(new Token(Token.COMPOSITE_KEYWORD, "d"));
      witness.add(new Token(Token.LITERAL, "A"));
      witness.add(Token.RIGHT_PARENTHESIS_TOKEN);
      List<String> ol = new ArrayList<String>();
      ol.add("o");
      witness.add(new Token(Token.OPTION, ol));

      assertEquals(witness, tl);
    }
  }
}