package utopia.access.http

object Method
{
    // VALUES    -----------------
    
    /**
     * The GET method is used for retrieving data from the server
     */
    case object Get extends Method("GET")
    /**
     * The POST method is used for storing / pushing new data to the server
     */
    case object Post extends Method("POST")
    /**
     * The PUT method is used for updating / overwriting existing data on the server
     */
    case object Put extends Method("PUT")
    /**
     * The DELETE method is used for deleting data on the server
     */
    case object Delete extends Method("DELETE")
    /**
      * PATCH method is used for updating parts of existing data on the server
      */
    case object Patch extends Method("PATCH")
    
    
    // ATTRIBUTES    -------------
    
    /**
     * The existing method values
     */
    lazy val values = Vector(Get, Post, Put, Delete, Patch)
    
    
    // OTHER METHODS    ---------
    
    /**
     * Parses a string into a method, if it matches one (case-insensitive)
     */
    def parse(methodString: String): Option[Method] = 
    {
        val trimmedName = methodString.toUpperCase().trim()
        values.find { _.name == trimmedName }
    }    
}

/**
 * Each http method represents a different function a client wants to perform on server side
 * @author Mikko Hilpinen
 * @since 24.8.2017
 * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 */
sealed abstract class Method(val name: String)
{
    override def toString = name
}