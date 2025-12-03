package utopia.nexus.controller

import utopia.flow.collection.immutable.Single

/**
 * @author Mikko Hilpinen
 * @since 03.12.2025, v2.0
 */
package object write
{
	/**
	 * Names of headers used, by default, for controlling, whether enveloping is used
	 */
	private[write] val defaultEnvelopHeaderNames = Single("X-Envelop")
	/**
	 * Names of the query parameters used, by default, for controlling, whether enveloping is used
	 */
	private[write] val defaultEnvelopParamNames: Iterable[String] = Single("envelop")
}
