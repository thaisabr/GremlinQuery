package util


class Util {

    static config = new ConfigSlurper().parse(Util.class.classLoader.getResource("Config.groovy"))
    static final FILE_SEPARATOR_REGEX = /(\\|\/)/
    static final NEW_LINE_REGEX = /\r\n|\n/

    public static List getChangedProductionFiles(List files){
        files?.findAll{ file ->
            !(config.search.exclude).any{ file.contains(it) }
        }
    }

}
