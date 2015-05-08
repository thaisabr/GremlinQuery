
class App {

	public static void main (String[] args){
        CommitsQuery cq = new CommitsQuery("C:\\Users\\Thaís\\Documents\\Repos para minerar\\tmp\\graph.db")
        def commits = cq.searchByComment(["bookChapter", "bookchapter", "BookChapter", "book chapter", "Book Chapter"])
        commits.each{
            println it.properties
        }
        println "Result size: ${commits.size()}"

        Printer.writeCSV(commits)
        println('Finished!')

	}
}
