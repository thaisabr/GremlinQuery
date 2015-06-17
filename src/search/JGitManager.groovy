package search

import org.eclipse.jgit.api.BlameCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
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

    private List<DiffEntry> getDiff(RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        List<DiffEntry> result = []
        diffs.each{
            it.oldPath = it.oldPath.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            it.newPath = it.newPath.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            result += it
        }
        return result
    }

    private static List getChangedProductionFilesFromDiffs(List<DiffEntry> diffs) {
        def productionFiles = []
        if (!diffs?.empty) {
            def rejectedFiles = diffs.findAll { entry ->
                if (entry.changeType == DiffEntry.ChangeType.DELETE) entry.newPath = entry.oldPath
                (Util.config.search.exclude).any { entry.newPath.contains(it) }
            }
            productionFiles = diffs - rejectedFiles
        }
        return productionFiles
    }

    private RevCommit extractCommit(String sha){
        RevWalk walk = new RevWalk(repository)
        ObjectId id = repository.resolve(sha)
        walk.parseCommit(id)
    }

    private CanonicalTreeParser getCanonicalTreeParser(RevCommit commit){
        CanonicalTreeParser tree = new CanonicalTreeParser()
        tree.reset(reader, commit.tree)
        return tree
    }

    private List<DiffEntry> getDiffFromFile(String filename, CanonicalTreeParser newTree, CanonicalTreeParser oldTree){
        List<DiffEntry> diff = new Git(repository).diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .setPathFilter(PathFilter.create(filename))
                .call()
        return diff
    }

    private showChangesFromCommit(RevCommit commit, List files){
        RevCommit parent = extractCommit(commit.parents[0].name)
        CanonicalTreeParser newTreeParser = getCanonicalTreeParser(commit)
        CanonicalTreeParser oldTreeParser = getCanonicalTreeParser(parent)

        files.each{ file ->
            List<DiffEntry> diff = getDiffFromFile(file.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement("/")), newTreeParser, oldTreeParser)
            diff.each { showDiff(it) }
        }
    }

    private showDiff(DiffEntry entry){
        println "File: ${entry.newPath}; Change type: ${entry.changeType}"
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(stream)
        formatter.setRepository(repository)
        formatter.format(entry)
        println stream
    }

    private List getChangedFilesFromCommit(RevCommit commit){
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
                files += tw.pathString.replaceAll(Util.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
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

    @Override
    Commit searchBySha(String sha) {
        Git git = new Git(repository)
        def c = git.log().call().find{it.name == sha}
        if(c != null){
            def files = getChangedFilesFromCommit(c)
            showChangesFromCommit(c, files)
            return new Commit(hash:c.name, message:c.fullMessage, files:files, author:c.authorIdent.name, date:c.commitTime)
        }
        else return null
    }

    /********************* THOSE METHODS COULD BE USEFUL TO COMPUTE REAL INTERFACES (CHANGED METHODS)*******************
    private TreeWalk generateTreeWalk(RevTree tree, String filename){
        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(filename))
        treeWalk.next()
        return treeWalk
    }

    private List getFileLinesContent(ObjectId commitID, String filename) {
        RevWalk revWalk = new RevWalk(repository)
        RevCommit commit = revWalk.parseCommit(commitID)
        TreeWalk treeWalk = generateTreeWalk(commit.tree, filename)
        ObjectId objectId = treeWalk.getObjectId(0)
        ObjectLoader loader = repository.open(objectId)
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        revWalk.dispose()
        return stream.toString().readLines()
    }

    private List getFileLinesContent(RevCommit commit, String filename){
        TreeWalk treeWalk = generateTreeWalk(commit.tree, filename)
        ObjectId objectId = treeWalk.getObjectId(0)
        ObjectLoader loader = repository.open(objectId)
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        return stream.toString().readLines()
    }

    //show file history by line
    def showChangedLines(String filename){
        BlameCommand blamer = new BlameCommand(repository)
        ObjectId head = repository.resolve(Constants.HEAD)
        blamer.setStartCommit(head)
        blamer.setFilePath(filename)
        BlameResult blame = blamer.call()

        def lines = getFileLinesContent(head, filename)
        lines.eachWithIndex{ line, i ->
            RevCommit commit = blame.getSourceCommit(i)
            println "Line $i(${commit.name}): $line"
        }
        println "Displayed commits responsible for ${lines.size()} lines of $filename"
    }

    def showFileLinesContent(RevCommit commit, String file){
        def commitLines = getFileLinesContent(commit, file)
        println "LINES: ${commitLines.size()}"
        commitLines.each{ line ->
            println line
        }
    }

    def showChanges(String sha, List<String> changedFiles){
        RevCommit commit = extractCommit(sha)
        RevCommit parent = extractCommit(commit.parents[0].name)
        CanonicalTreeParser newTreeParser = getCanonicalTreeParser(commit)
        CanonicalTreeParser oldTreeParser = getCanonicalTreeParser(parent)

        changedFiles.each{ file ->
            List<DiffEntry> diff = getDiffFromFile(file, newTreeParser, oldTreeParser)
            diff.each { showDiff(it) }

            println "<NEW VERSION>"
            showFileLinesContent(commit, file)

            println "<PARENT VERSION>"
            showFileLinesContent(parent, file)
        }
    }

    List showAllChangesFromCommit(String sha){
        RevCommit commit = extractCommit(sha)
        RevCommit parent = extractCommit(commit.parents[0].name)
        List<DiffEntry> diffs = getDiff(commit.tree, parent.tree)
        diffs.each{ showDiff(it) }
        return diffs
    }*/

}
