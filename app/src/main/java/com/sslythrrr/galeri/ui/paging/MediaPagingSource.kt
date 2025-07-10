package com.sslythrrr.galeri.ui.paging


import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.repository.TrashRepository
import com.sslythrrr.galeri.data.entity.ScannedImage
import com.sslythrrr.galeri.ui.media.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// CATATAN: PagingSource ini sekarang mungkin tidak lagi digunakan
// setelah kita memindahkan logika ke Room Paging.
// Anda bisa menghapus file ini jika tidak ada referensi lagi.
// Namun untuk sementara, kita biarkan saja.
class MediaPagingSource(
    private val context: Context,
    private val bucketId: Long? = null,
    private val trashRepository: TrashRepository
) : PagingSource<Int, Media>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Media> {
        // Implementasi ini sekarang menjadi usang karena kita akan menggunakan
        // PagingSource langsung dari Room.
        return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
    }

    override fun getRefreshKey(state: PagingState<Int, Media>): Int? {
        return null
    }
}
