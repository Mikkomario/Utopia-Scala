package utopia.echo.model.vastai.instance.offer

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.equality.EqualsExtensions._

/**
 * An enumeration for machine verification
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
sealed trait VerificationStatus extends SelfComparable[VerificationStatus]
{
	/**
	 * @return A key used for this status in Vast AI
	 */
	def key: String
	/**
	 * @return Whether this indicates that a machine is verified
	 */
	def isVerified: Boolean
	
	override def self: VerificationStatus = this
}

object VerificationStatus
{
	// ATTRIBUTES   -----------------
	
	/**
	 * Different possible values of this enumeration
	 */
	val values = Vector[VerificationStatus](Verified, Unverified, Deverified)
	
	
	// OTHER    ---------------------
	
	def findForKey(key: String) = values.find { _.key ~== key }
	def forKey(key: String) = findForKey(key)
		.toTry { new NoSuchElementException(s"No verification status matches \"$key\"") }
	
	
	// VALUES   ---------------------
	
	case object Unverified extends VerificationStatus
	{
		override val key: String = "unverified"
		override val isVerified: Boolean = false
		
		override def compareTo(o: VerificationStatus): Int = o match {
			case Unverified => 0
			case Verified => -1
			case Deverified => 1
		}
	}
	case object Verified extends VerificationStatus
	{
		override val key: String = "verified"
		override val isVerified: Boolean = true
		
		override def compareTo(o: VerificationStatus): Int = if (o == Verified) 0 else 1
	}
	case object Deverified extends VerificationStatus
	{
		override val key: String = "deverified"
		override val isVerified: Boolean = false
		
		override def compareTo(o: VerificationStatus): Int = if (o == Deverified) 0 else -1
	}
}
