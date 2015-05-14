package search

class App {

	public static void main (String[] args){
        def config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())
        CommitsQuery cq = new CommitsQuery(config.bd.path)

        def commitsByComments = cq.searchByComment(config.mining.keywords)
        println "Result size: ${commitsByComments.size()}"

        def commitsByFile = cq.searchByFile(config.mining.files)
        println "Result by files size: ${commitsByFile.size()}"

        def finalResult = (commitsByComments + commitsByFile) as Set
        println "Final result size: ${commitsByFile.size()}"
        Printer.writeCSV(finalResult as List)
        println('Finished!')

	}
}
