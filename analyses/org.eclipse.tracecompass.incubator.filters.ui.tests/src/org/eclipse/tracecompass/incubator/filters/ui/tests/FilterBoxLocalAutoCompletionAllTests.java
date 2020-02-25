/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.filters.ui.tests;


import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.incubator.internal.filters.ui.lspFilterTextbox.FilterBoxLocalTextCompletion;
import org.junit.jupiter.api.Test;

/**
 *
 *This tests the {@link FilterBoxLocalTextCompletion}
 *
 * @author Maxime Thibault
 *
 */
@SuppressWarnings("restriction")
public class FilterBoxLocalAutoCompletionAllTests {

    /**
     * Tests quote completion
     */
   @Test
   public void completeQuoteTest() {

       //Double quote
       assertEquals("\"\"", FilterBoxLocalTextCompletion.autocomplete("\"", 1));

       assertEquals("t\"\"", FilterBoxLocalTextCompletion.autocomplete("t\"", 2));

       assertEquals("\"t", FilterBoxLocalTextCompletion.autocomplete("\"t", 1));

       assertEquals("\"\" ", FilterBoxLocalTextCompletion.autocomplete("\" ", 1));


       //Single quote
       assertEquals("''", FilterBoxLocalTextCompletion.autocomplete("'", 1));

       assertEquals("t''", FilterBoxLocalTextCompletion.autocomplete("t'", 2));

       assertEquals("'t", FilterBoxLocalTextCompletion.autocomplete("'t", 1));

       assertEquals("'' ", FilterBoxLocalTextCompletion.autocomplete("' ", 1));

   }

   /**
    * Test brackets completions
    */
   @Test
   public void completeBracket() {

       //Squared brackets
       assertEquals("[]", FilterBoxLocalTextCompletion.autocomplete("[", 1));

       assertEquals("t[]", FilterBoxLocalTextCompletion.autocomplete("t[", 2));

       assertEquals("[t", FilterBoxLocalTextCompletion.autocomplete("[t", 1));

       assertEquals("[] ", FilterBoxLocalTextCompletion.autocomplete("[ ", 1));

       assertEquals("[[]]", FilterBoxLocalTextCompletion.autocomplete("[[]", 2));

       //Normal brackets
       assertEquals("{}", FilterBoxLocalTextCompletion.autocomplete("{", 1));

       assertEquals("t{}", FilterBoxLocalTextCompletion.autocomplete("t{", 2));

       assertEquals("{t", FilterBoxLocalTextCompletion.autocomplete("{t", 1));

       assertEquals("{} ", FilterBoxLocalTextCompletion.autocomplete("{ ", 1));

       assertEquals("{{}}", FilterBoxLocalTextCompletion.autocomplete("{{}", 2));

   }

   /**
    * Tests parenthesis completion
    */
   @Test
   public void completeParenthesis() {

       assertEquals("{}", FilterBoxLocalTextCompletion.autocomplete("{", 1));

       assertEquals("t{}", FilterBoxLocalTextCompletion.autocomplete("t{", 2));

       assertEquals("{t", FilterBoxLocalTextCompletion.autocomplete("{t", 1));

       assertEquals("{} ", FilterBoxLocalTextCompletion.autocomplete("{ ", 1));

       assertEquals("{{}}", FilterBoxLocalTextCompletion.autocomplete("{{}", 2));

}
}
