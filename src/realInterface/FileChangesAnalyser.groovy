package realInterface

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
import search.CommitManager

class FileChangesAnalyser {

    Repository repository
    ObjectReader reader
    CommitManager manager
    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public FileChangesAnalyser(CommitManager manager){
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setGitDir(new File(config.gitdirectory)).setMustExist(true).build()
        reader = repository.newObjectReader()
        this.manager = manager
    }

    private List<DiffEntry> getDiff(RevTree newTree, RevTree oldTree){
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream())
        df.setRepository(repository)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setDetectRenames(true)
        List<DiffEntry> diffs = df.scan(oldTree, newTree)
        return diffs
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

    private showDiff(DiffEntry entry){
        if( !(entry.changeType in [DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.MODIFY])) return
        println "File: ${entry.newPath}; Change type: ${entry.changeType}"
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(stream)
        formatter.setRepository(repository)
        formatter.format(entry)
        println stream
    }

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

    private RevCommit extractCommit(String sha){
        RevWalk walk = new RevWalk(repository)
        ObjectId id = repository.resolve(sha)
        walk.parseCommit(id)
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
    }

    RealInterface computeTaskInterface(){
        List<Commit> commits = manager.searchByComment()
        def realInterface = new RealInterface()
        realInterface.classes = commits*.files as Set
        return realInterface
    }

    RealInterface computeTaskInterface(String taskId){
        List<Commit> commits = manager.searchByComment(taskId)
        def realInterface = new RealInterface()
        realInterface.classes = commits*.files as Set
        return realInterface
    }

}
