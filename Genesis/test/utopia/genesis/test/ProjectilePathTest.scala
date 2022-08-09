package utopia.genesis.test

import utopia.flow.util.{Counter, Generator}
import utopia.paradigm.path.ProjectilePath

/**
  * Simply prints projectile path results
  * @author Mikko Hilpinen
  * @since 18.4.2020, v2.2.1
  */
object ProjectilePathTest extends App
{
	val curve = ProjectilePath()
	val progress = Generator(0.0) { _ + 0.1 }.iterator.takeWhile { _ <= 1.0 }
	
	progress.foreach { p =>
		println(s"${(p * 100).toInt}% => ${(curve(p) * 100).toInt}%")
	}
}
