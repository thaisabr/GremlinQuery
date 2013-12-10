import org.codehaus.groovy.antlr.Main;
import com.tinkerpop.blueprints.impls.tg.*
import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin
import com.tinkerpop.gremlin.groovy.GremlinGroovyPipeline
import com.tinkerpop.gremlin.pipes.transform.PropertyMapPipe;
import com.tinkerpop.gremlin.*
import com.tinkerpop.pipes.util.StartPipe;

import org.neo4j.management.impl.*


class GremlinQuery {
	
	ArrayList<MergeCommit> mergeCommitsList = new ArrayList<MergeCommit>()
	Graph graph
	
	
	public GremlinQuery(String path){
		
		Gremlin.load()
		this.setAllMergeCommits(path)
		
		//TO DO
		//Printer p = new Printer()
		//p.writeCSV(this.mergeCommitslist)
		
	}
	
	public ArrayList<MergeCommit> getMergeCommitsList(){
		
		return this.mergeCommitsList
	}
	
	public void setAllMergeCommits(String path){
		
		this.setGraph(path)
		
		def shas = this.getShas()
		
		for(sha in shas){
			
			MergeCommit mc = new MergeCommit()
			mc.sha = sha
			
			/*String[] parents = this.getParents(sha)
			mc.parent1 = parents[0]
			mc.parent2 = parents[1]
			
			mc.parentsCommonAncestor = this.getParentsCommonAncestor()*/
			
			this.setMergeCommit(mc)
			
			
		}
		
		
		
	}
	
	public void setGraph(String path){
		
		this.graph = new Neo4jGraph(path)
	}
	
	public void setMergeCommit(MergeCommit mc){
		
		
		this.mergeCommitsList.add(mc)
	}
	
	
	public ArrayList<String> getShas() {
		
		def queryCommits = this.graph.V.map.filter{it._type == "COMMIT" & it.isMerge == true}.sort{it.date}
		ArrayList<String> results = new ArrayList<String>()
		for(commit in queryCommits){
			
			results.add(this.auxGetSha(commit.toString()))
			
		}
		
		return results
	  }
	
	private String auxGetSha(String commit){
		
		String delims = "[,]"
		String[] tokens = commit.split(delims);
		String auxSha = tokens[2]
		String sha = auxSha.substring(6)
		
		return sha
	
	}
	public void getParents(){
		
		
		
	}
	
	public void getParentsCommonAncestor(){
		
	}
	
	
	
	public static void main (String[] args){
		//def g = new GremlinQuery();
		
		/*Gremlin.load()
		
		Graph g = new Neo4jGraph("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		def results = g.V.map.filter{it._type == "COMMIT" & it.isMerge == true}.sort{it.date}
		
		String delims = "[,]"
		
			for ( m in results) {
				
				String commit = m.toString()
				String[] tokens = commit.split(delims);
				String auxSha = tokens[2]
				String sha = auxSha.substring(6)
				println(sha)
				
			}*/
		
		GremlinQuery gq = new GremlinQuery("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		for(mergeCommit in gq.getMergeCommitsList() ){
			
			println(mergeCommit.sha)
		}	
			
		
	}
	
	
	
}
