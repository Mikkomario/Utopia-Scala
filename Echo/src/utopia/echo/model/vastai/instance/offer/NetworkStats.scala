package utopia.echo.model.vastai.instance.offer

import utopia.echo.model.enumeration.NetworkTrafficDirection
import utopia.echo.model.unit.ByteCount
import utopia.flow.generic.factory.{FromModelFactory, FromModelFactoryWithSchema}
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.DoubleType

object NetworkStats
{
	// ATTRIBUTES   -----------------
	
	private val parsers = NetworkTrafficDirection.values.view.map { dir => dir -> new NetworkStatsParser(dir) }.toMap
	
	
	// OTHER    ---------------------
	
	/**
	 * @param direction Targeted direction
	 * @return A parser for parsing that direction's information from a model
	 */
	def apply(direction: NetworkTrafficDirection): FromModelFactory[NetworkStats] = parsers(direction)
	
	
	// NESTED   ---------------------
	
	private class NetworkStatsParser(direction: NetworkTrafficDirection)
		extends FromModelFactoryWithSchema[NetworkStats]
	{
		// ATTRIBUTES   -------------
		
		private val baseKey = s"inet_${direction.key}"
		private val costKey = s"${baseKey}_cost"
		
		override val schema: ModelDeclaration = ModelDeclaration(baseKey -> DoubleType)
		
		
		// IMPLEMENTED  -------------
		
		override protected def fromValidatedModel(model: Model): NetworkStats =
			NetworkStats(model(baseKey).getDouble, model(costKey).getDouble)
	}
}

/**
 * Contains information about a machine's network speed & cost
 * @param speedMBs Network speed in MB/s
 * @param costPerGb Network usage cost in $/GB
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class NetworkStats(speedMBs: Double, costPerGb: Double)
{
	/**
	 * @param downloadSize Size of the download/upload
	 * @return Cost of that download/upload, in $
	 */
	def costOf(downloadSize: ByteCount) = downloadSize.gigas * costPerGb
}