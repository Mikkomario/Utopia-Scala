package utopia.citadel.coder.model.scala

import utopia.flow.util.SelfComparable

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
	
	override def repr = this
	
	override def toString = s"@$keywordString"
	
	override def compareTo(o: ScalaDocKeyword) = orderIndex.compareTo(o.orderIndex)
}

object ScalaDocKeyword
{
	/**
	  * Keyword for describing function parameters
	  */
	case object Param extends ScalaDocKeyword
	{
		override protected def keywordString = "param"
		
		override protected def orderIndex = 7
	}
	
	/**
	  * Keyword for describing generic type parameters
	  */
	case object TypeParam extends ScalaDocKeyword
	{
		override protected def keywordString = "tparam"
		
		override protected def orderIndex = 8
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
}
