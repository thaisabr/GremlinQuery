
class App {

	def static run(){
		//testing

		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')

		projects.each {
			GremlinQuery gq = new GremlinQuery(it.graph)

			Printer p = new Printer()
			p.writeCSV(gq.getMergeCommitsList())
			println('Printer Finished!')
			println("----------------------")

			it.setMergeCommits(gq.getMergeCommitsList())

			Extractor e = new Extractor(it)
			e.extractCommits()
			println('Extractor Finished!\n')
		}
	}

	def static runWithCommitCsv(){
		// running directly with the commit list in a CSV file

		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')

		projects.each {
			r.setCsvCommitsFile("commits.csv")
			r.readCommitsCSV()
			def ls = r.getMergeCommitsList()

			it.setMergeCommits(ls)

			Extractor e = new Extractor(it)
			e.extractCommits()
			println('Extractor Finished!\n')
		}
	}
	
	

	public static void main (String[] args){
		runWithCommitCsv()
	}
}
