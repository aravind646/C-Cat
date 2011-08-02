/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.DocumentReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SenseEval2007DocumentReaderTest {

    public static final String TEST_SENT =
        "   <instance id=\"explain.v.4\" corpus=\"wsj\">\n" +
        "OPEC Secretary-General Subroto <head> explains </head> " +
        ": Consumers offer security of markets " +
        "</instance>";

    @Test public void testReadDocument() {
        DocumentReader reader = new SenseEval2007DocumentReader();
        Document doc = reader.readDocument(TEST_SENT);
        assertEquals("explain.v.4", doc.key());
        assertEquals("explain", doc.title());
        assertEquals(3, doc.id());
        assertEquals(TEST_SENT, doc.originalText());
        assertEquals("senseeval2007", doc.sourceCorpus());
        assertTrue(doc.rawText().contains("explain "));
        assertFalse(doc.rawText().contains("head"));
    }
}

