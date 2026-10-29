package ir.alifaniani.waymate.domain.model

/**
 * Link health and connection status of an operational field unit.
 * Defined according to REQ-F-014 and MVP-10.
 */
enum class ConnectionStatus(val titlePersian: String) {
    ONLINE("برخط"),
    STALE("هشدار تأخیر"),
    OFFLINE("قطع ارتباط")
}
