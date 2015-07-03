package search

import util.Util

abstract class CommitManager {

    public abstract List<Commit> searchAllCommits()

    public abstract List<Commit> searchBySha(String... sha)

    List<Commit> searchByComment(List<String> words){
        def commits = searchAllCommits()
        println "Total commits: ${commits.size()}"

        def result = commits.findAll{ commit ->
            words?.any{commit.message.toLowerCase().contains(it)} && !commit.files.empty
        }
        def finalResult = result.unique{ a,b -> a.hash <=> b.hash }
        println "Total commits by comment: ${finalResult.size()}"

        return finalResult.sort{ it.date }
    }

    List<Commit> searchByComment(){
        searchByComment(Util.config.search.keywords)
    }

    List<Commit> searchByFiles(List<String> files){
        def commits = searchAllCommits()
        println "Total commits: ${commits.size()}"

        def result = commits.findAll{ commit -> !(commit.files.intersect(files)).isEmpty() }
        def finalResult = result.unique{ a,b -> a.hash <=> b.hash }
        println "Total commits by file: ${finalResult.size()}"

        return finalResult.sort{ it.date }
    }

    List<Commit> searchByFiles(){
        searchByFiles(Util.config.search.files)
    }

    List<Commit> search(){
        search(Util.config.search.keywords, Util.config.search.files)
    }

    List<Commit> search(List<String> words, List<String> files){
        def commits = searchAllCommits()
        println "Total commits: ${commits.size()}"

        def result = commits.findAll{ commit ->
            (words?.any{commit.message.toLowerCase().contains(it)} && !commit.files.empty) ||
            !(commit.files.intersect(files)).isEmpty()
        }
        def finalResult = result.unique{ a,b -> a.hash <=> b.hash }
        println "Total commits by comments and files: ${finalResult.size()}"

        return finalResult.sort{ it.date }
    }

}
