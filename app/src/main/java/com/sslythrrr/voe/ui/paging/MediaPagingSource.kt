package com.sslythrrr.voe.ui.paging // Sesuaikan package-nya ya

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sslythrrr.voe.ui.media.Media

class StaticListPagingSource(private val list: List<Media>) : PagingSource<Int, Media>() {
    override fun getRefreshKey(state: PagingState<Int, Media>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Media> {
        return LoadResult.Page(
            data = list,
            prevKey = null,
            nextKey = null
        )
    }
}