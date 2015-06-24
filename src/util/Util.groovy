package util


class Util {

    static config = new ConfigSlurper().parse(Util.class.classLoader.getResource("Config.groovy"))
    static final FILE_SEPARATOR_REGEX = /(\\|\/)/
    static final DELETED_FILE_OLD_PATH_SUFIX = ".orig"

    public static List getChangedProductionFiles(List files){
        def productionFiles = []
        if(!files?.empty) {
            def rejectedFiles = files.findAll{ file ->
                (config.search.exclude).any{ file.contains(it) }
            }
            productionFiles = files - rejectedFiles
        }
        return productionFiles
    }

}
