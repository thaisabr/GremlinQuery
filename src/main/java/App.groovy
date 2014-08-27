


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

			/*it.setMergeCommits(gq.getMergeCommitsList())

			Extractor e = new Extractor(it)
			e.extractCommits()
			println('Extractor Finished!\n')*/
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
	
	def static choose25MergeScenarios(){
		
		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')
		
		projects.each {
			GremlinQuery gq = new GremlinQuery(it.graph)
			ArrayList<MergeCommit> allMergeScenarios = gq.getMergeCommitsList()
			double temp = allMergeScenarios.size/5
			int partitionsSize = temp.round()
			ArrayList<ArrayList<MergeCommit>> partitionsTemp = allMergeScenarios.collate(partitionsSize)
			ArrayList<MergeCommit> partitions = new ArrayList<MergeCommit>();
			
			(0..4).each{
				
				partitions.add(partitionsTemp.get(it))
			}
			
			
			
			ArrayList<MergeCommit> chosenMergeScenarios = new ArrayList<MergeCommit>();
			
			
			
			for(ArrayList<MergeCommit> partition : partitions){
				
				def random = new Random()
				
				(0..4).each {
					
					def i = random.nextInt(partition.size())
					chosenMergeScenarios.add(partition.get(i))
					
					
				}
				
			}
			
			Printer p = new Printer()
			p.writeCSV(chosenMergeScenarios)
			println('Printer Finished!')
			println("----------------------")
			
		}
		
	}

	public static void main (String[] args){
		choose25MergeScenarios()
	}
}
