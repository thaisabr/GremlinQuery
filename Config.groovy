//THIS FILE MUST BE AT THE OUTPUT DIRECTORY

//local project
project.path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GitHub${File.separator}OriginalRgms"

//Git directory used by JGit
gitdirectory = "${project.path}${File.separator}.git"

//Path of graph database used by Gremlin (to change)
graphDB.path = "${System.getProperty("user.home")}${File.separator}Documents${File.separator}GraphBDs${File.separator}rgms${File.separator}graph.db"

//Mining commits by message (to change)
search.keywords = ["bookchapter", "book chapter"]

//Mining commits by file (to change)
search.files = ["grails-app${File.separator}domain${File.separator}rgms${File.separator}publication${File.separator}BookChapter.groovy",
                "grails-app${File.separator}controllers${File.separator}rgms${File.separator}publication${File.separator}BookChapterController.groovy"]

//Filter changed files which name contains substring
search.exclude = ["${File.separator}test", "test${File.separator}", ".gitignore", "README.md", ".iml", "target${File.separator}",
                  "${File.separator}conf${File.separator}", "application.properties", "web-app${File.separator}", ".exe",
                  ".db", ".jar", ".classpath", ".project", ".settings"]

//project name (to change)
projectShortName = "rgms"
prefix = "$projectShortName--"