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
To run this project, you need the following instructions:

1- Clone and run Gitminer (https://github.com/pridkett/gitminer), according with their instructions to 
download the database containing the desired projects metadata. 

2- Once you have the graph.db file in your computer, clone or download the GremlinQuery project

3- Run 'mvn eclipse:clean' to download the project's dependencies. 

4- Import the project into eclipse using "Import > Maven > Existing project into workspace..."  

5- In the App.groovy main method, point the directory path where your graph.db folder is located 

6- Run App.groovy. The .csv file will be created in the directory where you cloned the GremlinQuery
project.

