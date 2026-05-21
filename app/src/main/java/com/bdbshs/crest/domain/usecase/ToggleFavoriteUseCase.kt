package com.bdbshs.crest.domain.usecase

import com.bdbshs.crest.data.repository.FavoritesRepository
import com.bdbshs.crest.ui.viewmodels.ResearchType
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    suspend operator fun invoke(
        uid: String,
        researchId: String,
        researchType: ResearchType,
        isCurrentlyFavorite: Boolean
    ): Result<Unit> {
        return try {
            favoritesRepository.setFavorite(
                uid = uid,
                researchId = researchId,
                researchType = researchType,
                isFavorite = !isCurrentlyFavorite
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
