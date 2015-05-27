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

    private static List getProductionFilesFromCommit(def node){
        def files = []
        node.out('CHANGED').token.fill(files)
        files = files.collect{it-config.prefix}
        return getChangedProductionFiles(files)
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
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []

        result.each{ r ->
            if( config.keywords?.any{r.message.contains(it)} ) {
                def files = getProductionFilesFromCommit(r)
                if(!files.isEmpty()){
                    def author = getAuthorsFromCommit(r)
                    commits += new Commit(hash:r.hash, message:r.message, files:files, author:author, date:r.date)
                }
            }
        }

       return commits.sort{ it.date }
    }

    List searchByFiles(){
        List<Commit> commits = searchAllCommits()
        def result = commits.findAll{ commit -> !(commit.files.intersect(config.files)).isEmpty() }
        result = result.findAll{ !getChangedProductionFiles(it.files).empty }
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

