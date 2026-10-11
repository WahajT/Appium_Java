package io.github.wahajt.mobile.framework.ui;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class GesturesTest {

  @Test
  public void escapesTextForUiSelector() {
    assertEquals(Gestures.escapeUiSelector("Say \"hi\" \\o/"), "Say \\\"hi\\\" \\\\o/");
  }

  @Test
  public void escapesTextForNsPredicate() {
    assertEquals(Gestures.escapePredicate("It's"), "It\\'s");
  }
}
