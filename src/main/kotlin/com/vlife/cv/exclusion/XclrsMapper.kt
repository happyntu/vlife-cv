package com.vlife.cv.exclusion

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface XclrsMapper {

    fun findByXclrsSerial(@Param("xclrsSerial") xclrsSerial: Long): Xclrs?

    fun findByClaimReceNoAndClaimTypeAndXclrsCode(
        @Param("claimReceNo") claimReceNo: String,
        @Param("claimType") claimType: String,
        @Param("xclrsCode") xclrsCode: String
    ): Xclrs?

    fun findFirstByClaimReceNoAndClaimType(
        @Param("claimReceNo") claimReceNo: String,
        @Param("claimType") claimType: String
    ): Xclrs?

    fun findByClaimReceNo(@Param("claimReceNo") claimReceNo: String): List<Xclrs>

    fun findByEventId(@Param("eventId") eventId: String): List<Xclrs>

    fun insert(entity: Xclrs): Int

    fun update(entity: Xclrs): Int

    fun deleteByXclrsSerial(@Param("xclrsSerial") xclrsSerial: Long): Int

    fun deleteByClaimReceNo(@Param("claimReceNo") claimReceNo: String): Int
}
