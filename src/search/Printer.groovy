package search

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord

class Printer {

    static COMMITS_FILENAME = "commits.csv"

    public static void writeCSV(def commits){
        File csv = new File(COMMITS_FILENAME)
        csv.withWriterAppend("UTF-16"){ out ->
            CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withDelimiter('*' as char))
            commits.each { commit ->
                csvFilePrinter.printRecord(commit.toString())
            }
        }
    }

    public static void writeCSV(def commits, String filename){
        File csv = new File(filename)
        csv.withWriterAppend("UTF-16"){ out ->
            CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withDelimiter('*' as char))
            commits.each { commit ->
                csvFilePrinter.printRecord(commit.toString())
            }
        }
    }

}
