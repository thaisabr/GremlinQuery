import java.util.ArrayList;


class Printer {
	
	public void writeCSV(ArrayList<MergeCommit> listMC){
		
		def out = new File('commits.csv')
		listMC.each {
			def row = [it.sha, it.parent1, it.parent2]
			out.append row.join(',')
			out.append '\n'
		}
		
	}

}
