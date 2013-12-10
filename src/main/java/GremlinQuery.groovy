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
			
			String[] parents = this.getParentsSha(sha)
			mc.parent1 = parents.getAt(0)
			mc.parent2 = parents.getAt(1)
			
			//mc.parentsCommonAncestor = this.getParentsCommonAncestor()*/
			
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
	
	public ArrayList<String> getParentsSha(String shaSon){
		
		def commit = this.graph.idx("commit-idx").get("hash", shaSon).first()
		def parentsTemp = commit.outE('COMMIT_PARENT').sort{it.date}
		ArrayList<String> parentsSha = new ArrayList<String>()
		
		for(parent in parentsTemp){
			
			
			String id = this.auxGetParentsID(parent.toString())
			
			def parentCommit = this.graph.v(id).map
			
			for(p in parentCommit){
				
				parentsSha.add(this.auxGetSha(p.toString()))
			}
			
			
		}
		
		return parentsSha
	}
	

	private String auxGetParentsID(String parent){
		
		String delims = "[>]"
		String[] tokens = parent.toString().split(delims)
		String idTemp = tokens[1]
		
		String id = idTemp.substring(0, (idTemp.size() - 1))
		
		return id
		
	}
	public void getParentsCommonAncestor(){
		
	}
	
	
	
	public static void main (String[] args){
		
		//testing
		
		GremlinQuery gq = new GremlinQuery("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		
		
		
		for(mergeCommit in gq.getMergeCommitsList() ){
			
			println("Commit: " + mergeCommit.sha)
			println("Parent 1: " + mergeCommit.parent1)
			println("Parent 2: " + mergeCommit.parent2)
		
			}
		
		/*Gremlin.load()
		Graph graph = new Neo4jGraph("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		def commit = graph.idx("commit-idx").get("hash", "d9c87e1cc9b192837942ec658910cb7465b526dd").first()
		def parentsTemp = commit.outE('COMMIT_PARENT').sort{it.date}
		
		String delims = "[>]"
		
		for(parent in parentsTemp){
			String[] tokens = parent.toString().split(delims)
			String idTemp = tokens[1]
			String id = idTemp.substring(0, (idTemp.size() - 1))
			def parentCommit = graph.v(id).map
			
			String p = parentCommit.getAt(1).toString()
			
			println(p)
			for(p in parentCommit){
				
				println(p.toString())
			}
			
			//println(id)
		}*/
		
		
			
		
	}
	
	
	
}
