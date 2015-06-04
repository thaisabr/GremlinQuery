package search

class App {

	public static void main (String[] args){
        GremlinManager cq = new GremlinManager()

        //search by comment and files
        def result = cq.search()
        Printer.writeCSV(result)

        //search only by comment
        /*def result = cq.searchByComment()
        Printer.writeCSV(result)*/

        //search only by files
        /*def result = cq.searchByFiles()
        Printer.writeCSV(result)*/

        println('Finished!')
	}
}
