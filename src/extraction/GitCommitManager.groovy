package extraction

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
import search.Commit

class GitCommitManager {

    Repository repository
    ObjectReader reader
    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public GitCommitManager(){
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(config.gitdirectory)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    private CanonicalTreeParser getCanonicalTreeParser(RevCommit commit){
        CanonicalTreeParser tree = new CanonicalTreeParser()
        tree.reset(reader, commit.tree)
        return tree
    }

    private List<DiffEntry> getDiff(RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        return diffs
    }

    private List<DiffEntry> getDiffFromFile(String filename, CanonicalTreeParser newTree, CanonicalTreeParser oldTree){
        List<DiffEntry> diff = new Git(repository).diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .setPathFilter(PathFilter.create(filename))
                .call()
        return diff
    }

    private void showDiff(DiffEntry entry){
        if( !(entry.changeType in [DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.MODIFY])) return
        println "File: ${entry.newPath}; Change type: ${entry.changeType}"
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(stream)
        formatter.setRepository(repository)
        formatter.format(entry)
        println stream
    }

    private generateTreeWalk(RevTree tree, String filename){
        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(filename))
        treeWalk.next()
        return treeWalk
    }

    private getFileLinesContent(ObjectId commitID, String filename) {
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

    private getFileLinesContent(RevCommit commit, String filename){
        TreeWalk treeWalk = generateTreeWalk(commit.tree, filename)
        ObjectId objectId = treeWalk.getObjectId(0)
        ObjectLoader loader = repository.open(objectId)
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        return stream.toString().readLines()
    }

    private RevCommit extractCommit(String sha){
        RevWalk walk = new RevWalk(repository)
        ObjectId id = repository.resolve(sha)
        walk.parseCommit(id)
    }

    def showCommitsHistory(){
        Git git = new Git(repository)
        Iterable<RevCommit> logs = git.log().call()
        int count = 0
        logs.each { rev ->
            println "Commit: $rev, name: ${rev.getName()}"
            count++
        }
        println "Had $count commits overall on current branch"
    }

    List showAllChangesFromCommit(String sha){
        RevCommit commit = extractCommit(sha)
        RevCommit parent = extractCommit(commit.parents[0].name) //se for merge, vai ter mais de um pai?
        List<DiffEntry> diffs = getDiff(commit.tree, parent.tree)
        diffs.each{ showDiff(it) }
        return diffs
    }

    def showChanges(String sha, List changedFiles){
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

    def showFileLinesContent(RevCommit commit, String file){
        def commitLines = getFileLinesContent(commit, file)
        println "LINES: ${commitLines.size()}"
        commitLines.each{ line ->
            println line
        }
    }

    //show file history by line
    def showChangedLines(String filename){
        BlameCommand blamer = new BlameCommand(repository)
        ObjectId commitID = repository.resolve("HEAD")
        blamer.setStartCommit(commitID)
        blamer.setFilePath(filename)
        BlameResult blame = blamer.call()

        def lines = getFileLinesContent(commitID, filename)
        lines.eachWithIndex{ line, i ->
            RevCommit commit = blame.getSourceCommit(i)
            println "Line $i(${commit.name}): $line"
        }
        println "Displayed commits responsible for ${lines.size()} lines of $filename"
    }

    List searchByComment(){
        Git git = new Git(repository)
        Iterable<RevCommit> logs = git.log().call()
        def commits = []

        logs.each{ c ->
            if( config.keywords?.any{c.fullMessage.contains(it)}){
                def files = getChangedFilesFromCommit(c)
                if(!files.empty) {
                    commits += new Commit(hash: c.name, message: c.fullMessage, files: files, author: c.authorIdent.name, date: c.commitTime)
                }
            }
        }

        return commits.sort{ it.date }
    }

    List searchByFiles(){
        List<Commit> commits = searchAllCommits()
        def result = commits.findAll{ commit -> !(commit.files.intersect(config.files)).isEmpty() }
        return result.unique{ a,b -> a.hash <=> b.hash }
    }

    private static List getChangedProductionFiles(List<DiffEntry> diffs){
        if(!diffs || diffs.empty) return []
        def rejectedFiles = diffs.findAll{ entry ->
            entry.newPath.equals(DiffEntry.DEV_NULL) || (config.exclude).any{ entry.newPath.contains(it) }
        }
        diffs -= rejectedFiles
        return diffs
    }

    List getChangedFilesFromCommit(RevCommit commit){
        RevCommit parent

        if(commit.parentCount>0) {
            parent = extractCommit(commit.parents[0].name)

        }
        else{
            ObjectId head = repository.resolve(Constants.HEAD)
            RevWalk revWalk = new RevWalk(repository)
            parent = revWalk.parseCommit(head)
            revWalk.dispose()
        }

        List<DiffEntry> diffs = getDiff(commit.tree, parent.tree)
        return getChangedProductionFiles(diffs)*.newPath
    }

    List search(){
        def commitsByComments = searchByComment()
        println "Total commits by comments: ${commitsByComments.size()}"

        def commitsByFile = searchByFiles()
        println "Total commits by files: ${commitsByFile.size()}"

        def finalResult = (commitsByComments + commitsByFile).unique{ a,b -> a.hash <=> b.hash }
        println "Total commits: ${finalResult.size()}"

        return finalResult
    }

    List searchAllCommits(){
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
