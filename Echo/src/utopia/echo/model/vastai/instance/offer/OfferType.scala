package utopia.echo.model.vastai.instance.offer

/**
 * An enumeration for different types of offers
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
sealed trait OfferType
{
	/**
	 * @return Key used for this offer type by Vast AI
	 */
	def key: String
}

object OfferType
{
	// VALUES   ------------------------
	
	/**
	 * Fixed pricing based on listed rates.
	 */
	case object OnDemand extends OfferType
	{
		override val key: String = "ondemand"
	}
	/**
	 * Uses minimum bid price. Lower cost but may be interrupted if outbid.
	 */
	case object Bid extends OfferType
	{
		override val key: String = "bid"
	}
	/**
	 * Reserved instances allow one to get significant discounts (up to 50%) by pre-paying for GPU time
	 * (usually months at a time).
	 */
	case object Reserved extends OfferType
	{
		override val key: String = "reserved"
	}
}
