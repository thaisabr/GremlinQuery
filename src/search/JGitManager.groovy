package search

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import util.Util

class JGitManager extends CommitManager {

    Repository repository
    ObjectReader reader

    public JGitManager(){
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setGitDir(new File(Util.config.gitdirectory)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    private List<DiffEntry> getDiff(RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        return diffs
    }

    private static List getChangedProductionFilesFromDiffs(List<DiffEntry> diffs) {
        def productionFiles = []
        if (!diffs?.empty) {
            def rejectedFiles = diffs.findAll { entry ->
                if (entry.changeType == DiffEntry.ChangeType.DELETE) entry.newPath = entry.oldPath
                (Util.config.exclude).any { entry.newPath.contains(it) }
            }
            productionFiles = diffs - rejectedFiles
        }
        return productionFiles
    }

    List getChangedFilesFromCommit(RevCommit commit){
        def files = []
        if(commit.parentCount>0) {
            commit.parents.each{ parent ->
                def diffs = getDiff(commit.tree, parent?.tree)
                files += getChangedProductionFilesFromDiffs(diffs)*.newPath
            }
        }
        else{
            TreeWalk tw = new TreeWalk(repository)
            tw.reset()
            tw.setRecursive(true)
            tw.addTree(commit.tree)
            while(tw.next()){
                files += tw.pathString
            }
            tw.release()
            files = Util.getChangedProductionFiles(files)
        }

        return files
    }

    @Override
    List<Commit> searchAllCommits(){
        Git git = new Git(repository)
        Iterable<RevCommit> logs = git.log().call()
        def commits = []

        logs.each{ c ->
            def files = getChangedFilesFromCommit(c)
            commits += new Commit(hash:c.name, message:c.fullMessage, files:files, author:c.authorIdent.name, date:c.commitTime)
        }

        return commits.sort{ it.date }
    }

}
