package utopia.vault.coder.model.scala

/**
  * An enumeration for different data type categories
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
sealed trait ScalaTypeCategory

object ScalaTypeCategory
{
	/**
	  * Represents a scala type that is passed as is (non-functional)
	  */
	case object Standard extends ScalaTypeCategory
	/**
	  * Represents a scala type that is passed as 'call-by-name' - i.e. as a reference only
	  */
	case object CallByName extends ScalaTypeCategory
	/**
	  * Represents a function type
	  * @param parameterTypes Types of the parameters this function accepts
	  */
	case class Function(parameterTypes: Vector[ScalaType] = Vector()) extends ScalaTypeCategory
}