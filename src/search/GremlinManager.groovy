package search

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin
import util.Util

class GremlinManager extends CommitManager {

    Graph graph

    public GremlinManager(){
        Gremlin.load()
        graph = new Neo4jGraph(config.path)
    }

    private static List getFilesFromCommit(def node){
        def files = []
        node.out('CHANGED').token.fill(files)
        files = files.collect{it-config.prefix}
        return Util.getChangedProductionFiles(files)
    }

    private static String getAuthorsFromCommit(def node){
        def authors = []
        node.out('AUTHOR').out('NAME').name.fill(authors)
        return authors.get(0)
    }

    public List search(){
        def commitsByComments = searchByComment()
        println "Total commits by comments: ${commitsByComments.size()}"

        def commitsByFile = searchByFiles()
        println "Total commits by files: ${commitsByFile.size()}"

        def finalResult = (commitsByComments + commitsByFile).unique{ a,b -> a.hash <=> b.hash }
        println "Total commits: ${finalResult.size()}"

        return finalResult
    }

    @Override
    public searchAllCommits(){
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []

        result.each{ r ->
            def files = getFilesFromCommit(r)
            def author = getAuthorsFromCommit(r)
            commits += new Commit(hash:r.hash, message:r.message, files:files, author:author, date:r.date)
        }

        return commits.sort{ it.date }
    }

}

