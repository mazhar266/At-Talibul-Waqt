package fi.mazhar.prayer.adhan.data

import fi.mazhar.prayer.adhan.model.DefaultDhikrLibrary
import fi.mazhar.prayer.adhan.model.Dhikr
import fi.mazhar.prayer.adhan.model.DhikrCategory
import fi.mazhar.prayer.adhan.model.PrayerSlot
import fi.mazhar.prayer.adhan.model.progressFor
import fi.mazhar.prayer.adhan.model.routineFor
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class AppRepositoryTest {

    private class FixedClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private val clock = FixedClock(Instant.parse("2026-10-16T10:00:00Z"))
    private val store = InMemoryKeyValueStore()
    private fun repository() = AppRepository(store, clock) { TimeZone.UTC }

    @Test
    fun prayersWithoutTheirOwnListInheritTheSharedRoutine() {
        val repo = repository()
        val shared = repo.playlist.value.routineFor(PrayerSlot.ALL)
        assertEquals(shared, repo.playlist.value.routineFor(PrayerSlot.DHUHR))
        assertEquals(7, shared.size)
        assertEquals(10, repo.playlist.value.routineFor(PrayerSlot.FAJR).size)
    }

    @Test
    fun editingAnInheritedRoutineForksItForThatPrayerOnly() {
        val repo = repository()
        repo.addToRoutine(PrayerSlot.ASR, "salawat-prophet")
        val asr = repo.playlist.value.routineFor(PrayerSlot.ASR)
        assertEquals("salawat-prophet", asr.last().dhikrId)
        assertEquals(10, asr.last().count)
        assertEquals(7, repo.playlist.value.routineFor(PrayerSlot.DHUHR).size)
        assertEquals(7, repo.playlist.value.routineFor(PrayerSlot.ALL).size)
    }

    @Test
    fun routineEditsReorderRecountAndRemove() {
        val repo = repository()
        repo.moveInRoutine(PrayerSlot.ALL, fromIndex = 0, toIndex = 2)
        assertEquals("post-istighfar", repo.playlist.value.routineFor(PrayerSlot.ALL)[2].dhikrId)
        repo.setRoutineCount(PrayerSlot.ALL, "post-subhanallah", 100)
        assertEquals(100, repo.playlist.value.routineFor(PrayerSlot.ALL).first { it.dhikrId == "post-subhanallah" }.count)
        repo.removeFromRoutine(PrayerSlot.ALL, "post-salam")
        assertFalse(repo.playlist.value.routineFor(PrayerSlot.ALL).any { it.dhikrId == "post-salam" })
    }

    @Test
    fun stateSurvivesARestart() {
        repository().apply {
            addCustomDhikr(Dhikr("custom-1", "My dhikr", "ذكر", "dhikr", "remembrance", category = DhikrCategory.DAILY))
            addToRoutine(PrayerSlot.ISHA, "custom-1", count = 7)
            recordTap("custom-1", PrayerSlot.ISHA, amount = 5)
        }
        val reloaded = repository()
        assertTrue(reloaded.library.value.any { it.id == "custom-1" && it.isCustom })
        assertEquals(7, reloaded.playlist.value.routineFor(PrayerSlot.ISHA).last().count)
        assertEquals(5, reloaded.todayRecord().dhikrCounts["custom-1"])
    }

    @Test
    fun deletingACustomDhikrAlsoRemovesItFromRoutines() {
        val repo = repository()
        repo.addCustomDhikr(Dhikr("custom-2", "Temp", "x", "x", "x"))
        repo.addToRoutine(PrayerSlot.ALL, "custom-2")
        repo.deleteCustomDhikr("custom-2")
        assertFalse(repo.library.value.any { it.id == "custom-2" })
        assertFalse(repo.playlist.value.routineFor(PrayerSlot.ALL).any { it.dhikrId == "custom-2" })
    }

    @Test
    fun sessionProgressCapsAtTargetsAndCompletes() {
        val repo = repository()
        val routine = repo.playlist.value.routineFor(PrayerSlot.DHUHR)
        repo.recordTap("post-istighfar", PrayerSlot.DHUHR, amount = 5) // target 3
        repo.recordTap("post-subhanallah", PrayerSlot.DHUHR, amount = 11) // target 33

        val progress = repo.todayRecord().progressFor(PrayerSlot.DHUHR, routine, repo.library.value)
        assertEquals(3 + 11, progress.done)
        assertEquals(routine.sumOf { it.count }, progress.target)
        assertFalse(progress.isComplete)
        assertEquals(16, repo.todayRecord().totalRecitations)

        routine.forEach { repo.recordTap(it.dhikrId, PrayerSlot.DHUHR, amount = it.count) }
        assertTrue(repo.todayRecord().progressFor(PrayerSlot.DHUHR, routine, repo.library.value).isComplete)
    }

    @Test
    fun streakCountsConsecutiveDaysWithRecitations() {
        val repo = repository()
        clock.now = Instant.parse("2026-10-14T10:00:00Z")
        repo.recordTap("post-subhanallah", null)
        clock.now = Instant.parse("2026-10-15T10:00:00Z")
        repo.recordTap("post-subhanallah", null)
        clock.now = Instant.parse("2026-10-16T10:00:00Z")
        assertEquals(2, repo.streakDays(), "yesterday's streak still counts before today's first tap")
        repo.recordTap("post-subhanallah", null)
        assertEquals(3, repo.streakDays())
    }

    @Test
    fun defaultLibraryIdsAreUniqueAndPlaylistReferencesResolve() {
        val ids = DefaultDhikrLibrary.items.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        DefaultDhikrLibrary.defaultPlaylist.values.flatten().forEach { entry ->
            assertTrue(entry.dhikrId in ids, "${entry.dhikrId} missing from library")
        }
    }
}
