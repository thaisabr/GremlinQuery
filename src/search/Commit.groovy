package search
/**
 * Created by Thaís on 05/05/2015.
 */
class Commit {

    String hash
    String message
    ArrayList<String> files
    String author
    String date

    public String toString(){
         "$hash*$message*$files*$author*$date"
    }

}
