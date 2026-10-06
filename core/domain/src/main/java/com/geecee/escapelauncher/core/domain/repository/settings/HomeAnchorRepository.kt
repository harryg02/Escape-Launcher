package com.geecee.escapelauncher.core.domain.repository.settings

import com.geecee.escapelauncher.core.model.HomeAnchor
import kotlinx.coroutines.flow.Flow

interface HomeAnchorRepository {
    val homeAnchor: Flow<HomeAnchor>
    suspend fun setHomeAnchor(anchor: HomeAnchor)
    suspend fun clearHomeAnchor()
}
