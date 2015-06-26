package search

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin
import util.Util

import java.util.regex.Matcher

class GremlinManager extends CommitManager {

    Graph graph

    public GremlinManager(){
        Gremlin.load()
        graph = new Neo4jGraph(Util.config.graphDB.path)
    }

    private static List getFilesFromCommit(def node){
        def files = []
        node.out('CHANGED').token.fill(files)
        files = files.collect { (it-Util.config.prefix).replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator)) }
        return Util.getChangedProductionFiles(files)
    }

    private static String getAuthorsFromCommit(def node){
        def authors = []
        node.out('AUTHOR').out('NAME').name.fill(authors)
        return authors.get(0)
    }

    @Override
    List<Commit> searchAllCommits(){
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []
        result?.each{ r ->
            def files = getFilesFromCommit(r)
            def author = getAuthorsFromCommit(r)
            commits += new Commit(hash:r.hash, message:r.message.replaceAll("\r\n|\n"," "), files:files, author:author, date:r.date)
        }
        return commits.sort{ it.date }
    }

    @Override
    List<Commit> searchBySha(String... sha) {
        def result = graph.V.filter{ (it._type == "COMMIT") && (it.hash in sha) }
        def commits = []
        result?.each{ c ->
            def files = getFilesFromCommit(c)
            def author = getAuthorsFromCommit(c)
            commits += new Commit(hash:c.hash, message:c.message.replaceAll("\r\n|\n"," "), files:files, author:author, date:c.date)
        }
        return commits.sort{ it.date }
    }

}

