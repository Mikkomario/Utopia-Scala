package utopia.paradigm.test

import utopia.paradigm.path.ProjectilePath

/**
  * Visualizes the projectile path
  * @author Mikko Hilpinen
  * @since 11.06.2025, v1.7.3
  */
object ProjectilePathTest extends App
{
	private def test(mod: Double) = {
		val curve = ProjectilePath(mod, 30)
		println(s"\nMod = $mod")
		Iterator.iterate(0.0) { _ + 0.1 }.takeWhile { _ <= 1.0 }.foreach { p =>
			println(s"${ (p * 10).round.toString.padTo(3, " ").mkString }: ${ "-" * curve(p).round.toInt }")
		}
	}
	
	test(-0.5)
	test(-0.25)
	test(0.0)
	test(0.25)
	test(0.5)
	test(0.75)
	test(1.0)
	test(1.25)
}
