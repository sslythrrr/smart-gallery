package com.sslythrrr.galeri.ui.paging

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaType
import com.sslythrrr.galeri.viewmodel.MediaViewModel

class MediaPagingSource(
    private val context: Context,
    private val bucketId: Long? = null
) : PagingSource<Int, Media>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Media> {
        return try {
            val page = params.key ?: 0

            val mediaList = mutableListOf<Media>()

            loadImagesPage(mediaList)
            loadVideosPage(mediaList)

            val sortedMedia = mediaList.sortedByDescending { it.dateTaken }

            LoadResult.Page(
                data = sortedMedia,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (sortedMedia.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Media>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    private fun loadImagesPage(mediaList: MutableList<Media>) {
        val imageProjection = MediaViewModel.IMAGE_PROJECTION

        val selection = if (bucketId != null) {
            "${MediaStore.Images.Media.BUCKET_ID} = ?"
        } else null

        val selectionArgs = if (bucketId != null) {
            arrayOf(bucketId.toString())
        } else null

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val title =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        ?: ""
                val dateTaken =
                    if (cursor.isNull(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))) {
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000
                    } else {
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                    }
                val bucketId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                val bucketName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                        ?: "Unknown Album"
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val relativePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                        ?: ""

                mediaList.add(
                    Media(
                        id = id,
                        title = title,
                        uri = contentUri,
                        type = MediaType.IMAGE,
                        albumId = bucketId,
                        albumName = bucketName,
                        dateTaken = dateTaken,
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
                        size = size,
                        relativePath = relativePath
                    )
                )
            }
        }
    }

    private fun loadVideosPage(mediaList: MutableList<Media>) {
        val videoProjection = MediaViewModel.VIDEO_PROJECTION

        val selection = if (bucketId != null) {
            "${MediaStore.Video.Media.BUCKET_ID} = ?"
        } else null

        val selectionArgs = if (bucketId != null) {
            arrayOf(bucketId.toString())
        } else null

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val title =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        ?: ""
                val dateTaken =
                    if (cursor.isNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))) {
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000
                    } else {
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                    }
                val bucketId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val bucketName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                        ?: "Unknown Album"
                val duration =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val relativePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH))
                        ?: ""

                mediaList.add(
                    Media(
                        id = id,
                        title = title,
                        uri = contentUri,
                        type = MediaType.VIDEO,
                        albumId = bucketId,
                        albumName = bucketName,
                        dateTaken = dateTaken,
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)),
                        duration = duration,
                        size = size,
                        relativePath = relativePath
                    )
                )
            }
        }
    }

}
