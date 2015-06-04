package util


class Util {

    static config = new ConfigSlurper().parse(new File("Config.groovy").toURI().toURL())

    public static List getChangedProductionFiles(List files){
        def productionFiles = []
        if(!files?.empty) {
            def rejectedFiles = files.findAll{ file ->
                (config.exclude).any{ file.contains(it) }
            }
            productionFiles = files - rejectedFiles
        }
        return productionFiles
    }

}
