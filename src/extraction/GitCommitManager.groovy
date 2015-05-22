package extraction

import org.eclipse.jgit.api.BlameCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
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

class GitCommitManager {

    Repository repository
    ObjectReader reader
    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public GitCommitManager(){
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(config.gitdirectory)).setMustExist(true).build()
        reader = repository.newObjectReader()
    }

    private CanonicalTreeParser getTreeParser(RevCommit commit){
        CanonicalTreeParser tree = new CanonicalTreeParser()
        tree.reset(reader, commit.tree)
        return tree
    }

    private List<DiffEntry> getDiff(CanonicalTreeParser newTree, CanonicalTreeParser oldTree){
        List<DiffEntry> diff = new Git(repository).diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .setPathFilter(PathFilter.create(filename))
                .call()
        return diff
    }

    private List<DiffEntry> getDiffFromFile(String filename, CanonicalTreeParser newTree, CanonicalTreeParser oldTree){
        List<DiffEntry> diff = new Git(repository).diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .setPathFilter(PathFilter.create(filename))
                .call()
        return diff
    }

    private void showDiff(String filename, DiffEntry entry){
        if( !(entry.changeType in [DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.MODIFY])) return
        println "File: $filename; Change type: ${entry.changeType}"
        DiffFormatter formatter = new DiffFormatter(System.out)
        formatter.setRepository(repository)
        formatter.format(entry)
    }

    private static getLines(Repository repository, ObjectId commitID, String name) {
        RevWalk revWalk = new RevWalk(repository)
        RevCommit commit = revWalk.parseCommit(commitID)
        RevTree tree = commit.getTree()

        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(name))
        if (!treeWalk.next()) {
            throw new IllegalStateException("Did not find expected file $name")
        }

        ObjectId objectId = treeWalk.getObjectId(0)
        ObjectLoader loader = repository.open(objectId)

        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        revWalk.dispose()

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
            println "Commit: $rev, name: ${rev.getName()}, id: ${rev.getId().getName()}"
            count++
        }
        println "Had $count commits overall on current branch"
    }

    def showAllChangesFromCommit(String sha){
        RevCommit commit = extractCommit(sha)
        RevCommit parent = extractCommit(commit.parents[0].name) //se for merge, vai ter mais de um pai?

        CanonicalTreeParser newTreeIter = getTreeParser(commit)
        CanonicalTreeParser oldTreeIter = getTreeParser(parent)

        List<DiffEntry> diffs = getDiff(newTreeIter, oldTreeIter)
        diffs.each{ entry ->
            println "Entry: $entry"
            DiffFormatter formatter = new DiffFormatter(System.out)
            formatter.setRepository(repository)
            formatter.format(entry)
        }

    }

    def showChanges(String sha, List changedFiles){
        RevCommit commit = extractCommit(sha)
        RevCommit parent = extractCommit(commit.parents[0].name)
        CanonicalTreeParser newTreeParser = getTreeParser(commit)
        CanonicalTreeParser oldTreeParser = getTreeParser(parent)

        changedFiles.each{ file ->
            List<DiffEntry> diff = getDiffFromFile(file, newTreeParser, oldTreeParser)
            diff.each { entry ->
                showDiff(file, entry)
            }

            println "<CURRENT VERSION>"
            println "LINES: ${showFileContent(commit, file)}"

            println "<PARENT VERSION>"
            println "LINES: ${showFileContent(parent, file)}"
        }
    }

    def showFileContent(RevCommit commit, String fileName){
        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(commit.tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(fileName))

        if (!treeWalk.next()) {
            throw new IllegalStateException("Did not find expected file $fileName")
        }

        ObjectId objectId = treeWalk.getObjectId(0)
        ObjectLoader loader = repository.open(objectId)
        loader.copyTo(System.out)

        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        return stream.toString().readLines().size()
    }

    //show file history by line
    def showChangedLines(String filename){
        BlameCommand blamer = new BlameCommand(repository)
        ObjectId commitID = repository.resolve("HEAD")
        blamer.setStartCommit(commitID)
        blamer.setFilePath(filename)
        BlameResult blame = blamer.call()

        def lines = getLines(repository, commitID, filename)
        lines.eachWithIndex{ line, i ->
            RevCommit commit = blame.getSourceCommit(i)
            println "Line $i(${commit.name}): $line"
        }
        println "Displayed commits responsible for ${lines.size()} lines of $filename"
    }

}
