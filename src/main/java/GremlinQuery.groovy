import org.codehaus.groovy.antlr.Main;

import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.*
import com.tinkerpop.gremlin.groovy.Gremlin
import com.tinkerpop.gremlin.*

import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory


class GremlinQuery {
	static {
		Gremlin.load()
	  }
	public List exampleMethod() {
		Graph g = TinkerGraphFactory.createTinkerGraph()
		
		//new Neo4jGraph("/Users/paolaaccioly/Desktop/graph.db") 
		def results = []
		 g.v(1).out('knows').fill(results)
		return results
	  }
	
	public static void main (String[] args){
		def g = new GremlinQuery();
		def results = g.exampleMethod();
		println(results)
	}
	
	
	
}
