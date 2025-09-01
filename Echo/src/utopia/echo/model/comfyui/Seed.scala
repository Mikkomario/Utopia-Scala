package utopia.echo.model.comfyui

import utopia.flow.view.immutable.View

import scala.language.implicitConversions
import scala.util.Random

object Seed
{
	// COMPUTED -----------------------
	
	/**
	 * @return An interface that always yields random seeds
	 */
	def random = RandomSeed
	
	
	// IMPLICIT -----------------------
	
	/**
	 * @param seed A fixed seed
	 * @return A seed that will always stay the same
	 */
	implicit def apply(seed: Long): FixedSeed = FixedSeed(seed)
	
	/**
	 * @param view A view that will yield the seeds
	 * @return A seed based on the specified view
	 */
	implicit def view(view: View[Long]): Seed = new SeedView(view)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param f A function for generating a new seed
	 * @return A seed using the specified generator
	 */
	def generate(f: => Long): Seed = view(View(f))
	
	
	// NESTED   -----------------------
	
	object RandomSeed extends Seed
	{
		private lazy val rand = new Random()
		
		override def next(): Long = rand.nextLong().abs
	}
	
	case class FixedSeed(seed: Long) extends Seed
	{
		override def next(): Long = seed
	}
	
	private class SeedView(view: View[Long]) extends Seed
	{
		override def next(): Long = view.value
	}
}

/**
 * An interface for generating the random seeds used to control generation randomness.
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
trait Seed extends Iterator[Long]
{
	override def hasNext: Boolean = true
}