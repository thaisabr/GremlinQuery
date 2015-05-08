package search

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

class Printer {

    static filename = "commits.csv"

	public static void writeCSV(List listMC){
        File csv = new File(filename)
        csv.withWriterAppend{ out ->
            CSVFormat csvFileFormat = CSVFormat.TDF.withHeader("Hash", "Message", "Files", "Author", "Date")
            CSVPrinter csvFilePrinter = new CSVPrinter(out, csvFileFormat)
            csvFilePrinter.printRecord(listMC)
        }
    }

}
