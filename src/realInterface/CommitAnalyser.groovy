package realInterface

import search.Commit
import search.CommitManager

class CommitAnalyser {

    CommitManager manager

    public CommitAnalyser(CommitManager manager){
        this.manager = manager
    }

    RealInterface computeTaskInterfaceForCommit(String sha){
        Commit commit = manager?.searchBySha(sha)
        return new RealInterface(commits:[commit] as Set, files:commit.files.sort() as Set)
    }

    RealInterface computeTaskInterface(){
        List<Commit> commits = manager?.search()
        return new RealInterface(commits:commits as Set, files:(commits*.files.flatten().unique().sort()) as Set)
    }

    RealInterface computeTaskInterface(String taskId){
        List<Commit> commits = manager?.searchByComment([taskId])
        return new RealInterface(commits:commits as Set, files:(commits*.files.flatten().unique().sort()) as Set)
    }

    RealInterface computeTaskInterface(List words, List files){
        List<Commit> commits = manager?.search(words, files)
        return new RealInterface(commits:commits as Set, files:(commits*.files.flatten().unique().sort()) as Set)
    }

}
