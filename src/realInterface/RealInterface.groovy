package realInterface


class RealInterface {

    Set files //changed classes
    Set methods //static and non-static changed methods
    Set staticFields //changed static fields
    Set fields //changed fields

    public RealInterface(){
        this.files = [] as Set
        this.methods = [] as Set
        this.staticFields = [] as Set
        this.fields = [] as Set
    }

}
