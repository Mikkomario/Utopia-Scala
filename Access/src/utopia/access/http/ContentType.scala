package utopia.access.http

import utopia.flow.util.StringExtensions._

import java.net.URLConnection

object ContentType
{   
    // OTHER METHODS    ---------------------
    
    /**
     * Parses a string into a content type. Returns None if the content type couldn't be parsed. 
     * Only includes parameters which have a specified value
     */
    def parse(typeString: String) = {
        val (category, afterCategory) = typeString.splitAtFirst("/").toTuple
        if (afterCategory.isEmpty)
            None
        else {
            val subTypeAndParams = afterCategory.split(";")
            val params = {
                if (subTypeAndParams.length < 2)
                    Map[String, String]()
                else
                    subTypeAndParams.view.tail.map { _.splitAtFirst("=").toTuple }.toMap
            }
            Some(apply(ContentCategory.parse(category), subTypeAndParams.head, params))
        }
    }
    
    /**
     * Guesses a content type from a fileName
     */
    def guessFrom(fileName: String) = {
        val cType = URLConnection.guessContentTypeFromName(fileName)
        if (cType == null) None else parse(cType)
    }
}

/**
 * A content type is used for describing requested / returned content transferred over http
 * @author Mikko Hilpinen
 * @since 20.8.2017
 */
case class ContentType(category: ContentCategory, subType: String, parameters: Map[String, String] = Map())
{
    // IMPLEMENTED METHODS    --------------
    
    override def toString = {
        val parametersPart = parameters.view.map { case (key, value) => s";$key=$value" }.mkString
        s"$category/$subType$parametersPart"
    }
    
    
    // OPERATORS    -----------------------
    
    /**
     * Creates a new content type with the assigned parameter
     */
    def +(paramName: String, paramValue: String) =
        ContentType(category, subType, parameters + (paramName -> paramValue))
    
    /**
     * Creates a new content type with the assigned parameters
     */
    def ++(parameters: Map[String, String]) = ContentType(category, subType, this.parameters ++ parameters)
}