package ir.alifaniani.waymate.ui.home.map

/**
 * Geometric vector point representing a geographical coordinate (WGS-84).
 */
data class GeoPoint(
    val longitude: Double,
    val latitude: Double
)

/**
 * Representation of an individual polyline segment audited from the GIS dataset.
 */
data class GisLineSegment(
    val id: Int,
    val osmId: String?,
    val name: String?,
    val roadType: String?,
    val isWaterway: Boolean,
    val points: List<GeoPoint>
)

/**
 * Bounding envelope for the audited Jahrom GIS dataset (EPSG:4326).
 */
data class GisBoundingBox(
    val minLongitude: Double = 53.4523,
    val minLatitude: Double = 28.4391,
    val maxLongitude: Double = 53.7494,
    val maxLatitude: Double = 28.6215
) {
    val centerLongitude: Double
        get() = (minLongitude + maxLongitude) / 2.0

    val centerLatitude: Double
        get() = (minLatitude + maxLatitude) / 2.0
}

/**
 * Contract for loading GIS vector features asynchronously without blocking the UI thread.
 */
interface GisLayerRepository {
    /**
     * Loads the complete set of audited Jahrom vector segments (roads + waterways).
     */
    suspend fun loadJahromVectors(): List<GisLineSegment>

    /**
     * Backward-compatible alias for loading vectors.
     */
    suspend fun loadJahromSampleVectors(): List<GisLineSegment> = loadJahromVectors()

    fun getBoundingBox(): GisBoundingBox
}
