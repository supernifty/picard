/*
 * The MIT License
 *
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package picard.reference;

import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import htsjdk.samtools.util.SequenceUtil;
import picard.PicardException;
import picard.cmdline.CommandLineProgram;
import picard.cmdline.CommandLineProgramProperties;
import picard.cmdline.Option;
import picard.cmdline.programgroups.Fasta;
import picard.cmdline.StandardOptionDefinitions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Simple command line program that allows sub-sequences represented by an interval
 * list to be extracted from a reference sequence file.
 *
 * @author Tim Fennell
 */
@CommandLineProgramProperties(
        usage = ExtractSequences.USAGE_SUMMARY + ExtractSequences.USAGE_DETAILS,
        usageShort = ExtractSequences.USAGE_SUMMARY,
        programGroup = Fasta.class
)
public class ExtractSequences extends CommandLineProgram {
    static final String USAGE_SUMMARY ="Extracts intervals from a reference sequence, writing them to a FASTA file.  ";
    static final String USAGE_DETAILS ="Simple command line program that allows sub-sequences represented by an interval" +
            " list to be extracted from a reference sequence file and writes them in FASTA format.  " +
            "Requires a FASTA index file to be present.<br /><br />" +
            "Interval_list files are DNA sequence files in which the sequence is grouped by arbitrary but often functional criteria.  " +
            "A common use for interval files is to separate protein coding (exons) from non-coding (introns," +
            " regulatory, etc.) regions.  <br /><br />" +
            "Subsequent to extraction, FASTA-formated data can be used by a multitude of sequence analysis tools.  For additional information, " +
            "please see: http://en.wikipedia.org/wiki/FASTA_format/ " +
            "<br />" +
            "<h4>Usage example:</h4>" +
            "<pre>" +
            "java -jar picard.jar ExtractSequences \\<br />" +
            "     -INTERVAL_LIST=myIL.interval_list \\<br />" +
            "     -R=human_b37_20.fasta \\<br />" +
            "     -O=extracted_IL_sequences.fasta" +
            "</pre>" +
            "<hr />"
            ;
    @Option(doc="Interval list describing intervals to be extracted from the reference sequence.")
    public File INTERVAL_LIST;

    @Option(shortName= StandardOptionDefinitions.REFERENCE_SHORT_NAME, doc="Reference sequence file.")
    public File REFERENCE_SEQUENCE;

    @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="Output fasta file.")
    public File OUTPUT;

    @Option(doc="Maximum line length for sequence data.")
    public int LINE_LENGTH = 80;

    public static void main(final String[] args) {
        new ExtractSequences().instanceMainWithExit(args);
    }

    @Override
    protected int doWork() {
        IOUtil.assertFileIsReadable(INTERVAL_LIST);
        IOUtil.assertFileIsReadable(REFERENCE_SEQUENCE);
        IOUtil.assertFileIsWritable(OUTPUT);

        final IntervalList intervals = IntervalList.fromFile(INTERVAL_LIST);
        final ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_SEQUENCE);
        SequenceUtil.assertSequenceDictionariesEqual(intervals.getHeader().getSequenceDictionary(), ref.getSequenceDictionary());

        final BufferedWriter out = IOUtil.openFileForBufferedWriting(OUTPUT);

        for (final Interval interval : intervals) {
            final ReferenceSequence seq = ref.getSubsequenceAt(interval.getSequence(), interval.getStart(), interval.getEnd());
            final byte[] bases = seq.getBases();
            if (interval.isNegativeStrand()) SequenceUtil.reverseComplement(bases);

            try {
                out.write(">");
                out.write(interval.getName());
                out.write("\n");

                for (int i=0; i<bases.length; ++i) {
                    if (i > 0 && i % LINE_LENGTH == 0) out.write("\n");
                    out.write(bases[i]);
                }

                out.write("\n");
            }
            catch (IOException ioe) {
                throw new PicardException("Error writing to file " + OUTPUT.getAbsolutePath(), ioe);

            }
        }

        CloserUtil.close(out);

        return 0;
    }
}
