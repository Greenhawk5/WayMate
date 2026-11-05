package ir.alifaniani.waymate.domain.model

import ir.alifaniani.waymate.ui.home.map.GeoPoint

/**
 * Technical difficulty rating for hiking trails according to standard outdoor trail classifications.
 */
enum class HikingDifficulty(val displayNamePersian: String) {
    EASY("ساده و هموار"),
    MODERATE("متوسط و کوهپایه‌ای"),
    DIFFICULT("دشوار و فنی")
}

/**
 * Functional category of a waypoint positioned along a hiking route.
 */
enum class WaypointType(val displayNamePersian: String) {
    TRAILHEAD("مبدأ پاکوب"),
    VIEWPOINT("دیدگاه و چشم‌انداز"),
    REST_SITE("استراحت‌گاه"),
    WATER_SPRING("چشمه آب طبیعی"),
    SUMMIT("قله و خط‌الرأس")
}

/**
 * Intermediate geographic waypoint on a hiking trail.
 */
data class HikingWaypoint(
    val id: String,
    val routeId: String,
    val title: String,
    val description: String,
    val type: WaypointType,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int,
    val orderIndex: Int
) {
    val formattedCoordinates: String
        get() = String.format("%.4f°N, %.4f°E", latitude, longitude)
}

/**
 * Representation of a designated hiking or outdoor trail route.
 */
data class HikingRoute(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: HikingDifficulty,
    val lengthKm: Double,
    val elevationGainMeters: Int,
    val estimatedTimeMinutes: Int,
    val coordinates: List<GeoPoint>,
    val waypoints: List<HikingWaypoint> = emptyList(),
    val isSynthetic: Boolean = true
) {
    val formattedDuration: String
        get() {
            val hours = estimatedTimeMinutes / 60
            val mins = estimatedTimeMinutes % 60
            return if (hours > 0) "$hours ساعت و $mins دقیقه" else "$mins دقیقه"
        }
}

/**
 * Audited synthetic hiking demonstration routes located within the Jahrom geographical boundary.
 * Strictly non-operational and used for educational demonstration and simulation.
 */
val defaultJahromHikingRoutes: List<HikingRoute> = listOf(
    HikingRoute(
        id = "route_qalat_trail",
        name = "مسیر قله و کوهستان قلات جهرم",
        description = "مسیر پاکوب کوهنوردی دامنه‌های قلات با عبور از چشمه‌های طبیعی و چشم‌انداز نخلستان‌ها",
        difficulty = HikingDifficulty.MODERATE,
        lengthKm = 4.8,
        elevationGainMeters = 420,
        estimatedTimeMinutes = 150,
        coordinates = listOf(
            GeoPoint(53.5650, 28.5020),
            GeoPoint(53.5710, 28.5065),
            GeoPoint(53.5780, 28.5110),
            GeoPoint(53.5830, 28.5120),
            GeoPoint(53.5890, 28.5160),
            GeoPoint(53.5940, 28.5210),
            GeoPoint(53.6010, 28.5280),
            GeoPoint(53.6060, 28.5320)
        ),
        waypoints = listOf(
            HikingWaypoint(
                id = "wp_qalat_01",
                routeId = "route_qalat_trail",
                title = "مبدأ پاکوب دره قلات",
                description = "محل شروع صعود و بررسی آخرین هماهنگی‌های گروهی",
                type = WaypointType.TRAILHEAD,
                latitude = 28.5020,
                longitude = 53.5650,
                elevationMeters = 1080,
                orderIndex = 0
            ),
            HikingWaypoint(
                id = "wp_qalat_02",
                routeId = "route_qalat_trail",
                title = "چشمه قلات",
                description = "منبع آب طبیعی و توقفگاه مناسب استراحت نخست",
                type = WaypointType.WATER_SPRING,
                latitude = 28.5120,
                longitude = 53.5830,
                elevationMeters = 1240,
                orderIndex = 1
            ),
            HikingWaypoint(
                id = "wp_qalat_03",
                routeId = "route_qalat_trail",
                title = "چشم‌انداز صخره‌ای",
                description = "دید گسترده به دره جنوبی و نخلستان‌های پایین‌دست",
                type = WaypointType.VIEWPOINT,
                latitude = 28.5210,
                longitude = 53.5940,
                elevationMeters = 1390,
                orderIndex = 2
            ),
            HikingWaypoint(
                id = "wp_qalat_04",
                routeId = "route_qalat_trail",
                title = "قله قلات",
                description = "مقصد پایانی مسیر و نقطه مرتفع خط‌الرأس",
                type = WaypointType.SUMMIT,
                latitude = 28.5320,
                longitude = 53.6060,
                elevationMeters = 1500,
                orderIndex = 3
            )
        )
    ),
    HikingRoute(
        id = "route_tadovan_trail",
        name = "مسیر دره و تنگه تادوان جهرم",
        description = "مسیر پیاده‌روی طبیعی در حاشیه رودخانه با بافت نیمه‌کوهستانی و پوشش گیاهی",
        difficulty = HikingDifficulty.EASY,
        lengthKm = 3.2,
        elevationGainMeters = 140,
        estimatedTimeMinutes = 80,
        coordinates = listOf(
            GeoPoint(53.6100, 28.5380),
            GeoPoint(53.6160, 28.5420),
            GeoPoint(53.6210, 28.5450),
            GeoPoint(53.6280, 28.5490),
            GeoPoint(53.6350, 28.5520),
            GeoPoint(53.6420, 28.5560)
        ),
        waypoints = listOf(
            HikingWaypoint(
                id = "wp_tado_01",
                routeId = "route_tadovan_trail",
                title = "ورودی تنگه تادوان",
                description = "آغاز مسیر مسطح حاشیه رودخانه",
                type = WaypointType.TRAILHEAD,
                latitude = 28.5380,
                longitude = 53.6100,
                elevationMeters = 1010,
                orderIndex = 0
            ),
            HikingWaypoint(
                id = "wp_tado_02",
                routeId = "route_tadovan_trail",
                title = "سایه‌بان نخلستان",
                description = "محل استراحت میان‌مسیر و دسترسی به رودخانه",
                type = WaypointType.REST_SITE,
                latitude = 28.5450,
                longitude = 53.6210,
                elevationMeters = 1045,
                orderIndex = 1
            ),
            HikingWaypoint(
                id = "wp_tado_03",
                routeId = "route_tadovan_trail",
                title = "دهانه غار تادوان",
                description = "نقطه پایانی و جاذبه طبیعی دره",
                type = WaypointType.VIEWPOINT,
                latitude = 28.5560,
                longitude = 53.6420,
                elevationMeters = 1150,
                orderIndex = 2
            )
        )
    )
)
