package utopia.access.http

object ContentCategory
{
    // ATTRIBUTES    ----------------
    
    case object Application extends ContentCategory("application")
    {
        /**
         * A content type for json documents
         */
        lazy val json = /("json")
        /**
         * A content type for xml documents
         */
        lazy val xml = /("xml")
        /**
         * A content type for pdf (portable document format)
         */
        lazy val pdf = /("pdf")
        /**
         * A content type for zip files
         */
        lazy val zip = /("zip")
    }
    case object Audio extends ContentCategory("audio")
    {
        /**
         * A content type for midi audio files
         */
        lazy val midi = /("midi")
        /**
         * A content type for wav audio files
         */
        lazy val wav = /("wav")
    }
    case object Image extends ContentCategory("image")
    {
        /**
         * A content type for jpeg / jpg images
         */
        lazy val jpeg = /("jpeg")
        /**
         * A content type for png images
         */
        lazy val png = /("png")
        /**
         * A content type for svg images
         */
        lazy val svg = /("svg+xml")
    }
    case object Message extends ContentCategory("message")
    case object MultiPart extends ContentCategory("multipart")
    {
        /**
         * A content type for mixed multipart content
         */
        lazy val mixed = /("mixed")
    }
    case object Text extends ContentCategory("text")
    {
        /**
         * A content type for plaintext
         */
        lazy val plain = /("plain")
        /**
         * A content type for html documents
         */
        lazy val html = /("html")
        /**
         * A content type for csv documents
         */
        lazy val csv = /("csv")
    }
    case object Video extends ContentCategory("video")
    {
        /**
         * A content type for mpeg video files
         */
        lazy val mpeg = /("mpeg")
    }
    
    case class Custom(override val name: String) extends ContentCategory(name, true)
    
    lazy val existingOptions: Vector[ContentCategory] =
        Vector(Application, Audio, Image, Message, MultiPart, Text, Video)
    
    
    // OTHER METHODS    -------------
    
    /**
     * Parses a content category string into a content category
     */
    def parse(categoryString: String) =
        if (categoryString.startsWith("X-"))
            Custom(categoryString.substring(2))
        else
            existingOptions.find { _.name.equalsIgnoreCase(categoryString) }.getOrElse(Custom(categoryString))
}

/**
 * Content categories provide some general information about the specific content type. There are 
 * certain predefined categories along with custom ones
 */
sealed abstract class ContentCategory(val name: String, val isCustom: Boolean = false)
{
    // IMPLEMENTED METHODS    ----------------------
    
    override def toString = if (isCustom) s"X-$name" else name
    
    
    // OPERATORS    --------------------------------
    
    /**
     * Specifies a content type from this content category
     */
    def /(subType: String) = ContentType(this, subType)
}