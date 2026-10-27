package com.bdbshs.crest

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.CrestStorage
import com.bdbshs.crest.data.EdgeApi
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.repository.PdfRepository
import com.bdbshs.crest.data.repository.ResearchRepository
import com.bdbshs.crest.data.repository.WorkspaceDeletionRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Explicitly opted-in device checks. School records are only read, never changed. */
@RunWith(AndroidJUnit4::class)
class LivePhoneVerificationTest {
    @Before fun requireLiveVerification() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyLiveSchool") == "true")
        assertNotNull("Sign in on the phone before running this check.", FirebaseClient.auth.currentUser)
    }

    @Test fun groupMembershipLookupWorksWithDeployedIndexes() = runBlocking<Unit> {
        withTimeout(60_000) {
            val firestore = FirebaseClient.firestore
            val teacherId = FirebaseClient.auth.currentUser!!.uid
            assertTrue(firestore.collection(FirestorePaths.TEACHERS).document(teacherId)
                .get(Source.SERVER).await().getBoolean("access") == true)
            val groups = firestore.collection(FirestorePaths.GROUPS).get(Source.SERVER).await()
            val repository = WorkspaceDeletionRepository(firestore, FirebaseClient.auth)
            for (group in groups.documents) repository.findLinkedStudentIds(group.id)
            Log.i("CrestDeviceCheck", "Live membership lookup passed for ${groups.size()} groups with deployed indexes.")
        }
    }

    @Test fun researchMetadataAndPdfAreAvailableWithoutNetwork() = runBlocking<Unit> {
        withTimeout(90_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val firestore = FirebaseClient.firestore
            val preferences = context.getSharedPreferences("crest-device-cache-verification", Context.MODE_PRIVATE)
            val warm = InstrumentationRegistry.getArguments().getString("verificationPhase") != "cold"
            val storage = CrestStorage(AppwriteClient.storage, EdgeApi(FirebaseClient.auth))
            val repository = ResearchRepository(firestore, storage)
            val pdfRepository = PdfRepository(storage)
            if (warm) {
                val qualitative = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES).get(Source.SERVER).await()
                val quantitative = firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES).get(Source.SERVER).await()
                val fileId = (qualitative.documents + quantitative.documents)
                    .firstOrNull { !it.getString("file_link").isNullOrBlank() }?.getString("file_link")
                assertNotNull("A library PDF is needed to verify offline reading.", fileId)
                val file = pdfRepository.getOrDownloadPdfFile(context, fileId!!, true)
                assertTrue(file.length() > 0)
                preferences.edit().putInt("papers", qualitative.size() + quantitative.size())
                    .putString("fileId", fileId).commit()
            }
            assertTrue("Warm the cache once before the cold-start check.", preferences.contains("papers"))
            try {
                firestore.disableNetwork().await()
                val qualitative = repository.fetchCachedQualitativeResearches()
                val quantitative = repository.fetchCachedQuantitativeResearches()
                assertTrue(qualitative.metadata.isFromCache)
                assertTrue(quantitative.metadata.isFromCache)
                assertEquals(preferences.getInt("papers", -1), qualitative.size() + quantitative.size())
                val file = pdfRepository.getOrDownloadPdfFile(context, preferences.getString("fileId", null)!!, false)
                assertTrue(file.length() > 0)
                Log.i("CrestDeviceCheck", "${if (warm) "Warm" else "Cold-start"} offline read passed: ${qualitative.size() + quantitative.size()} paper records and a cached PDF.")
            } finally {
                withContext(NonCancellable) { firestore.enableNetwork().await() }
                if (!warm) preferences.edit().clear().commit()
            }
        }
    }
}
