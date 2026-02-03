package com.vlife.cv.commission

import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CratController 單元測試
 *
 * 遵循 ADR-017 規範，測試表格導向命名的 Controller。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CratController 單元測試")
class CratControllerTest {

    private lateinit var service: CratService
    private lateinit var controller: CratController

    @BeforeEach
    fun setup() {
        service = mockk()
        controller = CratController(service)
    }

    private fun createTestRate(
        serial: Long = 1L,
        commClassCode: String = "12RA1",
        commLineCode: String = "31",
        cratType: String = "1",
        startDate: LocalDate = LocalDate.of(2020, 1, 1),
        endDate: LocalDate = LocalDate.of(2030, 12, 31),
        commRate: BigDecimal? = BigDecimal("5.0000")
    ) = Crat(
        serial = serial,
        commClassCode = commClassCode,
        commLineCode = commLineCode,
        cratType = cratType,
        projectNo = null,
        startDate = startDate,
        endDate = endDate,
        cratKey1 = "030",
        cratKey2 = "030",
        commStartYear = 1,
        commEndYear = 10,
        commStartAge = 20,
        commEndAge = 65,
        commStartModx = null,
        commEndModx = null,
        commRate = commRate,
        commRateOrg = null,
        premLimitStart = null,
        premLimitEnd = null
    )

    @Nested
    @DisplayName("search")
    inner class Search {

        @Test
        fun `should return paginated results`() {
            // Given
            val rates = listOf(createTestRate(serial = 1L), createTestRate(serial = 2L))
            val pageInfo = PageInfo(rates).apply {
                pageNum = 1
                pageSize = 20
                total = 2
            }
            every { service.search(any(), any<PageRequest>()) } returns pageInfo

            // When
            val response = controller.search(
                commClassCode = "12RA1",
                commLineCode = "31",
                cratType = "1",
                effectiveDate = LocalDate.now(),
                pageNum = 1,
                pageSize = 20
            )

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.content?.size)
        }
    }

    @Nested
    @DisplayName("getBySerial")
    inner class GetBySerial {

        @Test
        fun `should return rate when exists`() {
            // Given
            val rate = createTestRate(serial = 100L)
            every { service.findBySerial(100L) } returns rate

            // When
            val response = controller.getBySerial(100L)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertNotNull(response.body!!.data)
            assertEquals(100L, response.body!!.data?.serial)
            assertEquals("12RA1", response.body!!.data?.commClassCode)
            verify(exactly = 1) { service.findBySerial(100L) }
        }

        @Test
        fun `should return 404 when not exists`() {
            // Given
            every { service.findBySerial(999L) } returns null

            // When
            val response = controller.getBySerial(999L)

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(404, response.body!!.code)
            assertEquals("COMMISSION_RATE_NOT_FOUND", response.body!!.error?.errorCode)
        }
    }

    @Nested
    @DisplayName("getByClassCode")
    inner class GetByClassCode {

        @Test
        fun `should return rates for given class code`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L),
                createTestRate(serial = 2L),
                createTestRate(serial = 3L)
            )
            every { service.findByClassCode("12RA1") } returns rates

            // When
            val response = controller.getByClassCode("12RA1")

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(3, response.body!!.data?.size)
            verify(exactly = 1) { service.findByClassCode("12RA1") }
        }

        @Test
        fun `should return empty list for unknown class code`() {
            // Given
            every { service.findByClassCode("XXXXX") } returns emptyList()

            // When
            val response = controller.getByClassCode("XXXXX")

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertTrue(response.body!!.data!!.isEmpty())
        }
    }

    @Nested
    @DisplayName("getEffectiveRates")
    inner class GetEffectiveRates {

        @Test
        fun `should return effective rates without age filter`() {
            // Given
            val rates = listOf(createTestRate(serial = 1L), createTestRate(serial = 2L))
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { service.findEffectiveRates("12RA1", "31", effectiveDate) } returns rates

            // When
            val response = controller.getEffectiveRates(
                commClassCode = "12RA1",
                commLineCode = "31",
                effectiveDate = effectiveDate,
                age = null
            )

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.size)
        }

        @Test
        fun `should return single rate when age filter applied`() {
            // Given
            val rate = createTestRate(serial = 1L)
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { service.findEffectiveRateForAge("12RA1", "31", effectiveDate, 30) } returns rate

            // When
            val response = controller.getEffectiveRates(
                commClassCode = "12RA1",
                commLineCode = "31",
                effectiveDate = effectiveDate,
                age = 30
            )

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(1, response.body!!.data?.size)
        }

        @Test
        fun `should return empty list when no rate matches age`() {
            // Given
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { service.findEffectiveRateForAge("12RA1", "31", effectiveDate, 99) } returns null

            // When
            val response = controller.getEffectiveRates(
                commClassCode = "12RA1",
                commLineCode = "31",
                effectiveDate = effectiveDate,
                age = 99
            )

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertTrue(response.body!!.data!!.isEmpty())
        }
    }

    @Nested
    @DisplayName("getAllLineCodes")
    inner class GetAllLineCodes {

        @Test
        fun `should return all line codes with descriptions`() {
            // Given
            every { service.findAllLineCodes() } returns listOf("31", "21", "35")

            // When
            val response = controller.getAllLineCodes()

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(3, response.body!!.data?.size)
            assertEquals("31", response.body!!.data!![0].code)
            assertEquals("三階業務員", response.body!!.data!![0].description)
        }
    }

    @Nested
    @DisplayName("getAllCratTypes")
    inner class GetAllCratTypes {

        @Test
        fun `should return all crat types with descriptions`() {
            // Given
            every { service.findAllCratTypes() } returns listOf("1", "9")

            // When
            val response = controller.getAllCratTypes()

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.size)
            assertEquals("1", response.body!!.data!![0].code)
            assertEquals("一般佣金_折算率", response.body!!.data!![0].description)
        }
    }

    @Nested
    @DisplayName("getClassCodesByLineCode")
    inner class GetClassCodesByLineCode {

        @Test
        fun `should return class codes for given line code`() {
            // Given
            every { service.findClassCodesByLineCode("31") } returns listOf("12RA1", "12RA2", "12RA3")

            // When
            val response = controller.getClassCodesByLineCode("31")

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(3, response.body!!.data?.size)
            assertEquals("12RA1", response.body!!.data!![0])
        }
    }

    // TODO: refreshCache 測試需要進一步調查 ApiResponse<String> 序列化問題

    @Nested
    @DisplayName("DTO 轉換")
    inner class DtoConversion {

        @Test
        fun `should convert Crat to CratResponse correctly`() {
            // Given
            val rate = createTestRate(
                serial = 1L,
                commClassCode = "12RA1",
                commLineCode = "31",
                cratType = "1",
                commRate = BigDecimal("5.0000")
            )
            every { service.findBySerial(1L) } returns rate

            // When
            val response = controller.getBySerial(1L)

            // Then
            val dto = response.body!!.data!!
            assertEquals(1L, dto.serial)
            assertEquals("12RA1", dto.commClassCode)
            assertEquals("31", dto.commLineCode)
            assertEquals("三階業務員", dto.commLineCodeDesc)
            assertEquals("1", dto.cratType)
            assertEquals("一般佣金_折算率", dto.cratTypeDesc)
            assertEquals(BigDecimal("5.0000"), dto.commRate)
        }
    }
}
