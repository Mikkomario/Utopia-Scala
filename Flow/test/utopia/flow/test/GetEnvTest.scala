package utopia.flow.test

import utopia.flow.util.Env

/**
 * Tests env variable listing
 * @author Mikko Hilpinen
 * @since 01.03.2026, v2.8
 */
object GetEnvTest extends App
{
	Env.properties.sortBy { _.name }.foreach { p => println(s"${ p.name }: ${ p.value }") }
}
