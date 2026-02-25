package utopia.echo.model.unit

import utopia.echo.model.enumeration.ByteCountUnit.{GigaBytes, MegaBytes, TeraBytes}

/**
 * Provides utility functions for constructing byte counts
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
object ByteCountExtensions
{
	implicit class ByteCountInt(val i: Int) extends AnyVal
	{
		/**
		 * @return This amount in MB
		 */
		def mb = ByteCount(i, MegaBytes)
		/**
		 * @return This amount in GB
		 */
		def gb = ByteCount(i, GigaBytes)
		/**
		 * @return This amount in TB
		 */
		def tb = ByteCount(i, TeraBytes)
	}
}
