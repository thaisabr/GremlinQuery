package search

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin

class CommitsQuery {

    Graph graph
    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public CommitsQuery(){
        Gremlin.load()
        graph = new Neo4jGraph(config.path)
    }

    private static List getChangedProductionFiles(List files){
        if(!files || files.empty) return []
        def rejectedFiles = files.findAll{ file ->
            (config.exclude).any{ file.contains(it) }
        }
        files -= rejectedFiles
        return files
    }

    private static List getFilesFromCommit(def node){
        def files = []
        node.out('CHANGED').token.fill(files)
        files = files.collect{it-config.prefix}
        return files
    }

    private static List filterProductionFilesFromCommit(List<Commit> commits){
        commits.each{ commit ->
            commit.files = getChangedProductionFiles(commit.files)
        }
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

    public List searchByComment(){
        def commits = searchAllCommits()
        filterProductionFilesFromCommit(commits)
        def result = commits.findAll{ commit ->
            config.keywords?.any{commit.message.contains(it)} && !commit.files.empty
        }
       return result.sort{ it.date }
    }

    List searchByFiles(){
        def commits = searchAllCommits()
        filterProductionFilesFromCommit(commits)
        def result = commits.findAll{ commit -> !(commit.files.intersect(config.files)).isEmpty() }
        return result.unique{ a,b -> a.hash <=> b.hash }
    }

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

