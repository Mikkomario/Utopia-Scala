package utopia.access.model.enumeration

import utopia.access.model.ContentType
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.{EqualsBy, EqualsFunction}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.{OpenEnumeration, OpenEnumerationValue}

/**
  * Content categories provide some general information about the specific content type.
  * There are certain predefined categories along with custom ones
  * @author Mikko Hilpinen
  * @since < v1.6
  */
trait ContentCategory extends OpenEnumerationValue[String] with EqualsBy
{
	// ABSTRACT ------------------------------------
	
	/**
	  * @return Name of this content type category (without a possible X- prefix)
	  */
	def name: String
	/**
	  * @return Whether this is a custom category (indicated by a X- prefix)
	  */
	def isCustom: Boolean
	
	
	// IMPLEMENTED METHODS    ----------------------
	
	override def identifier: String = name
	override protected def equalsProperties: Seq[Any] = Pair(name, isCustom)
	
	override def toString = if (isCustom) s"X-$name" else name
	
	
	// OPERATORS    --------------------------------
	
	/**
	  * Specifies a content type from this content category
	  */
	def /(subType: String) = ContentType(this, subType)
}

object ContentCategory
	extends OpenEnumeration[ContentCategory, String](identifiersMatch = EqualsFunction.stringCaseInsensitive)
{
	// ATTRIBUTES    ----------------
	
	case object Application extends ContentCategory
	{
		override val name: String = "application"
		override val isCustom: Boolean = false
		
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
	case object Audio extends ContentCategory
	{
		override val name: String = "audio"
		override val isCustom: Boolean = false
		
		/**
		  * A content type for midi audio files
		  */
		lazy val midi = /("midi")
		/**
		  * A content type for wav audio files
		  */
		lazy val wav = /("wav")
	}
	case object Image extends ContentCategory
	{
		override val name: String = "image"
		override val isCustom: Boolean = false
		
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
	case object Message extends ContentCategory
	{
		override val name: String = "message"
		override val isCustom: Boolean = false
	}
	case object MultiPart extends ContentCategory
	{
		override val name: String = "multipart"
		override val isCustom: Boolean = false
		
		/**
		  * A content type for mixed multipart content
		  */
		lazy val mixed = /("mixed")
	}
	case object Text extends ContentCategory
	{
		override val name: String = "text"
		override val isCustom: Boolean = false
		
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
	case object Video extends ContentCategory
	{
		override val name: String = "video"
		override val isCustom: Boolean = false
		
		/**
		  * A content type for mpeg video files
		  */
		lazy val mpeg = /("mpeg")
	}
	
	object Custom
	{
		def apply(name: String) = new Custom(name.notStartingWith("X-"))
	}
	class Custom(override val name: String) extends ContentCategory
	{
		override val isCustom: Boolean = true
	}
	
	
	// INITIAL CODE ------------------
	
	introduce(Application, Audio, Image, Message, MultiPart, Text, Video)
	
	
	// COMPUTED ----------------------
	
	@deprecated("Deprecated for removal. Please use .values instead", "v1.6")
	def existingOptions = values
	
	
	// IMPLEMENTED  -----------------
	
	override def findFor(identifier: String) =
		super.findFor(identifier.notStartingWith("X-"))
	
	
	// OTHER METHODS    -------------
	
	/**
	  * @param category A string representing a content type category
	  * @return A content type category matching that string
	  */
	def apply(category: String) = findFor(category).getOrElse { Custom(category) }
	
	/**
	  * Parses a content category string into a content category
	  */
	@deprecated("Please use .apply(String) instead", "v1.6")
	def parse(categoryString: String) = apply(categoryString)
}