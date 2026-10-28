package com.arahin

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.util.zip.Inflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Fixtures in src/test/resources/maneuver are large icons saved from real Google Maps navigation notifications.
class ManeuverIconTest {
    // alpha plane of an image; the fixtures are 8-bit RGBA PNGs as Android's Bitmap.compress writes them
    private class Alpha(val w: Int, val h: Int, val a: IntArray)

    private fun load(name: String): Alpha {
        val input = DataInputStream(javaClass.getResourceAsStream("/maneuver/$name.png")!!)
        input.skipBytes(8)
        var w = 0
        var h = 0
        val idat = ByteArrayOutputStream()
        while (true) {
            val len = input.readInt()
            val type = String(ByteArray(4).also { input.readFully(it) })
            val data = ByteArray(len).also { input.readFully(it) }
            input.readInt()
            when (type) {
                "IHDR" -> {
                    w = java.nio.ByteBuffer.wrap(data).int
                    h = java.nio.ByteBuffer.wrap(data, 4, 4).int
                    check(data[8].toInt() == 8 && data[9].toInt() == 6) { "$name: not 8-bit RGBA" }
                }
                "IDAT" -> idat.write(data)
                "IEND" -> break
            }
        }
        val stride = w * 4
        val raw = ByteArray((stride + 1) * h)
        Inflater().apply { setInput(idat.toByteArray()); inflate(raw); end() }
        val px = ByteArray(stride * h)
        for (y in 0 until h) {
            val f = raw[y * (stride + 1)].toInt()
            for (i in 0 until stride) {
                val x = raw[y * (stride + 1) + 1 + i].toInt() and 0xff
                val l = if (i >= 4) px[y * stride + i - 4].toInt() and 0xff else 0
                val u = if (y > 0) px[(y - 1) * stride + i].toInt() and 0xff else 0
                val ul = if (i >= 4 && y > 0) px[(y - 1) * stride + i - 4].toInt() and 0xff else 0
                val v = when (f) {
                    1 -> x + l
                    2 -> x + u
                    3 -> x + (l + u) / 2
                    4 -> {
                        val p = l + u - ul
                        val pa = Math.abs(p - l); val pb = Math.abs(p - u); val pc = Math.abs(p - ul)
                        x + if (pa <= pb && pa <= pc) l else if (pb <= pc) u else ul
                    }
                    else -> x
                }
                px[y * stride + i] = v.toByte()
            }
        }
        return Alpha(w, h, IntArray(w * h) { px[it * 4 + 3].toInt() and 0xff })
    }

    private fun match(img: Alpha) = matchIcon(iconSignature(img.a, img.w, img.h))

    // bilinear resize, standing in for another phone's screen density
    private fun scaled(img: Alpha, size: Int) = Alpha(size, size, IntArray(size * size) { i ->
        val fx = ((i % size) + 0.5) * img.w / size - 0.5
        val fy = ((i / size) + 0.5) * img.h / size - 0.5
        val x0 = fx.toInt().coerceIn(0, img.w - 2)
        val y0 = fy.toInt().coerceIn(0, img.h - 2)
        val dx = (fx - x0).coerceIn(0.0, 1.0)
        val dy = (fy - y0).coerceIn(0.0, 1.0)
        fun at(x: Int, y: Int) = img.a[y * img.w + x]
        (at(x0, y0) * (1 - dx) * (1 - dy) + at(x0 + 1, y0) * dx * (1 - dy) + at(x0, y0 + 1) * (1 - dx) * dy + at(x0 + 1, y0 + 1) * dx * dy).toInt()
    })

    private fun mirrored(img: Alpha) = Alpha(img.w, img.h, IntArray(img.a.size) { img.a[it / img.w * img.w + img.w - 1 - it % img.w] })

    private val expected = mapOf(
        "destination" to IconMatch('D', null), "destination-road" to IconMatch('D', null), "destination-road-left" to IconMatch('D', null),
        "depart" to IconMatch('S', null), "straight" to IconMatch('S', null), "straight-past-branch" to IconMatch('S', null),
        "left" to IconMatch('L', null), "right" to IconMatch('R', null),
        "slight-left" to IconMatch('l', null), "slight-right" to IconMatch('r', null), "keep-left" to IconMatch('l', null), "keep-right" to IconMatch('r', null),
        "sharp-right" to IconMatch('j', null), "u-turn" to IconMatch('U', null), "merge" to IconMatch('M', null),
        "roundabout-exit-1" to IconMatch('O', 1), "roundabout-exit-2" to IconMatch('O', 2),
        "roundabout-exit-3" to IconMatch('O', 3), "roundabout-exit-3-sharp" to IconMatch('O', 3),
    )
    private val mirror = mapOf('L' to 'R', 'R' to 'L', 'l' to 'r', 'r' to 'l', 'h' to 'j', 'j' to 'h')

    @Test fun realIcons() = expected.forEach { (name, m) -> assertEquals(name, m, match(load(name))) }

    // other phones render the icon at their own density
    @Test fun otherDensities() = expected.forEach { (name, m) ->
        for (size in listOf(84, 96, 168, 189)) assertEquals("$name@$size", m, match(scaled(load(name), size)))
    }

    @Test fun mirroredIcons() = expected.forEach { (name, m) ->
        assertEquals("$name mirrored", m.copy(dir = mirror[m.dir] ?: m.dir), match(mirrored(load(name))))
    }

    @Test fun blankIconIsUnknown() = assertNull(match(Alpha(126, 126, IntArray(126 * 126))))

    @Test fun turnFromRealNotification() = assertEquals(
        Turn('R', "180 m", 180, null, "Belok kanan"),
        notificationTurn("180 m", "Jl. Sutan Syahrir", IconMatch('R', null)),
    )

    @Test fun roundaboutFromIcon() = assertEquals(
        Turn('O', "650 m", 650, 3, "Keluar ke-3"),
        notificationTurn("650 m", "Jl. Medan Merdeka Sel.", IconMatch('O', 3)),
    )

    // the ETA line ("12 km") is not passed in, so an icon-less title without distance has none
    @Test fun streetNameFallback() = assertEquals(
        Turn('?', "40 m", 40, null, "Jl. K.H. Wahid Hasyim"),
        notificationTurn("40 m", "Jl. K.H. Wahid Hasyim", null),
    )

    @Test fun towardIsStraightWithoutIcon() = assertEquals('S', notificationTurn("0 m", "toward Jl. M.H. Thamrin", null)?.dir)
}
