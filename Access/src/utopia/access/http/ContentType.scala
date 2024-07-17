package utopia.access.http

import utopia.flow.util.StringExtensions._

import java.net.URLConnection
import java.nio.charset.Charset
import scala.util.Try

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
  * @param category The category in which this content type belongs (e.g. "application" or "image")
  * @param subType The name of this this specific content type within 'category' (e.g. "json" or "png")
  * @param parameters Additional parameters, such as the character set used
 * @author Mikko Hilpinen
 * @since 20.8.2017
 */
case class ContentType(category: ContentCategory, subType: String, parameters: Map[String, String] = Map())
{
    // ATTRIBUTES   --------------------
    
    /**
      * The character set specified in this content type
      */
    // TODO: Add support for case-insensitivity
    // TODO: Also, add support for default character sets (?)
    lazy val charset = parameters.get("charset").flatMap { c => Try { Charset.forName(c) }.toOption }
    
    
    // IMPLEMENTED    --------------
    
    override def toString = {
        val parametersPart = parameters.view.map { case (key, value) => s";$key=$value" }.mkString
        s"$category/$subType$parametersPart"
    }
    
    
    // OTHER    -----------------------
    
    /**
     * Creates a new content type with the assigned parameter
     */
    def +(param: (String, String)) = copy(parameters = parameters + param)
    /**
     * Creates a new content type with the assigned parameters
     */
    def ++(parameters: IterableOnce[(String, String)]) = copy(parameters = this.parameters ++ parameters)
    
    /**
      * @param charset Character set to assign
      * @return Copy of this content type with the specified character set
      */
    def withCharset(charset: Charset) = this + ("charset" -> charset.name())
}