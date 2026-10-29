package ir.alifaniani.waymate.ui.home.map

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.InputStreamReader

@Serializable
internal data class VectorFeatureDto(
    val id: Int = 0,
    val osm_id: String? = null,
    val name: String? = null,
    val road_type: String? = null,
    val is_waterway: Boolean = false,
    val points: List<List<Double>> = emptyList()
)

/**
 * Implementation of [GisLayerRepository] that loads audited Jahrom vector segments
 * from application assets or a provided stream asynchronously without blocking the UI thread.
 */
class AssetGisLayerRepository(
    private val context: Context? = null,
    private val inputStreamProvider: (() -> InputStream)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GisLayerRepository {

    private val boundingBox = GisBoundingBox(
        minLongitude = 53.4523,
        minLatitude = 28.4391,
        maxLongitude = 53.7494,
        maxLatitude = 28.6215
    )

    private var cachedSegments: List<GisLineSegment>? = null

    override suspend fun loadJahromVectors(): List<GisLineSegment> {
        cachedSegments?.let { return it }

        return withContext(ioDispatcher) {
            try {
                val inputStream: InputStream = when {
                    inputStreamProvider != null -> inputStreamProvider.invoke()
                    context != null -> {
                        val assetManager = context.assets
                        try {
                            assetManager.open("gis/jahrom/jahrom_vectors.json")
                        } catch (e: Exception) {
                            assetManager.open("gis/jahrom/jahrom_sample_vectors.json")
                        }
                    }
                    else -> throw IllegalStateException("Neither Context nor InputStreamProvider was supplied")
                }

                val segments = parseJsonStream(inputStream)
                cachedSegments = segments
                segments
            } catch (e: Exception) {
                // Return fallback minimal segments if assets missing or on unhandled error
                fallbackSampleSegments
            }
        }
    }

    override suspend fun loadJahromSampleVectors(): List<GisLineSegment> = loadJahromVectors()

    override fun getBoundingBox(): GisBoundingBox = boundingBox

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /**
         * Parses a JSON stream containing vector geometries into strongly-typed [GisLineSegment]s.
         * Pure Kotlin parser utilizing kotlinx.serialization, 100% testable on both Android and JVM.
         */
        fun parseJsonStream(inputStream: InputStream): List<GisLineSegment> {
            val jsonString = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
            if (jsonString.isBlank()) return emptyList()

            val dtoList = try {
                jsonParser.decodeFromString<List<VectorFeatureDto>>(jsonString)
            } catch (e: Exception) {
                return emptyList()
            }

            return dtoList.map { dto ->
                GisLineSegment(
                    id = dto.id,
                    osmId = dto.osm_id,
                    name = dto.name,
                    roadType = dto.road_type,
                    isWaterway = dto.is_waterway,
                    points = dto.points.mapNotNull { pt ->
                        if (pt.size >= 2) GeoPoint(pt[0], pt[1]) else null
                    }
                )
            }
        }
    }
}

/**
 * In-memory repository for unit testing and offline preview when context is unavailable.
 */
class FakeGisLayerRepository(
    private val testSegments: List<GisLineSegment> = fallbackSampleSegments
) : GisLayerRepository {
    override suspend fun loadJahromVectors(): List<GisLineSegment> = testSegments
    override suspend fun loadJahromSampleVectors(): List<GisLineSegment> = testSegments
    override fun getBoundingBox(): GisBoundingBox = GisBoundingBox()
}

/**
 * Fallback representative Jahrom road segments for instant display and testing.
 */
val fallbackSampleSegments: List<GisLineSegment> = listOf(
    GisLineSegment(
        id = 1,
        osmId = "1001",
        name = "کمربندی جهرم",
        roadType = "جاده اصلی",
        isWaterway = false,
        points = listOf(
            GeoPoint(53.5800, 28.5200),
            GeoPoint(53.6000, 28.5300),
            GeoPoint(53.6250, 28.5450)
        )
    ),
    GisLineSegment(
        id = 2,
        osmId = "1002",
        name = "بلوار مطهری",
        roadType = "خیابان اصلی",
        isWaterway = false,
        points = listOf(
            GeoPoint(53.5950, 28.5250),
            GeoPoint(53.6020, 28.5350),
            GeoPoint(53.6100, 28.5420)
        )
    ),
    GisLineSegment(
        id = 3,
        osmId = "1003",
        name = "رودخانه پلنگی",
        roadType = "رودخانه",
        isWaterway = true,
        points = listOf(
            GeoPoint(53.5850, 28.5150),
            GeoPoint(53.5980, 28.5280),
            GeoPoint(53.6150, 28.5380)
        )
    )
)
