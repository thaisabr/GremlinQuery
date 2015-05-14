package search
/**
 * Created by Thaís on 05/05/2015.
 */
class Commit {

    String hash
    String message
    List files
    String author
    String date

    public String toString(){
         "$hash*$date*$author*${message.replaceAll("[\n\r]", ";")}*${files.toListString()}"
    }

}
