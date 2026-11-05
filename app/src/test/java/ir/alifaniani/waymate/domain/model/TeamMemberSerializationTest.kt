package ir.alifaniani.waymate.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamMemberSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun teamMember_serializesAndDeserializesCorrectly() {
        val member = TeamMember(
            id = "mem_001",
            fullName = "رضا کریمی",
            callsign = "البرز ۱",
            roleInTeam = "سرتیم"
        )
        val list = listOf(member)

        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<List<TeamMember>>(encoded)

        assertEquals(1, decoded.size)
        assertEquals("mem_001", decoded[0].id)
        assertEquals("رضا کریمی", decoded[0].fullName)
        assertEquals("البرز ۱", decoded[0].callsign)
        assertEquals("سرتیم", decoded[0].roleInTeam)
    }
}

