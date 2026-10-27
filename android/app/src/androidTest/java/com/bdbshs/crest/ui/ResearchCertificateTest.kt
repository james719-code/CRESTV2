package com.bdbshs.crest.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bdbshs.crest.ui.certificates.ResearchCertificateData
import com.bdbshs.crest.ui.certificates.ResearchCertificateRenderer
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses fictional records only and never writes to the database or changes group membership. */
@RunWith(AndroidJUnit4::class)
class ResearchCertificateTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val example = ResearchCertificateData(
        groupName = "Sustainable Futures",
        strand = "STEM",
        researchTitle = "Student Perspectives on Sustainable Waste Management in Senior High School",
        memberNames = listOf("Andrea Mae Santos", "Miguel Antonio Reyes", "Sofia Isabelle Cruz"),
        groupReference = "GROUP-024",
        preparedOn = LocalDate.of(2026, 10, 27)
    )

    @Test fun rendersAPrintSizedSchoolCertificate() {
        val image = ResearchCertificateRenderer.render(context, example)
        try {
            assertEquals(3508, image.width)
            assertEquals(2480, image.height)
            assertEquals(Color.WHITE, image.getPixel(0, 0))
            val border = image.getPixel((27f / 1123f * image.width).toInt(), image.height / 2)
            assertTrue("The formal border must be rendered", Color.red(border) < 100)
            save(image, "certificate-school")
        } finally {
            image.recycle()
        }
    }

    @Test fun fitsLongNamesAndResearchTitlesWithoutTruncation() {
        val image = ResearchCertificateRenderer.render(context, example.copy(
            groupName = "Young Researchers for Environmental Stewardship and Community Development",
            researchTitle = "A Comparative Investigation of Sustainable Solid Waste Management Practices, Environmental Awareness, and Community Participation among Senior High School Learners in Coastal Communities of Caramoan, Camarines Sur",
            memberNames = listOf(
                "Maria Alexandra de los Santos", "Juan Miguel Villanueva-Reyes",
                "Gabrielle Antoinette dela Cruz", "Jose Antonio Fernandez-Bautista",
                "Sophia Ysabelle Macapagal", "Christian Emmanuel San Agustin",
                "Angela Dominique Villafuerte", "Rafael Lorenzo Santiago III",
                "Isabella Francesca Buenaventura", "Gabriel Sebastian Montenegro",
                "Bianca Patricia de la Concepcion", "Luis Alejandro Evangelista",
                "Camille Therese Rodriguez", "Nathaniel Joaquin Manansala",
                "Ariana Beatrice Constantino", "Paolo Benedict Alcantara"
            )
        ))
        try {
            // The renderer rejects overflow instead of silently clipping any name or title.
            assertEquals(ResearchCertificateRenderer.HEIGHT, image.height)
            save(image, "certificate-long-roster")
        } finally {
            image.recycle()
        }
    }

    private fun save(image: Bitmap, name: String) {
        val directory = File(context.getExternalFilesDir(null), "certificate-review")
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "$name.png").outputStream().use {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
