import org.eclipse.jgit.api.CleanCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.lib.ObjectId;


class DownloadManager {
	
	
	
	
	DownloadManager(String filename){
		
	}
	
	def retrieveBaseRevision(){
		
	}
	
	private void writeBaserevisions(){
		
	}
	
	def downloadSources(){
		
	}
	
	public static void main (String[] args){
		//open repository
		
		File gitWorkDir = new File('/Users/paolaaccioly/gitClones/rgms/git');
		
		gitWorkDir.mkdirs()
		println "Cloning from " + 'https://github.com/spgroup/rgms' + " to " + gitWorkDir + "..."
		Git.cloneRepository()
				.setURI('https://github.com/spgroup/rgms')
				.setDirectory(gitWorkDir)
				.call();
				
		Git git = Git.open(gitWorkDir);
		Repository repository = git.getRepository()
		println "repository cloned!"
		
		//initialize revision walk
		RevWalk walk = new RevWalk(repository);
		walk.setRetainBody(false);
		
		walk.setRevFilter(RevFilter.MERGE_BASE);
		
		walk.reset();
		
		ObjectId shaJ = ObjectId.fromString('4716cfa0bc85b7ca3dd6bfa4baac4fcf830aa2e3')
		
		ObjectId shaI = ObjectId.fromString('89e1a9b0ff92f5c8db994763a14c75a4cf11daf5')
		
		walk.markStart(walk.parseCommit(shaJ));
		
		walk.markStart(walk.parseCommit(shaI));
		
		ObjectId commonAncestor = walk.next();
		
		println(commonAncestor.toString())
	}

	
}
