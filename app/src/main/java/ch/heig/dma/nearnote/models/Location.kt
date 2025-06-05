package ch.heig.dma.nearnote.models

/**
 * A simple data class to hold latitude, longitude, and a display name for a location.
 *
 * @property lat The latitude of the location.
 * @property lng The longitude of the location.
 * @property name A human-readable name or address for the location.
 */
data class Location(val lat: Double, val lng: Double, val name: String)
