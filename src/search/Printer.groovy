package search

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

class Printer {

    static filename = "commits.csv"

	public static void writeCSV(List commits){
        File csv = new File(filename)
        csv.withWriterAppend{ out ->
            CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withDelimiter('*' as char))
            csvFilePrinter.printRecord("Hash","Message","Files","Author","Date")
            commits.each { commit ->
                csvFilePrinter.printRecord(commit)
            }
        }
    }

}
