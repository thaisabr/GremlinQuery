//Git directory (to change)
gitdirectory = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}github${File.separator}rgms${File.separator}.git"

//Path of graph database (to change)
path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GraphBDs${File.separator}rgms${File.separator}graph.db"

//Mining commits by message
keywords = ["bookChapter", "bookchapter", "BookChapter", "book chapter", "Book Chapter"]

//Mining commits by file
files = ["BookChapter.groovy", "BookChapterController.groovy"]

//Filter changed files which name contains substring
exclude = ["rgms--test"]
