package search

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

class Printer {

    static COMMITS_FILENAME = "commits.csv"

    public static void writeCSV(List<Commit> commits){
        writeCSV(commits, COMMITS_FILENAME)
    }

    public static void writeCSV(List<Commit> commits, String filename){
        File csv = new File(filename)
        csv.withWriterAppend("UTF-16"){ out ->
            CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withDelimiter('*' as char))
            commits.each { commit ->
                csvFilePrinter.printRecord(commit.toString())
            }
        }
    }

}
