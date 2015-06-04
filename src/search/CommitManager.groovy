package search


abstract class CommitManager {

    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public abstract searchAllCommits()

    List searchByComment(){
        def commits = searchAllCommits()
        def result = commits.findAll{ commit ->
            config.keywords?.any{commit.message.contains(it)} && !commit.files.empty
        }
        return result.sort{ it.date }
    }

    List searchByFiles(){
        def commits = searchAllCommits()
        def result = commits.findAll{ commit -> !(commit.files.intersect(config.files)).isEmpty() }
        return result.unique{ a,b -> a.hash <=> b.hash }
    }

}
