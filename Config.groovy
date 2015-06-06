//THIS FILE MUST BE AT THE OUTPUT DIRECTORY

//local project
project.path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GitHub${File.separator}OriginalRgms"

//Git directory used by JGit
gitdirectory = "${project.path}${File.separator}.git"

//Path of graph database used by Gremlin (to change)
graphDB.path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GraphBDs${File.separator}rgms${File.separator}graph.db"

//Mining commits by message (to change)
search.keywords = ["bookChapter", "bookchapter", "BookChapter", "book chapter", "Book Chapter"]

//Mining commits by file (to change)
search.files = ["grails-app/domain/rgms/publication/BookChapter.groovy", "grails-app/controllers/rgms/publication/BookChapterController.groovy"]

//Filter changed files which name contains substring
search.exclude = ["/test", "test/", ".gitignore", "README.md", ".iml", "target/"]

//project name (to change)
projectShortName = "rgms"
prefix = "$projectShortName--"