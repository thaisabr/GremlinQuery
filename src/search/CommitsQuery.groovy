package search

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.gremlin.groovy.Gremlin

/**
 * Created by Thaís on 05/05/2015.
 */
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
                def files = []
                r.out('CHANGED').token.fill(files)
                files = getChangedProductionFiles(files)

                if(!files.isEmpty()){
                    def authors = []
                    r.out('AUTHOR').out('NAME').name.fill(authors)
                    commits += new Commit(hash:r.hash, message:r.message, files:files, author:authors.get(0), date:r.date)
                }
            }
        }

       return commits.sort{ it.date }
    }

    public List searchByFiles(){
        def result = []
        config.files?.each{ filename ->
            result += searchByFile(filename)
        }
        return result
    }

    public List searchByFile(String filename){
        def result = graph.V.filter{it._type == "COMMIT"}
        def commits = []

        result.each{ r ->
            def files = []
            r.out('CHANGED').token.fill(files)

            if( files.any{it.contains(filename)} ) {
                files = getChangedProductionFiles(files)

                if(!files.isEmpty()){
                    def authors = []
                    r.out('AUTHOR').out('NAME').name.fill(authors)
                    commits += new Commit(hash:r.hash, message:r.message, files:files, author:authors.get(0), date:r.date)
                }
            }
        }

        return commits.sort{ it.date }
    }

}

