package utopia.flow

/**
  * @author Mikko Hilpinen
  * @since 13.11.2023, v2.3
  */
package object operator
{
	@deprecated("Please use MayBeZero instead", "v2.3")
	type CanBeZero[+Repr] = MayBeZero[Repr]
	@deprecated("Please use MayBeAboutZero instead", "v2.3")
	type CanBeAboutZero[-A, +Repr] = MayBeAboutZero[A, Repr]
}
