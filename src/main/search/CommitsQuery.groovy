import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin

/**
 * Created by Thaís on 05/05/2015.
 */
class CommitsQuery {

    Graph graph

    public CommitsQuery(String path){
        Gremlin.load()
        graph = new Neo4jGraph(path)
    }

    public List searchByComment(List keywords){
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []

        result.each{ r ->
            if( keywords.any{r.message.contains(it)} ) {
                def authors = []
                r.out('AUTHOR').out('NAME').name.fill(authors)
                def files = []
                r.out('CHANGED').token.fill(files)
                commits += new Commit(hash:r.hash, message:r.message, files:files, author:authors.get(0), date:r.date)
            }
        }

        commits.sort{ it.date }
    }

    public List searchByFile(List<String> filenames){
        def result = []
        filenames.each{ filename ->
            result += searchByFile(filename)
        }
        (result as Set) as List
    }

    public List searchByFile(String filename){
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []

        result.each{ r ->
            def files = []
            r.out('CHANGED').token.fill(files)

            if( files.any{it.contains(filename)} ) {
                def authors = []
                r.out('AUTHOR').out('NAME').name.fill(authors)
                commits += new Commit(hash:r.hash, message:r.message, files:files, author:authors.get(0), date:r.date)
            }
        }

        commits.sort{ it.date }
    }

}

