/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.wordnet.builder;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class TermToAdd implements Comparable<TermToAdd> {

    public String term;

    public String[] parents;

    public double[] parentScores;

    public Map<String, Double> cousinScores;

    public int numParentsInWordNet;

    public int numParentsToAdd;

    private boolean compareInWn;

    public TermToAdd(String term, String[] parents, double[] parentScores,
                     Map<String, Double> cousinScores) {
        this(term, parents, parentScores, cousinScores, true);
    }

    public TermToAdd(String term, String[] parents, double[] parentScores,
                     Map<String, Double> cousinScores, boolean compareInWn) {
        this.term = term;
        this.parents = parents;
        this.parentScores = parentScores;
        this.cousinScores = cousinScores;
        this.compareInWn = compareInWn;
    }

    public void checkParentsInWordNet(OntologyReader wordnet) {
        Set<String> wordNetTerms = wordnet.wordnetTerms(PartsOfSpeech.NOUN);
        for (String parent : parents)
            if (wordNetTerms.contains(parent))
                numParentsInWordNet++;
    }

    public void checkParentsInList(Set<String> terms) {
        for (String parent : parents)
            if (terms.contains(parent))
                numParentsToAdd++;
    }

    public int compareTo(TermToAdd other) {
        if (compareInWn) {
            int inWordNetDiff =
                other.numParentsInWordNet - this.numParentsInWordNet;
            return (inWordNetDiff == 0)
                ? other.numParentsToAdd - this.numParentsToAdd
                : inWordNetDiff;
        }
        int inListDiff = this.numParentsToAdd - other.numParentsToAdd;
        return (inListDiff == 0)
            ? other.numParentsInWordNet - this.numParentsInWordNet
            : inListDiff;
    }
}
