package com.bdbshs.crest.ui.certificates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.bdbshs.crest.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

internal data class ResearchCertificateData(
    val groupName: String,
    val strand: String,
    val researchTitle: String,
    val memberNames: List<String>,
    val groupReference: String,
    val preparedOn: LocalDate = LocalDate.now(ZoneId.of("Asia/Manila"))
) {
    init {
        require(groupName.isNotBlank() && researchTitle.isNotBlank())
        require(memberNames.isNotEmpty() && memberNames.none { it.isBlank() })
    }
}

/** A4 landscape at 300 dpi. Document typography is independent of phone theme and font scale. */
internal object ResearchCertificateRenderer {
    const val WIDTH = 3508
    const val HEIGHT = 2480
    private const val PAGE_WIDTH = 1123f
    private const val PAGE_HEIGHT = 794f
    private val ink = Color.rgb(24, 40, 65)
    private val gold = Color.rgb(157, 124, 63)
    private val muted = Color.rgb(83, 89, 98)
    private val serif = Typeface.create("serif", Typeface.NORMAL)
    private val serifBold = Typeface.create("serif", Typeface.BOLD)
    private val serifItalic = Typeface.create("serif", Typeface.ITALIC)
    private val sans = Typeface.create("sans-serif", Typeface.NORMAL)
    private val sansBold = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val whitespace = Regex("\\s+")

    fun render(context: Context, data: ResearchCertificateData): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(WIDTH / PAGE_WIDTH, HEIGHT / PAGE_HEIGHT)
            canvas.drawColor(Color.WHITE)
            drawBorder(canvas)

            val seal = requireNotNull(BitmapFactory.decodeResource(
                context.resources, R.drawable.bdbshs_school_logo,
                BitmapFactory.Options().apply { inScaled = false }
            ))
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(seal, null, RectF(121f, 87f, 219f, 185f), paint)
                paint.alpha = 10
                canvas.drawBitmap(seal, null, RectF(414f, 308f, 709f, 603f), paint)
            } finally {
                seal.recycle()
            }

            text(canvas, "Republic of the Philippines", RectF(244f, 86f, 983f, 108f), 15f, serif)
            text(canvas, "Department of Education", RectF(244f, 109f, 983f, 133f), 17f, serifBold)
            text(canvas, "BONIFACIO D. BOREBOR SR. HIGH SCHOOL", RectF(244f, 138f, 983f, 164f), 22f, serifBold)
            text(canvas, "Tawog, Caramoan, Camarines Sur", RectF(244f, 168f, 983f, 187f), 13f, sans, muted)

            text(canvas, "SENIOR HIGH SCHOOL RESEARCH", RectF(160f, 205f, 963f, 224f), 11f, sansBold, gold, tracking = .18f)
            text(canvas, "Certificate of Completion", RectF(108f, 229f, 1015f, 283f), 47f, serifBold)
            line(canvas, 448f, 294f, 544f, 294f, gold, .8f)
            line(canvas, 579f, 294f, 675f, 294f, gold, .8f)
            diamond(canvas, PAGE_WIDTH / 2f, 294f, 3.5f)
            text(canvas, "This certifies that", RectF(160f, 309f, 963f, 332f), 16f, serif, muted)

            drawMembers(canvas, data.memberNames)
            val group = listOf(data.groupName.clean(), data.strand.clean()).filter { it.isNotEmpty() }.joinToString("  ·  ")
            text(canvas, group, RectF(155f, 445f, 968f, 478f), 16f, sansBold, minimumSize = 12f)
            val completion = if (data.memberNames.size == 1) "has completed" else "have completed"
            text(canvas, "$completion and submitted the research manuscript entitled", RectF(155f, 486f, 968f, 510f), 16f, serif)
            text(canvas, data.researchTitle, RectF(151f, 518f, 972f, 578f), 23f, serifItalic, minimumSize = 13f)
            text(canvas, "Reviewed and approved for the school research library.", RectF(155f, 586f, 968f, 607f), 13.5f, serif, muted)
            val date = data.preparedOn.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
            text(canvas, "Prepared on $date", RectF(155f, 614f, 968f, 634f), 12f, sans, muted)

            // Blank signing spaces deliberately carry no invented names or signatures.
            signature(canvas, 235f, 476f, "Research Adviser")
            signature(canvas, 647f, 888f, "School Head")
            line(canvas, 97f, 714f, 1026f, 714f, gold, .6f)
            text(canvas, "CREST  ·  School research library", RectF(99f, 720f, 1024f, 734f), 9f, sans, muted)
            val reference = data.groupReference.clean().takeIf { it.isNotEmpty() }?.let { "Group reference: $it  ·  " }.orEmpty()
            text(canvas, "${reference}Valid upon signature of the authorized school officials.", RectF(99f, 735f, 1024f, 747f), 8.5f, sans, muted, minimumSize = 7f)
            return bitmap
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    private fun drawBorder(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        paint.color = ink
        paint.strokeWidth = 3f
        canvas.drawRect(27f, 27f, PAGE_WIDTH - 27f, PAGE_HEIGHT - 27f, paint)
        paint.color = gold
        paint.strokeWidth = 1f
        canvas.drawRect(36f, 36f, PAGE_WIDTH - 36f, PAGE_HEIGHT - 36f, paint)
        paint.color = ink
        paint.strokeWidth = .5f
        canvas.drawRect(42f, 42f, PAGE_WIDTH - 42f, PAGE_HEIGHT - 42f, paint)
        for (x in listOf(54f, PAGE_WIDTH - 54f)) {
            for (y in listOf(54f, PAGE_HEIGHT - 54f)) {
                val dx = if (x < PAGE_WIDTH / 2) 1f else -1f
                val dy = if (y < PAGE_HEIGHT / 2) 1f else -1f
                line(canvas, x, y, x + 35f * dx, y, gold, 2f)
                line(canvas, x, y, x, y + 35f * dy, gold, 2f)
                diamond(canvas, x + 8f * dx, y + 8f * dy, 2.3f)
            }
        }
    }

    private fun drawMembers(canvas: Canvas, names: List<String>) {
        val columns = when { names.size <= 4 -> 1; names.size <= 10 -> 2; else -> 3 }
        val rows = ceil(names.size.toFloat() / columns).toInt()
        val gap = 32f
        val width = (813f - gap * (columns - 1)) / columns
        val rowHeight = 100f / rows
        val fontSize = when { names.size <= 3 -> 25f; names.size <= 8 -> 22f; else -> 18f }
        names.forEachIndexed { index, name ->
            val row = index / columns
            val column = index % columns
            // Center an incomplete final row instead of leaving an empty column.
            val rowColumns = minOf(columns, names.size - row * columns)
            val rowWidth = rowColumns * width + (rowColumns - 1) * gap
            val left = (PAGE_WIDTH - rowWidth) / 2 + column * (width + gap)
            text(canvas, name, RectF(left, 337f + row * rowHeight, left + width, 337f + (row + 1) * rowHeight - 2f),
                fontSize, serifBold, minimumSize = 9f)
        }
    }

    private fun signature(canvas: Canvas, left: Float, right: Float, role: String) {
        line(canvas, left, 679f, right, 679f, ink, .7f)
        text(canvas, role, RectF(left, 687f, right, 706f), 12f, sansBold)
    }

    /** Fit all the text inside its area: names and titles are never ellipsized or cut off. */
    private fun text(
        canvas: Canvas, value: String, bounds: RectF, size: Float, font: Typeface,
        color: Int = ink, minimumSize: Float = size * .85f, tracking: Float = 0f
    ) {
        val content = value.clean()
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = font
            letterSpacing = tracking
        }
        var candidateSize = size
        while (candidateSize >= minimumSize) {
            paint.textSize = candidateSize
            val layout = StaticLayout.Builder.obtain(content, 0, content.length, paint, bounds.width().toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(2f, 1f)
                .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .build()
            if (layout.height <= bounds.height()) {
                canvas.save()
                canvas.translate(bounds.left, bounds.top + (bounds.height() - layout.height) / 2f)
                layout.draw(canvas)
                canvas.restore()
                return
            }
            candidateSize -= .5f
        }
        throw IllegalArgumentException("Certificate text exceeds the printable area")
    }

    private fun String.clean() = trim().replace(whitespace, " ")

    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float) {
        canvas.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = width
        })
    }

    private fun diamond(canvas: Canvas, x: Float, y: Float, radius: Float) {
        canvas.drawPath(Path().apply {
            moveTo(x, y - radius)
            lineTo(x + radius, y)
            lineTo(x, y + radius)
            lineTo(x - radius, y)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold })
    }
}
