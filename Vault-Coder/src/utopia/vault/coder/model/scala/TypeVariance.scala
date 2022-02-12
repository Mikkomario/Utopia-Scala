package utopia.vault.coder.model.scala

/**
  * An enumeration describing generic type variance options. Variance determines whether a subclass may be used
  * instead of a superclass or vise versa.
  * @author Mikko Hilpinen
  * @since 12.2.2022, v1.5
  */
sealed trait TypeVariance
{
	/**
	  * @return Prefix added before a generic type to indicate this type of variance
	  */
	def typePrefix: String
}

object TypeVariance
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * All available type variance options
	  */
	val values = Vector(Covariance, Contravariance, Invariance)
	/**
	  * Variance options that need to be declared explicitly
	  */
	val explicitValues = Vector(Covariance, Contravariance)
	
	
	// NESTED   ------------------------------
	
	/**
	  * Used to declare that a class may be used in place of its superclass.
	  * E.g. Vector[Double] may be used as Vector[Any]
	  */
	case object Covariance extends TypeVariance
	{
		override def typePrefix = "+"
	}
	/**
	  * Used to declare that a superclass may be used in place of a subclass.
	  * E.g. A function may accept Iterable[Double] instead of only Vector[Double]
	  */
	case object Contravariance extends TypeVariance
	{
		override def typePrefix = "-"
	}
	/**
	  * Used to declare that only the specified class may be used in this context.
	  */
	case object Invariance extends TypeVariance
	{
		override def typePrefix = ""
	}
}
