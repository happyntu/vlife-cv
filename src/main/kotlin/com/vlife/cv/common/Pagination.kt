package com.vlife.cv.common

import com.github.pagehelper.PageInfo

/**
 * 通用分頁請求
 *
 * 依據 ADR-015 規範，提供標準化的分頁參數。
 *
 * @property pageNum 頁碼 (從 1 開始)
 * @property pageSize 每頁筆數 (1-100)
 */
data class PageRequest(
    val pageNum: Int = 1,
    val pageSize: Int = 20
) {
    init {
        require(pageNum >= 1) { "pageNum must be >= 1" }
        require(pageSize in 1..100) { "pageSize must be 1-100" }
    }

    companion object {
        val DEFAULT = PageRequest()
    }
}

/**
 * 通用分頁回應
 *
 * 封裝 PageHelper 的 PageInfo，提供統一的回應格式。
 *
 * @property content 資料內容
 * @property pageNum 當前頁碼
 * @property pageSize 每頁筆數
 * @property total 總筆數
 * @property pages 總頁數
 * @property hasPrevious 是否有上一頁
 * @property hasNext 是否有下一頁
 */
data class PageResponse<T>(
    val content: List<T>,
    val pageNum: Int,
    val pageSize: Int,
    val total: Long,
    val pages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean
) {
    companion object {
        /**
         * 從 PageHelper 的 PageInfo 轉換
         */
        fun <T> from(pageInfo: PageInfo<T>): PageResponse<T> = PageResponse(
            content = pageInfo.list,
            pageNum = pageInfo.pageNum,
            pageSize = pageInfo.pageSize,
            total = pageInfo.total,
            pages = pageInfo.pages,
            hasPrevious = pageInfo.isHasPreviousPage,
            hasNext = pageInfo.isHasNextPage
        )

        /**
         * 轉換內容類型
         */
        fun <T, R> from(pageInfo: PageInfo<T>, mapper: (T) -> R): PageResponse<R> = PageResponse(
            content = pageInfo.list.map(mapper),
            pageNum = pageInfo.pageNum,
            pageSize = pageInfo.pageSize,
            total = pageInfo.total,
            pages = pageInfo.pages,
            hasPrevious = pageInfo.isHasPreviousPage,
            hasNext = pageInfo.isHasNextPage
        )
    }
}
