
class App {

	public static void main (String[] args){
		//testing
		GremlinQuery gq = new GremlinQuery("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
	
		Printer p = new Printer()
		p.writeCSV(gq.getMergeCommitsList())
		println('Finished!')
		
		
		
	}
}
