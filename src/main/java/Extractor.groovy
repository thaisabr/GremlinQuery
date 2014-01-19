import java.util.concurrent.TimeUnit

import org.eclipse.jgit.api.CleanCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import util.ChkoutCmd

class Extractor {

	// the url of the repository
	private String remoteUrl

	// the directory to clone in
	private String repositoryDir

	// the work folder
	private String projectsDirectory

	// the list of all merge commits
	private ArrayList<MergeCommit> listMergeCommit

	// the referred project
	private Project project

	// the git repository
	private Git git

	// conflicts counter
	private def CONFLICTS

	// signal of error execution
	private def ERROR

	public Extractor(Project project){
		this.project			= project
		this.listMergeCommit 	= this.project.listMergeCommit
		this.remoteUrl 			= this.project.url
		this.projectsDirectory	= "E:/TG/gitClones/"
		this.repositoryDir		= this.projectsDirectory + this.project.name + "/git"
		this.CONFLICTS 			= 0
		this.ERROR				= false

		this.setup()
	}

	def cloneRepository(){
		// prepare a new folder for the cloned repository
		File gitWorkDir = new File(repositoryDir)
		gitWorkDir.mkdirs()

		// then clone
		println "Cloning from " + remoteUrl + " to " + gitWorkDir + "..."
		Git.cloneRepository()
				.setURI(remoteUrl)
				.setDirectory(gitWorkDir)
				.call();

		// now open the created repository
		FileRepositoryBuilder builder = new FileRepositoryBuilder()
		Repository repository = builder.setGitDir(gitWorkDir)
				.readEnvironment() // scan environment GIT_* variables
				.findGitDir() // scan up the file system tree
				.build();

		println "Having repository: " + repository.getDirectory()
		repository.close()

	}

	def Git openRepository() {
		try {
			File gitWorkDir = new File(repositoryDir);
			Git git = Git.open(gitWorkDir);
			Repository repository = git.getRepository()
			return git
		} catch(org.eclipse.jgit.errors.RepositoryNotFoundException e){
			this.cloneRepository()
			this.openRepository()
		}
	}

	def listAllBranches() {
		List<Ref> refs = this.git.branchList().call();
		for (Ref ref : refs) {
			println "Branch-Before: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName();
		}
	}

	def checkoutMasterBranch() {
		ChkoutCmd chkcmd = new ChkoutCmd(this.git.getRepository());
		chkcmd.setName("refs/heads/master")
		chkcmd.setForce(true)
		Ref checkoutResult = chkcmd.call()
		println "Checked out branch sucessfully: " + checkoutResult.getName()
	}

	def Ref checkoutAndCreateBranch(String branchName, String commit){
		ChkoutCmd chkcmd = new ChkoutCmd(this.git.getRepository());
		chkcmd.setName(branchName)
		chkcmd.setStartPoint(commit)
		chkcmd.setCreateBranch(true)
		chkcmd.setForce(true);
		Ref checkoutResult = chkcmd.call()
		println "Checked out and created branch sucessfully: " + checkoutResult.getName()

		return checkoutResult
	}

	def deleteBranch(String branchName) {
		this.git.branchDelete()
				.setBranchNames(branchName)
				.setForce(true)
				.call()
	}

	def resetCommand(git, ref){
		ResetCommand resetCommand = git.reset()
		resetCommand.setMode(ResetType.HARD)
		resetCommand.setRef(ref)
		Ref resetResult = resetCommand.call()
		println "Reseted sucessfully to: " + resetResult.getName()
	}

	def runAllFiles(parent1, parent2) {
		// folder of the revisions being tested
		def allRevFolder = this.projectsDirectory + this.project.name + "/revisions/rev_" + parent1.substring(0, 5) + "_" + parent2.substring(0, 5)
		try{
			// opening the working directory
			this.git = openRepository();
			// git reset --hard SHA1_1
			this.resetCommand(this.git, parent1)
			// copy files for parent1 revision
			def destinationDir = allRevFolder + "/rev_left_" + parent1.substring(0, 5)
			this.copyFiles(this.repositoryDir, destinationDir, "")
			// git clean -f
			CleanCommand cleanCommandgit = this.git.clean()
			cleanCommandgit.call()
			// git checkout -b new SHA1_2
			def refNew = checkoutAndCreateBranch("new", parent2)
			// copy files for parent2 revision
			destinationDir = allRevFolder + "/rev_right_" + parent2.substring(0, 5)
			def excludeDir	   = "**/" + allRevFolder + "/**"
			this.copyFiles(this.repositoryDir, destinationDir, excludeDir)
			// git checkout master
			checkoutMasterBranch()
			// git merge new
			MergeCommand mergeCommand = this.git.merge()
			mergeCommand.include(refNew)
			MergeResult res = mergeCommand.call()
			if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
				CONFLICTS = CONFLICTS + 1
				println "Revision Base: " + res.getBase().toString()
				println "Conflitcts: " + res.getConflicts().toString()
				printConflicts(res)
				// git reset --hard BASE
				def revBase = (res.getBase().toString()).split()[1]
				this.resetCommand(this.git, revBase)
				// copy files for base revision
				destinationDir = allRevFolder + "/rev_base_" + revBase.substring(0, 5)
				this.copyFiles(this.repositoryDir, destinationDir, excludeDir)
				// the input revisions listed in a file
				this.writeRevisionsFile(parent1.substring(0, 5), parent2.substring(0, 5), revBase.substring(0, 5), allRevFolder)
			} else {
				// keeping only the conflicting revisions
				this.deleteFiles(allRevFolder)
			}
			// avoiding references issues
			this.deleteBranch("new")
		} catch(Exception e){
			this.ERROR = true
			println "ERROR: " + e
			// reseting
			this.deleteFiles(allRevFolder)
			this.restoreGitRepository()
			println "Trying again..."
		} finally {
			println "Closing git repository..."
			// closing git repository
			this.git.getRepository().close()
		}
	}

	def runOnlyConflicts(parent1, parent2) {
		try{
			// opening the working directory
			this.git = openRepository();
			// git reset --hard SHA1_1
			this.resetCommand(this.git, parent1)
			// git clean -f
			CleanCommand cleanCommandgit = this.git.clean()
			cleanCommandgit.call()
			// git checkout -b new SHA1_2
			def refNew = checkoutAndCreateBranch("new", parent2)
			// git checkout master
			checkoutMasterBranch()
			// git merge new
			MergeCommand mergeCommand = this.git.merge()
			mergeCommand.include(refNew)
			MergeResult res = mergeCommand.call()
			if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
				CONFLICTS = CONFLICTS + 1
				println "Revision Base: " + res.getBase().toString()
				println "Conflitcts: " + res.getConflicts().toString()
				def allConflicts = printConflicts(res)
				this.deleteBranch("new")
				this.git.getRepository().close()
				this.moveConflictingFiles(parent1, parent2, allConflicts)
			}
			// avoiding references issues
			this.deleteBranch("new")
		} catch(Exception e){
			this.ERROR = true
			println "ERROR: " + e
			this.restoreGitRepository()
			println "Trying again..."
		} finally {
			println "Closing git repository..."
			// closing git repository
			this.git.getRepository().close()
		}
	}
	
	def moveConflictingFiles(parent1, parent2, allConflicts) {
		// folder of the revisions being tested
		def allRevFolder = this.projectsDirectory + this.project.name + "/revisions/rev_" + parent1.substring(0, 5) + "_" + parent2.substring(0, 5)
		try{
			// opening the working directory
			this.git = openRepository();
			// git reset --hard SHA1_1
			this.resetCommand(this.git, parent1)
			// copy files for parent1 revision
			def destinationDir = allRevFolder + "/rev_left_" + parent1.substring(0, 5)
			this.copyFiles(this.repositoryDir, destinationDir, allConflicts)
			// git clean -f
			CleanCommand cleanCommandgit = this.git.clean()
			cleanCommandgit.call()
			// git checkout -b new SHA1_2
			def refNew = checkoutAndCreateBranch("new", parent2)
			// copy files for parent2 revision
			destinationDir = allRevFolder + "/rev_right_" + parent2.substring(0, 5)
			def excludeDir	   = "**/" + allRevFolder + "/**"
			this.copyFiles(this.repositoryDir, destinationDir, allConflicts)
			// git checkout master
			checkoutMasterBranch()
			// git merge new
			MergeCommand mergeCommand = this.git.merge()
			mergeCommand.include(refNew)
			MergeResult res = mergeCommand.call()
			if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
				// git reset --hard BASE
				def revBase = (res.getBase().toString()).split()[1]
				this.resetCommand(this.git, revBase)
				// copy files for base revision
				destinationDir = allRevFolder + "/rev_base_" + revBase.substring(0, 5)
				this.copyFiles(this.repositoryDir, destinationDir, allConflicts)
				// the input revisions listed in a file
				this.writeRevisionsFile(parent1.substring(0, 5), parent2.substring(0, 5), revBase.substring(0, 5), allRevFolder)
			} 
			// avoiding references issues
			this.deleteBranch("new")
		} catch(Exception e){
			this.ERROR = true
			println "ERROR: " + e
			// reseting
			this.deleteFiles(allRevFolder)
			this.restoreGitRepository()
			println "Trying again..."
		} finally {
			// closing git repository
			this.git.getRepository().close()
		}
	}

	def countConflicts(parent1, parent2){
		try{
			// opening the working directory
			this.git = openRepository();
			// git reset --hard SHA1_1
			this.resetCommand(this.git, parent1)
			// git clean -f
			CleanCommand cleanCommandgit = this.git.clean()
			cleanCommandgit.call()
			// git checkout -b new SHA1_2
			def refNew = checkoutAndCreateBranch("new", parent2)
			// git checkout master
			checkoutMasterBranch()
			// git merge new
			MergeCommand mergeCommand = this.git.merge()
			mergeCommand.include(refNew)
			MergeResult res = mergeCommand.call()
			if (res.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
				CONFLICTS = CONFLICTS + 1
				println "Revision Base: " + res.getBase().toString()
				println "Conflitcts: " + res.getConflicts().toString()
				printConflicts(res)
			}
			// avoiding references issues
			this.deleteBranch("new")
		} catch(Exception e){
			this.ERROR = true
			println "ERROR: " + e
			this.restoreGitRepository()
			println "Trying again..."
		} finally {
			println "Closing git repository..."
			// closing git repository
			this.git.getRepository().close()
		}
	}

	def printConflicts(MergeResult res) {
		Map allConflicts = res.getConflicts();
		def listConflicts = []
		for (String path : allConflicts.keySet()) {
			int[][] c = allConflicts.get(path);
			println "Conflicts in file " + path
			for (int i = 0; i < c.length; ++i) {
				println " Conflict #" + i
				for (int j = 0; j < (c[i].length) - 1; ++j) {
					if (c[i][j] >= 0)
						println" Chunk for " + res.getMergedCommits()[j] + " starts on line #" + c[i][j];
				}
			}
			listConflicts.add(path)
		}
		return listConflicts
	}

	def copyFiles(String sourceDir, String destinationDir, String excludeDir){
		new AntBuilder().copy(todir: destinationDir) {
			fileset(dir: sourceDir){
				exclude(name:excludeDir)
			}
		}
	}

	def copyFiles(String sourceDir, String destinationDir, ArrayList<String> listConflicts){
		AntBuilder ant = new AntBuilder()
		listConflicts.each {
			String file = "**/" + it
			ant.copy(todir: destinationDir) {
				fileset(dir: sourceDir){
					include(name:file)
				}
			}
		}
	}

	def deleteFiles(String dir){
		(new AntBuilder()).delete(dir:dir,failonerror:false)
	}

	def writeRevisionsFile(String leftRev, String rightRev, String baseRev, String dir){
		def filePath = dir + "/rev_" + leftRev + "-" + rightRev + ".revision"
		def out = new File(filePath)
		// deleting old files if it exists
		out.delete()
		out = new File(filePath)
		def row = "rev_left_" + leftRev
		out.append row
		out.append '\n'
		row = "rev_base_" + baseRev
		out.append row
		out.append '\n'
		row = "rev_right_" + rightRev
		out.append row
		out.append '\n'
	}

	def setup(){
		println "Setupping..."
		// keeping a backup dir
		new AntBuilder().copy(todir:"E:/TG/gitClones/temp/"+this.project.name+"/git") {fileset(dir: this.projectsDirectory+this.project.name+"/git", defaultExcludes: false){}}
		println "----------------------"
	}

	def restoreGitRepository(){
		println "Restoring Git repository..."
		this.git.getRepository().close()
		// restoring the backup dir
		new File(this.projectsDirectory+this.project.name+"/git").deleteDir()
		new AntBuilder().copy(todir:this.projectsDirectory+this.project.name+"/git") {fileset(dir:"E:/TG/gitClones/temp/"+this.project.name+"/git" , defaultExcludes: false){}}
	}

	def extractCommits(){
		def iterationCounter = 1
		Iterator ite = this.listMergeCommit.iterator()
		MergeCommit mc = null
		while(ite.hasNext()){
			if(!this.ERROR){
				mc = (MergeCommit)ite.next()
				println ("Running " + iterationCounter + "/" + this.listMergeCommit.size())
			} else {
				this.ERROR = false;
			}

			// the commits to checkout
			def SHA_1 = mc.parent1
			def SHA_2 = mc.parent2
			this.runOnlyConflicts(SHA_1, SHA_2)

			if(!this.ERROR){
				println "----------------------"
				iterationCounter++
			}
		}
		println ("Number of conflicts: " + CONFLICTS)
	}

	static void main (String[] args){
		//		//testing
		//		MergeCommit mc = new MergeCommit()
		//		mc.sha 		= "b4df7ee0b908f16cce2f7c819927fe5deb8cb6b9"
		//		mc.parent1  = "fd21ef43df591ef86ad899d96d2d6a821ebb342d"
		//		mc.parent2  = "576c6b3966cb85353ba874f6c9f2e65c4a89c70b"
		//
		//		ArrayList<MergeCommit> lm = new ArrayList<MergeCommit>()
		//		lm.add(mc)
		//
		//		Project p = new Project()
		//		p.name = "rgms"
		//		p.url = "https://github.com/spgroup/rgms.git"
		//		p.graph = "C:/Users/Guilherme/Documents/workspace/gitminer-master/gitminer-master/graph.db_30-10"
		//		p.listMergeCommit = lm
		//
		//		Extractor ex = new Extractor(p)
		//		//ex.extractCommits()

		AntBuilder ant = new AntBuilder()
		ant.copy(todir: "todir") {
			fileset(dir: "gitClones/tgm/git"){
				include(name:"**/CaseStudy.java")
			}
		}
	}
}
