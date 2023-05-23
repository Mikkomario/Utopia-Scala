package utopia.flow

/**
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
package object util
{
	/**
	  * A function type which mutates a value without altering its type
	  */
	type Mutate[A] = A => A
}
