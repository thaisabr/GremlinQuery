package search


abstract class CommitManager {

    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public abstract List<Commit> searchAllCommits()

    List searchByComment(String... words){
        def commits = searchAllCommits()
        def result = commits.findAll{ commit ->
            words?.any{commit.message.contains(it)} && !commit.files.empty
        }
        return result.sort{ it.date }
    }

    List<Commit> searchByComment(){
        def commits = searchAllCommits()
        def result = commits.findAll{ commit ->
            config.keywords?.any{commit.message.contains(it)} && !commit.files.empty
        }
        return result.sort{ it.date }
    }

    List<Commit> searchByFiles(){
        def commits = searchAllCommits()
        def result = commits.findAll{ commit -> !(commit.files.intersect(config.files)).isEmpty() }
        return result.unique{ a,b -> a.hash <=> b.hash }
    }

    List<Commit> search(){
        def commitsByComments = searchByComment()
        println "Total commits by comments: ${commitsByComments.size()}"

        def commitsByFile = searchByFiles()
        println "Total commits by files: ${commitsByFile.size()}"

        def finalResult = (commitsByComments + commitsByFile).unique{ a,b -> a.hash <=> b.hash }
        println "Total commits: ${finalResult.size()}"

        return finalResult
    }

}
