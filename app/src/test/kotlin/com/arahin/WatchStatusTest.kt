package com.arahin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchStatusTest {
    @Test fun `only a found watch counts as linked`() {
        assertTrue(LinkState.Ready("Watch Fit 4").linked)
        assertTrue(LinkState.Sent("Watch Fit 4").linked)
        listOf(LinkState.Unknown, LinkState.NoPermission, LinkState.NoWatch, LinkState.Failed(201, "kirim"))
            .forEach { assertFalse(it.toString(), it.linked) }
    }

    @Test fun `labels name the watch`() {
        assertEquals("Terkirim ke Watch Fit 4.", LinkState.Sent("Watch Fit 4").label())
        assertTrue(LinkState.Ready("Watch Fit 4").label().contains("Watch Fit 4"))
    }

    @Test fun `known Wear Engine codes get a plain reason and keep the code`() {
        assertEquals("App Arahin di jam belum dibuka (kode 201, kirim).", LinkState.Failed(201, "kirim").label())
        assertEquals("App Arahin belum terpasang di jam (kode 200, kirim).", LinkState.Failed(200, "kirim").label())
        assertEquals("Jam tidak tersambung (kode 16, cari jam).", LinkState.Failed(16, "cari jam").label())
    }

    @Test fun `unknown codes still show the code`() {
        assertEquals("Gagal (kode 999, cek izin).", LinkState.Failed(999, "cek izin").label())
    }
}
