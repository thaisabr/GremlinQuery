GremlinQuery
============

GremlinQuery is a groovy project that uses Gremlin Graph Traversal language to query a Neo4j database 
which contains metadata about project repositories hosted at github. It takes as input the directory 
path to the graph database and outputs a list of all the merge commits that happened in the database
history. Each item of this list contains the merge commit SHA-1 and their parents SHA-1. 
The output list is structured in the following manner:

MergeCommit1, Parent1, Parent2

...

MergeCommitN, Parent1, Parent2


Currently, the project saves the output list in a .csv file.
To run this project, first you need to clone and run Gitminer (https://github.com/pridkett/gitminer) in order to 
download the database containing the desired projects metadata. Next, you need to clone this 
repository, run 'mvn clean compile package' to download the project's dependencies, edit the directory path 
in the App.groovy main method and run this class.


