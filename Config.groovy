//project name (to change)
project = "rgms"

//Git directory (to change)
gitdirectory = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}github${File.separator}$project${File.separator}.git"

//Path of graph database (to change)
path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GraphBDs${File.separator}rgms${File.separator}graph.db"

//Mining commits by message
keywords = ["bookChapter", "bookchapter", "BookChapter", "book chapter", "Book Chapter"]

//Mining commits by file
files = ["grails-app/domain/rgms/publication/BookChapter.groovy", "grails-app/controllers/rgms/publication/BookChapterController.groovy"]

//Filter changed files which name contains substring
exclude = ["/test", "test/", "test/cucumber", "test/functional/", ".gitignore", "README.md", ".iml", "target/"]

prefix = "$project--"