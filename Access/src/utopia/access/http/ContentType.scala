package utopia.access.http

import scala.collection.immutable.Map
import scala.collection.immutable.HashMap
import java.net.URLConnection

object ContentType
{   
    // OTHER METHODS    ---------------------
    
    /**
     * Parses a string into a content type. Returns None if the content type couldn't be parsed. 
     * Only includes parameters which have a specified value
     */
    def parse(typeString: String) = 
    {
        val categoryAndRest = typeString.split("/", 2)
        
        if (categoryAndRest.size < 2)
        {
            None
        }
        else
        {
            val subTypeAndParams = categoryAndRest(1).split(";")
            
            val params: Map[String, String] = if (subTypeAndParams.size < 2) HashMap() else 
                    subTypeAndParams.tail.map { _.split("=", 2) }.filter { _.length == 2 }.map {
                    arr => (arr(0), arr(1)) }.toMap
            
            Some(ContentType(ContentCategory.parse(categoryAndRest(0)), subTypeAndParams(0), params))
        }
    }
    
    /**
     * Quesses a content type from a fileName
     */
    def guessFrom(fileName: String) = 
    {
        val cType = URLConnection.guessContentTypeFromName(fileName)
        if (cType == null) None else parse(cType)
    }
}

/**
 * A content type is used for describing requested / returned content transferred over http
 * @author Mikko Hilpinen
 * @since 20.8.2017
 */
case class ContentType(category: ContentCategory, subType: String, parameters: Map[String, String] = HashMap())
{
    // IMPLEMENTED METHODS    --------------
    
    override def toString = s"$category/$subType${ if (parameters.isEmpty) "" else 
            parameters.foldLeft("") { case (str, param) => str + s";${ param._1 }=${ param._2 }" } }"
    
    
    // OPERATORS    -----------------------
    
    /**
     * Creates a new content type with the assigned parameter
     */
    def +(paramName: String, paramValue: String) = ContentType(category, subType, parameters + 
            (paramName -> paramValue))
    
    /**
     * Creates a new content type with the assigned parameters
     */
    def ++(parameters: Map[String, String]) = ContentType(category, subType, this.parameters ++ parameters)
}