package utopia.echo.model.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Binary

/**
 * An enumeration for different network traffic directions (i.e. download & upload)
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
sealed trait NetworkTrafficDirection extends Binary[NetworkTrafficDirection]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Key used for this direction in Vast AI
	 */
	def key: String
	
	
	// IMPLEMENTED  --------------------
	
	override def self: NetworkTrafficDirection = this
}

object NetworkTrafficDirection
{
	// ATTRIBUTES   --------------------
	
	/**
	 * Download, then Upload
	 */
	val values = Pair[NetworkTrafficDirection](Download, Upload)
	
	
	// VALUES   ------------------------
	
	case object Download extends NetworkTrafficDirection
	{
		override val key: String = "down"
		
		override def unary_- : NetworkTrafficDirection = Upload
		override def compareTo(o: NetworkTrafficDirection): Int = if (o == Download) 0 else -1
	}
	case object Upload extends NetworkTrafficDirection
	{
		override val key: String = "up"
		
		override def unary_- : NetworkTrafficDirection = Download
		override def compareTo(o: NetworkTrafficDirection): Int = if (o == Upload) 0 else 1
	}
}