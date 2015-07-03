package search

import org.eclipse.jgit.api.BlameCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import util.Util
import java.util.regex.Matcher

class JGitManager extends CommitManager {

    Repository repository
    ObjectReader reader

    public JGitManager(){
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setGitDir(new File(Util.config.gitdirectory)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    private List<DiffEntry> getDiff(String filename, RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        if(filename!=null && !filename.isEmpty()) df.setPathFilter(PathFilter.create(filename))
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        List<DiffEntry> result = []
        diffs.each{
            it.oldPath = it.oldPath.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            it.newPath = it.newPath.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            result += it
        }
        return result
    }

    private static List getAllChangedFilesFromDiffs(List<DiffEntry> diffs) {
        def files = []
        if (!diffs?.empty) {
            diffs.each{ entry ->
                if(entry.changeType==DiffEntry.ChangeType.DELETE) files += entry.oldPath
                else {
                    files += entry.newPath
                    //if(entry.changeType==DiffEntry.ChangeType.RENAME) println "<RENAME> old:${entry.oldPath}; new:${entry.newPath}"
                }
            }
        }
        return files
    }

    private RevCommit extractCommit(String sha){
        Git git = new Git(repository)
        RevCommit result1 = git.log().call().find{ it.name == sha }

        /* RevWalk is better to search commits when we need to filter the search. The problem is the code produces a
           wrong result (based on Github). Changed files from result1 are different from result2. The reason is the
           parents of such commits are different.*/
        /*RevWalk revWalk = new RevWalk(repository)
        ObjectId id = repository.resolve(sha)
        RevCommit result2 = revWalk.parseCommit(id)
        revWalk.dispose()*/

        return result1
    }

    private showDiff(DiffEntry entry){
        println "File: ${entry.newPath}; Change type: ${entry.changeType}"
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(stream)
        formatter.setRepository(repository)
        formatter.setDetectRenames(true)
        formatter.format(entry)
        println stream
    }

    private List getChangedProductionFilesFromCommit(RevCommit commit){
        def files = getAllChangedFilesFromCommit(commit)
        def productionFiles = Util.getChangedProductionFiles(files)
        return productionFiles
    }

    private List getAllChangedFilesFromCommit(RevCommit commit){
        def files = []
        if(commit.parentCount>0) {
            def diffs = getDiff(null, commit.tree, commit.parents.first().tree)
            files = getAllChangedFilesFromDiffs(diffs)
        }
        else{
            TreeWalk tw = new TreeWalk(repository)
            tw.reset()
            tw.setRecursive(true)
            tw.addTree(commit.tree)
            while(tw.next()){
                files += tw.pathString.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            }
            tw.release()
        }
        return files
    }

    @Override
    List<Commit> searchAllCommits(){
        Git git = new Git(repository)
        Iterable<RevCommit> logs = git.log().call()
        def commits = []

        logs.each{ c ->
            def files = getChangedProductionFilesFromCommit(c)
            commits += new Commit(hash:c.name, message:c.fullMessage.replaceAll(Util.NEW_LINE_REGEX," "), files:files,
                                  author:c.authorIdent.name, date:c.commitTime)
        }

        return commits.sort{ it.date }
    }

    @Override
    List<Commit> searchBySha(String... sha) {
        Git git = new Git(repository)
        def result = git.log().call().findAll{ it.name in sha }
        def commits = []
        result?.each{ c ->
            def files = getChangedProductionFilesFromCommit(c)
            commits += new Commit(hash:c.name, message:c.fullMessage.replaceAll(Util.NEW_LINE_REGEX," "), files:files,
                              author:c.authorIdent.name, date:c.commitTime)
        }
        return commits.sort{ it.date }
    }

    /********************* THOSE METHODS COULD BE USEFUL TO COMPUTE REAL INTERFACES (CHANGED METHODS)******************/
    private TreeWalk generateTreeWalk(RevTree tree, String filename){
        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        if(filename) treeWalk.setFilter(PathFilter.create(filename))
        treeWalk.next()
        return treeWalk
    }

    private List getFileContent(ObjectId commitID, String filename) {
        RevWalk revWalk = new RevWalk(repository)
        RevCommit commit = revWalk.parseCommit(commitID)
        TreeWalk treeWalk = generateTreeWalk(commit?.tree, filename)
        ObjectId objectId = treeWalk.getObjectId(0)
        try{
            ObjectLoader loader = repository.open(objectId)
            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            loader.copyTo(stream)
            revWalk.dispose()
            return stream.toString().readLines()
        }
        catch(MissingObjectException exception){
            if(objectId.equals(ObjectId.zeroId()))
                println "There is no ObjectID for the commit tree. Verify the file separator used in the filename."
            return []
        }
    }

    /* Retrieving file content */
    private List getFileContent(RevCommit commit, String filename){
        ObjectId commitID = ObjectId.fromString(commit.name)
        getFileContent(commitID, filename)
    }

    /* Printing file content by line */
    def showFileContent(RevCommit commit, String file){
        def commitLines = getFileContent(commit, file)
        println "FILE: $file; SIZE: ${commitLines.size()} lines"
        commitLines.each{ line ->
            println line
        }
    }

    /* Printing file history by line */
    List<String> showLinesHistoryOfHeadVersion(String filename){
        println "Filename: $filename"
        BlameCommand blamer = new BlameCommand(repository)
        ObjectId head = repository.resolve(Constants.HEAD)
        blamer.setStartCommit(head)
        blamer.setFilePath(filename)
        BlameResult blameResult = blamer.call()
        def shas = []

        def lines = getFileContent(head, filename)
        lines.eachWithIndex{ line, i ->
            RevCommit commit = blameResult.getSourceCommit(i)
            shas += commit
            println "Line ${i+1}(${commit.name}): $line"
        }
        println "Displayed commits responsible for ${lines.size()} lines of $filename"
        return shas.unique().sort{ it.commitTime }*.name
    }

    /* Printing changes in specific files of a commit */
    def showChanges(String sha, Collection<String> changedFiles){
        changedFiles = changedFiles*.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement("/"))
        println "Commit sha: $sha"
        RevCommit commit = extractCommit(sha)
        if(commit.parentCount>0) {
            changedFiles.each{ file ->
                List<DiffEntry> diff = getDiff(file, commit?.tree, commit.parents.first().tree)
                diff.each { showDiff(it) }
            }
        }
        else{
            TreeWalk tw = generateTreeWalk(commit.tree, null)
            while (tw.next()) {
                def path = tw.pathString
                if(path in changedFiles) showFileContent(commit, path)
            }
            tw.release()
        }
    }

    def showAllChangesFromCommit(String sha){
        println "Commit sha: $sha"
        RevCommit commit = extractCommit(sha)
        if(commit.parentCount>0) {
            def diffs = getDiff(null, commit.tree, commit.parents.first().tree)
            diffs.each{ showDiff(it) }
        }
        else{
            TreeWalk tw = generateTreeWalk(commit.tree, null)
            while(tw.next()){
                def path = tw.pathString
                showFileContent(commit, path)
            }
            tw.release()
        }
    }
    /******************************************************************************************************************/
}
