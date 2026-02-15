package com.vlife.cv.interest.helper

import com.vlife.common.util.DateUtils
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 利息計算輔助工具
 *
 * 提供年天數、月數計算等利息計算所需的基礎運算。
 *
 * V3 對應：
 * - pk_sub_period.Period
 * - pk_sub_addyear.AddYear
 * - pk_sub_addmonth.AddMonth
 * - pk_sub_subym.sub_ym
 */
@Component
class InterestCalcHelper {

    /**
     * 計算年天數（考慮閏年）
     *
     * V3 對應：
     * ```
     * p_leap_date := pk_sub_addyear.AddYear(begin_date, 1)
     * p_y := pk_sub_period.Period(begin_date, p_leap_date)
     * ```
     *
     * @param beginDate 起算日
     * @return 年天數（365 或 366）
     */
    fun calculateYearDays(beginDate: LocalDate): Int {
        val oneYearLater = DateUtils.addYears(beginDate, 1) ?: beginDate
        return DateUtils.daysBetween(beginDate, oneYearLater).toInt()
    }

    /**
     * 計算兩個日期之間的天數
     *
     * V3 對應：pk_sub_period.Period(beginDate, endDate)
     *
     * @param beginDate 起始日
     * @param endDate 結束日
     * @return 天數（包含起始日，不含結束日）
     */
    fun calculateDays(beginDate: LocalDate, endDate: LocalDate): Int {
        return DateUtils.daysBetween(beginDate, endDate).toInt()
    }

    /**
     * 計算兩個日期之間的月數
     *
     * V3 對應：pk_sub_subym.sub_ym(beginYm, endYm)
     *
     * @param beginDate 起始日
     * @param endDate 結束日
     * @return 月數
     */
    fun calculateMonths(beginDate: LocalDate, endDate: LocalDate): Int {
        return DateUtils.monthsBetween(beginDate, endDate).toInt()
    }

    /**
     * 建立月份標示字串
     *
     * V3 對應：rec_iri_array.month (format: YYYY/MM)
     *
     * @param date 日期
     * @return 月份標示（如 "2025/01"）
     */
    fun formatMonth(date: LocalDate): String {
        return String.format("%04d/%02d", date.year, date.monthValue)
    }

    /**
     * 將日期調整至月初
     *
     * @param date 原始日期
     * @return 該月份的第一天
     */
    fun toMonthStart(date: LocalDate): LocalDate {
        return date.withDayOfMonth(1)
    }

    /**
     * 將日期調整至月底
     *
     * @param date 原始日期
     * @return 該月份的最後一天
     */
    fun toMonthEnd(date: LocalDate): LocalDate {
        return date.withDayOfMonth(date.lengthOfMonth())
    }
}
