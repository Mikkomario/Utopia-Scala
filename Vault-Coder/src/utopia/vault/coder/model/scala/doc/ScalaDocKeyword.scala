package utopia.vault.coder.model.scala.doc

import utopia.flow.operator.SelfComparable

/**
  * An enumeration for different keywords used in scaladocs
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
sealed trait ScalaDocKeyword extends SelfComparable[ScalaDocKeyword]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return String of the keyword part (e.g. "param")
	  */
	protected def keywordString: String
	
	/**
	  * @return Priority used when ordering this keyword. Higher values come last.
	  */
	protected def orderIndex: Int
	
	
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def toString = s"@$keywordString"
	
	override def compareTo(o: ScalaDocKeyword) = orderIndex.compareTo(o.orderIndex)
}

object ScalaDocKeyword
{
	/**
	  * Values of this enumeration which don't take any parameters
	  */
	val staticValues = Vector[ScalaDocKeyword](Return, Author, Since)
	
	/**
	  * @param keywordString Searched keyword string (not including @)
	  * @return The keyword that matches that string
	  */
	def matching(keywordString: String, paramPart: => String) =
	{
		staticValues.find { _.keywordString == keywordString }
			.orElse {
				keywordString match
				{
					case "param" => Some(Param(paramPart))
					case "tparam" => Some(TypeParam(paramPart))
					case _ => None
				}
			}
	}
	
	/**
	  * Keyword for describing method return values
	  */
	case object Return extends ScalaDocKeyword
	{
		override protected def keywordString = "return"
		override protected def orderIndex = 10
	}
	
	/**
	  * Keyword for describing file author
	  */
	case object Author extends ScalaDocKeyword
	{
		override protected def keywordString = "author"
		override protected def orderIndex = 12
	}
	
	/**
	  * Keyword for describing file creation time
	  */
	case object Since extends ScalaDocKeyword
	{
		override protected def keywordString = "since"
		override protected def orderIndex = 13
	}
	
	/**
	  * Keyword for describing function parameters
	  */
	case class Param(paramName: String) extends ScalaDocKeyword
	{
		override protected def keywordString = "param"
		override protected def orderIndex = 7
		
		override def toString = s"${super.toString} $paramName"
	}
	
	/**
	  * Keyword for describing generic type parameters
	  */
	case class TypeParam(paramName: String) extends ScalaDocKeyword
	{
		override protected def keywordString = "tparam"
		override protected def orderIndex = 8
		
		override def toString = s"${super.toString} $paramName"
	}
}
