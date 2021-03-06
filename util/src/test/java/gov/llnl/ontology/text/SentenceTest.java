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

package gov.llnl.ontology.text;

import gov.llnl.ontology.text.tokenize.*;
import gov.llnl.ontology.text.sentsplit.*;
import gov.llnl.ontology.text.tag.*;

import gov.llnl.ontology.util.StringPair;

import com.google.gson.Gson;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.Span;


/**
 * @author Keith Stevens
 */
public class SentenceTest {

    private static final String[][] TOKEN_INFO = {
        {"cat", "DT"},
        {"dog", null},
        {"blah", "JOB"},
        {"what", "1"}
    };

    private static final int[][] RANGES = {
        {0, 4},
        {4, 6},
        {6, 10},
        {-1, 11}
    };

    private static final String[][] TEST_PARSED_SENTENCES = {
        {"When", "WRB", "2", "advmod"},
        {"released", "VBN", "0", "null"},
        {"into", "IN", "2", "prep"}, 
        {"the", "DT", "5", "det"},
        {"air", "NN", "3", "pobj"},
        {",", ",", "7", "punct"},
        {"CFCs", "NNP", "4", "nn"},
    };

    public static Sentence makeSentence(int start, int end,
                                        String[][] tokenInfo, int[][] ranges) {
        Sentence sent = new Sentence(start, end, tokenInfo.length);
        for (int i = 0; i < tokenInfo.length; ++i) {
            Annotation tok = new SimpleAnnotation(
                    tokenInfo[i][0], tokenInfo[i][1],
                    ranges[i][0], ranges[i][1]);
            sent.addAnnotation(i, tok);
        }
        return sent;
    }

    public static Annotation annotationFromCoNLL(String[] conllString) {
        Annotation token = new SimpleAnnotation(conllString[0]);
        token.setPos(conllString[1]);
        token.setDependencyRelation(conllString[3]);
        token.setDependencyParent(Integer.parseInt(conllString[2]));
        return token;
    }

    @Test public void testConstructorAndGetters() {
        Sentence sent = new Sentence(0, 100, 8);
        assertEquals(0, sent.start());
        assertEquals(100, sent.end());
        assertEquals(8, sent.numTokens());
        assertEquals(0, sent.start());
        assertEquals(100, sent.end());
    }

    @Test public void testSentenceText() {
        Sentence sent = new Sentence(2, 10, 8);
        String text = "abcdefghijlkmop";
        sent.setText(text);
        assertEquals(text.substring(2, 10), sent.sentenceText());
    }

    @Test public void testSetAndGet() {
        Sentence sent = new Sentence(0, 100, 1);
        Annotation annot = new SimpleAnnotation("blah");
        sent.addAnnotation(0, annot);
        assertEquals(annot, sent.getAnnotation(0));
    }

    @Test public void testEmptyGet() {
        Sentence sent = new Sentence(0, 100, 1);
        assertEquals(null, sent.getAnnotation(0));
    }

    @Test (expected=IndexOutOfBoundsException.class)
    public void testNegativeSet() {
        Sentence sent = new Sentence(0, 100, 1);
        Annotation annot = new SimpleAnnotation("blah");
        sent.addAnnotation(-1, annot);
    }

    @Test (expected=IndexOutOfBoundsException.class)
    public void testTooLargeSet() {
        Sentence sent = new Sentence(0, 100, 1);
        Annotation annot = new SimpleAnnotation("blah");
        sent.addAnnotation(1, annot);
    }

    @Test (expected=IndexOutOfBoundsException.class)
    public void testNegativeGet() {
        Sentence sent = new Sentence(0, 100, 1);
        sent.getAnnotation(-1);
    }

    @Test (expected=IndexOutOfBoundsException.class)
    public void testTooLargeGet() {
        Sentence sent = new Sentence(0, 100, 1);
        sent.getAnnotation(1);
    }

    @Test public void testAnnotationIterator() {
        Sentence sentence = new Sentence(0, 100, 7);
        for (int i = 0; i < TEST_PARSED_SENTENCES.length; ++i)
            sentence.addAnnotation(
                    i, annotationFromCoNLL(TEST_PARSED_SENTENCES[i]));

        Iterator<Annotation> iter = sentence.iterator();
        for (int i = 0; i < TEST_PARSED_SENTENCES.length; ++i) {
            assertTrue(iter.hasNext());
            Annotation token = iter.next();
            assertEquals(TEST_PARSED_SENTENCES[i][0], token.word());
            assertEquals(TEST_PARSED_SENTENCES[i][1], token.pos());
            assertEquals(Integer.parseInt(TEST_PARSED_SENTENCES[i][2]),
                         token.dependencyParent());
            assertEquals(TEST_PARSED_SENTENCES[i][3],
                         token.dependencyRelation());
        }
    }

    @Test public void testDependencyParseTree() {
        Sentence sentence = new Sentence(0, 100, 7);
        for (int i = 0; i < TEST_PARSED_SENTENCES.length; ++i)
            sentence.addAnnotation(
                    i, annotationFromCoNLL(TEST_PARSED_SENTENCES[i]));
        DependencyTreeNode[] tree = sentence.dependencyParseTree();

        assertEquals(TEST_PARSED_SENTENCES.length, tree.length);
        for (int i = 0; i < TEST_PARSED_SENTENCES.length; ++i) {
            assertEquals(TEST_PARSED_SENTENCES[i][0], tree[i].word());
            assertEquals(TEST_PARSED_SENTENCES[i][1], tree[i].pos());
        }

        assertEquals(1, tree[0].neighbors().size());
        DependencyRelation relation = tree[0].neighbors().get(0);
        assertEquals("When", relation.dependentNode().word());
        assertEquals("advmod", relation.relation());
        assertEquals("released", relation.headNode().word());

        assertEquals(2, tree[1].neighbors().size());
        relation = tree[1].neighbors().get(0);
        assertEquals("When", relation.dependentNode().word());
        assertEquals("advmod", relation.relation());
        assertEquals("released", relation.headNode().word());
    }

    @Test public void testTaggedTokens() {
        Sentence sentence = makeSentence(0, 4, TOKEN_INFO, RANGES);
        StringPair[] taggedPairs = sentence.taggedTokens();
        assertEquals(TOKEN_INFO.length, taggedPairs.length);
        for (int i = 0; i < TOKEN_INFO.length; ++i) {
            assertEquals(TOKEN_INFO[i][0], taggedPairs[i].x);
            assertEquals(TOKEN_INFO[i][1], taggedPairs[i].y);
        }
    }

    @Test public void testJSon() {
        Sentence sent = makeSentence(0, 4, TOKEN_INFO, RANGES);
        Gson gson = new Gson();
        String json = gson.toJson(sent);
    }

    @Test public void testReadSentence() {
        String testSentenceText = "0 4 3|";
        String testTokenText = "0 0;word:bo&semi,b;pos:DT;dep-rel:g;dep-index:5;span:0,1|0 1;pos:NN;word:ca&pipe,t|0 2;word:&quot,";
        List<Sentence> readSentences = Sentence.readSentences(testSentenceText, testTokenText);

        assertEquals(1, readSentences.size());
        assertEquals(3, readSentences.get(0).numTokens());

        Iterator<Annotation> annots = readSentences.get(0).iterator();

        assertTrue(annots.hasNext());
        Annotation annot = annots.next();
        assertEquals("bo;b", annot.word());
        assertEquals("DT", annot.pos());
        assertEquals("g", annot.dependencyRelation());
        assertEquals(5, annot.dependencyParent());
        assertEquals(0, annot.start());
        assertEquals(1, annot.end());

        assertTrue(annots.hasNext());
        annot = annots.next();
        assertEquals("ca|t", annot.word());
        assertEquals("NN", annot.pos());

        assertTrue(annots.hasNext());
        annot = annots.next();
        assertEquals("\"", annot.word());
        assertFalse(annots.hasNext());
    }

    @Test public void splittingText() {
        String text = "FOR two weeks now, the New England Patriots have been talking about not getting their props. Frankly, I think they're feeling guilty. Everyone knows why the St. Louis Rams are here. The Rams are in the Super Bowl on the strength of a scintillating, high-octane offense and a much-improved defense. St. Louis has been the team to beat virtually from start to finish this season. The public is split on why New England is here: Fate or luck? Two weeks after an official's decision after the review of a replay rescued the Patriots from a playoff loss against the Oakland Raiders, New England players have found themselves having to justify their presence in the championship game. ''I've seen people get lucky breaks; maybe we got a lucky break,'' Bryan Cox, the Patriots' veteran linebacker, said. ''But I also feel like we earned this. With the breaks that we had, we capitalized on them. ''The Raiders thing, when you say luck about that situation, I'll say that play happened and we got the ball back. We're still down, 13-10. If they stop us, it's not an issue. Then they don't stop us in overtime. So what's the argument? Was that luck? Or did we take advantage of a play that we got?'' Everyone is entitled to a break here and there. What's become annoying about New England is that the Patriots don't want to attribute their presence in New Orleans to luck. They have chosen, through safety Lawyer Milloy, to dress luck in martyr's clothing and say they have been traduced by the news media. But what New England calls lack of respect is actually skepticism. Skepticism that can be erased only by a victory on Sunday. The Patriots may very well be last year's Giants. The Giants tripped and stumbled through part of the regular season, played well at the end and had playoff success. Just as the Giants were crushed in the Super Bowl by a pulverizing Baltimore defense, New England could be annihilated by an equally pulverizing St. Louis offense. World-class athletes will tell you that they don't deal in luck. Most embrace the philosophy that you make your luck through hard work. Isaac Bruce, the Rams' stoic, battle-tested wide receiver, said: ''Me, personally? I don't like to deal with luck, because I think luck runs out, either good luck or bad luck. I figure that hard work can override luck.'' When pressed, Bruce conceded that luck was part of the game and used New England as an example. He cited the play that was first ruled a fumble, then an incomplete pass after it was reviewed, allowing the Patriots to keep the ball. ''I mean, you can look at some perspectives and say the Patriots are lucky for getting here, that that fumble was really a fumble,'' he said. ''Some guys would say that was luck.'' Luck matters more than most athletes care to admit. You work tirelessly, running hills, working out in sweltering heat. The moment of truth comes, you execute perfectly but an official nullifies the play by invoking an obscure rule. ''Luck plays a huge role within each sport, within life,'' Marshall Faulk, the Rams' running back, said yesterday. ''There's been times that we've been lucky and things have happened for us, but for the most part we prepared for success. You've got to be at the right place at the right time in order to be lucky.'' There is a thin line between luck and an ill-gotten break, and a number of Rams players believe that New England crossed that line. Torry Holt, the Rams receiver, watched the Oakland-New England game like millions of fans, most of whom didn't care whether Oakland or New England won. This was great theater: swirling snow, both teams playing well in spots and not so well in others. The Raiders needed a yard and couldn't get it; New England failed to convert first downs. Finally, the game's denouement: Charles Woodson blitzed and hit Tom Brady with a perfectly timed tackle. Brady fumbled. Game over. There was an emotional release. New England was buried. Then, as if by some miracle, the Patriots were resurrected by an official's replay ruling. Holt reacted as a player and a fan. His reaction betrays a lingering resentment of the Patriots' road to New Orleans. ''I was hurt,'' Holt said. ''I was hurt for Oakland, because that was a great call by the defense, a great play by Woodson. They gave it all they had and they felt like it was a fumble. To get it overturned from upstairs, to have the game in somebody else's hand, is hurting. Unfortunately, there's nothing that we can do. ''Luckily, New England got a call.'' Sports of The Times";
        Tokenizer tokenizer = new OpenNlpMETokenizer();
        SentenceDetector sentenceDetector = new OpenNlpMESentenceSplitter();
        POSTagger tagger = new OpenNlpMEPOSTagger();

        for (Span sentSpan : sentenceDetector.sentPosDetect(text)) {
            // Get the indices for easy use.
            int start = sentSpan.getStart();
            int end = sentSpan.getEnd();
            String sentence = text.substring(start, end);

            // Extract the token spans found within this sentence and do
            // part of speech tagging for each word in the sentence.
            Span[] tokSpans = tokenizer.tokenizePos(sentence);
            String[] tokens = tokenizer.tokenize(sentence);
            String[] pos = tagger.tag(tokens);

            // Create the sentence level annotation.
            Sentence sentAnnotation = new Sentence(start, end, pos.length);

            // Iterate through each word and create a single annotation for
            // this object.
            for (int i = 0; i < tokSpans.length; ++i) {
                // Extract the start and end indices for easier use.
                start = tokSpans[i].getStart();
                end = tokSpans[i].getEnd();

                // Create the token annotation and add it to the sentence.
                Annotation wordAnnotation = new SimpleAnnotation(tokens[i]);
                wordAnnotation.setPos(pos[i]);
                wordAnnotation.setSpan(start, end);
                sentAnnotation.addAnnotation(i, wordAnnotation);
            }
            StringPair serialized = (Sentence.writeSentences(
                        Collections.singletonList(sentAnnotation)));

            List<Sentence> readSents = Sentence.readSentences(
                    serialized.x, serialized.y);
            assertEquals(1, readSents.size());
            Sentence read = readSents.get(0);

            int i = 0;
            for (Annotation token : read) {
                Annotation real = sentAnnotation.getAnnotation(i++);
                assertEquals(real.word(), token.word());
                assertEquals(real.pos(), token.pos());
            }

        }
    }
}
