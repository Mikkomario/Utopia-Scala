package utopia.echo.model.unit

import utopia.echo.model.enumeration.ByteCountUnit.{GibiBytes, MebiBytes, TebiBytes}

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
		 * @return This amount in MiB
		 */
		def mib = ByteCount(i, MebiBytes)
		/**
		 * @return This amount in GiB
		 */
		def gib = ByteCount(i, GibiBytes)
		/**
		 * @return This amount in TiB
		 */
		def tib = ByteCount(i, TebiBytes)
	}
}
