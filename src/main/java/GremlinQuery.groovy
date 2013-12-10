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
	
	ArrayList<Mergecommit> mergeCommitslist = new ArrayList<Mergecommit>()
	
	static {
		Gremlin.load()
	  }
	
	public void getMergeCommits() {
		
		def g = new Neo4jGraph("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		
		def queryCommits = g.V.map.filter{it._type == "COMMIT" & it.isMerge == true}.sort{it.date}

		for(commits in queryCommits){
			
			//queryCommits.getProperties().
			//mergeCommitslist.add()
			
		}
		
	  }
	
	public void getParents(){
		
	}
	
	public void getParentsCommonAncestor(){
		
	}
	
	public static void main (String[] args){
		//def g = new GremlinQuery();
		
		Gremlin.load()
		
		def g = new Neo4jGraph("/Users/paolaaccioly/Documents/Doutorado/gitminer/graph.db")
		def results = g.V.map.filter{it._type == "COMMIT" & it.isMerge == true}.sort{it.date}
		
			/*for ( HashMap<StartPipe, PropertyMapPipe> m in results) {
				
		def sha = m.keySet().getAt("hash")
				println(sha)
			}*/
			
			
		
	}
	
	
	
}
