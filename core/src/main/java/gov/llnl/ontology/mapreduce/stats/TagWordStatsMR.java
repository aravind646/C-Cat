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

package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.MRArgOptions;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Maps;


import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Map;


/**
 * A Map/Reduce job that counts the co-occurrence statistics between a word and
 * a documen tag.
 *
 * @author Keith Stevens
 */
public class TagWordStatsMR extends CorpusTableMR {

    /**
     * The job description used in the help text.
     */
    public static final String ABOUT =
        "Computes the co-occurrence frequency between document tags and " +
        "words in the documents for a particular corpus.  If no corpus " +
        "is specified, all documents will be used.  The co-occurrence " +
        "counts will be stored in reduce parts on hdfs under the " +
        "specified <outdir";

    /**
     * The main.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new TagWordStatsMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", TagWordStatsMR.class, 1, 'C');
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "Tag Word Stats";
    }

    /**
     * {@inheritDoc}
     */
    public Class mapperClass() {
        return TagWordStatsMapper.class;
    }

    /**
     * {@inheritDoc}
     */
    public Class mapperKeyClass() {
        return StringPair.class;
    }

    /**
     * {@inheritDoc}
     */
    public Class mapperValueClass() {
        return IntWritable.class;
    }

    /**
     * Sets up the Reducer for this job.  
     */
    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) {
        job.setCombinerClass(WordCountSumReducer.class);
        job.setReducerClass(WordCountSumReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(24);
    }

    /**
     * The {@link TableMapper} responsible for the real work.
     */
    public static class TagWordStatsMapper
            extends CorpusTableMR.CorpusTableMapper<StringPair, IntWritable> {

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            context.setStatus("Processing Documents");

            StringCounter counter = new StringCounter();
            for (Sentence sentence : table.sentences(row))
                for (StringPair word : sentence.taggedTokens())
                    counter.count(word.x);

            Map<String, StringCounter> tagWordCounters = Maps.newHashMap();
            for (String category : table.getCategories(row))
                tagWordCounters.put(category, counter);

            WordCountSumReducer.emitCounts(tagWordCounters, context);
            context.getCounter("TagWordStatsMR", "Documents").increment(1);
        }
    }
}
