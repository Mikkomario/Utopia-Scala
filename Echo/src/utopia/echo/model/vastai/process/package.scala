package utopia.echo.model.vastai

/**
 * @author Mikko Hilpinen
 * @since 11.10.2026, v1.6
 */
package object process
{
	@deprecated("Renamed to HostingResult", "v1.6")
	type ApiHostingResult = HostingResult
	
	@deprecated("Renamed to VastAiServiceState", "v1.6")
	type VastAiVllmProcessState = VastAiServiceState
}
