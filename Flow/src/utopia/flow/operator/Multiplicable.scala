package utopia.flow.operator

/**
  * A common trait for items which can be multiplied but not scaled linearly
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Multiplicable[+Repr] extends Any with Scalable[Repr, Int] with Reversible[Repr]
{
	// IMPLEMENTED  ---------------------------
	
	override def unary_-() = this * -1
}
