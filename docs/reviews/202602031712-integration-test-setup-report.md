# P2-7 整合測試設定報告

**測試日期**: 2026-02-03 17:12
**測試環境**: Windows 10, Docker 29.1.3, JDK 21
**測試類別**: `com.vlife.cv.CratMapperIntegrationTest`

---

## 測試設定摘要

### 已建立的測試基礎架構

| 組件 | 檔案 | 狀態 |
|:-----|:-----|:----:|
| TestContainers 配置 | `CratMapperIntegrationTest.kt` | ✅ 已建立 |
| Schema 初始化腳本 | `init-cv-schema.sql` | ⚠️ 需修正 |
| 測試 Profile | `application-integration-test.yml` | ✅ 已配置 |
| 測試配置類別 | `TestConfiguration.kt` | ✅ 已建立 |
| 測試應用程式 | `TestApplication.kt` | ✅ 已建立 |

---

## 測試架構設計 (ADR-018)

### Singleton Container Pattern

採用 TestContainers 官方推薦的 Singleton Container Pattern：

```kotlin
companion object {
    @Container
    @JvmStatic
    val oracle: OracleContainer = OracleContainer(
        DockerImageName.parse("gvenzl/oracle-xe:21-slim")
    )
        .withDatabaseName("VLIFE")
        .withUsername("vlife")
        .withPassword("vlife123")
        .withInitScript("init-cv-schema.sql")

    @JvmStatic
    @DynamicPropertySource
    fun configureProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { oracle.jdbcUrl }
        registry.add("spring.datasource.username") { oracle.username }
        registry.add("spring.datasource.password") { oracle.password }
    }
}
```

### 測試案例設計

| 測試群組 | 測試案例 | 驗證目標 |
|:---------|:---------|:---------|
| FindBySerial | 存在時返回資料 | 主鍵查詢正確性 |
| FindBySerial | 不存在時返回 null | 空結果處理 |
| FindByClassCode | 返回該類別碼的所有資料 | 非主鍵查詢 |
| FindByClassCode | 未知類別碼返回空清單 | 邊界條件 |
| FindAllLineCodes | 返回不重複的業務線代號 | DISTINCT 查詢 |

---

## 初始化腳本 (init-cv-schema.sql)

### 已定義的表格

| 表格 | 用途 | 欄位數 |
|:-----|:-----|:------:|
| CV.CRAT | 佣金率表 | 19 |
| CV.CVCO | 承保範圍表 | 15 |
| CV.CVPU | 產品單位表 | 16 |
| CV.CVDI | 紅利分配水準表 | 22 |
| CV.CVRF | 準備金因子表 | 33 |

### 已建立的索引

- `CV.IDX_CRAT_CLASS_CODE` - 佣金率類別碼索引
- `CV.IDX_CRAT_LINE_CODE` - 業務線代號索引
- `CV.IDX_CVCO_PLAN_CODE` - 險種代碼索引
- `CV.IDX_CVCO_STATUS` - 承保狀態索引
- `CV.IDX_CVPU_COVERAGE` - 承保範圍索引
- `CV.IDX_CVDI_PLAN` - 紅利分配計畫索引
- `CV.IDX_CVRF_PLAN` - 準備金因子計畫索引

---

## 待解決問題

### Oracle 初始化腳本語法問題

**錯誤類型**: `SQLSyntaxErrorException`

**根本原因**: TestContainers 執行多語句 SQL 腳本時，Oracle 對於某些語法的處理與 MySQL/PostgreSQL 不同。可能需要：

1. 將多個 GRANT 語句分開
2. 確認 Oracle XE 21 slim 映像的特定限制
3. 考慮使用 `/` 作為語句分隔符

**建議修正方向**:
- 將複雜的初始化邏輯拆分為多個小腳本
- 或使用 Flyway 進行遷移而非單一 init script

---

## 測試資源檔案

### application-integration-test.yml

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521/VLIFE
    username: vlife
    password: vlife123
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

---

## 下一步行動

1. **修正初始化腳本**: 調整 `init-cv-schema.sql` 的語法以相容 Oracle XE
2. **驗證容器啟動**: 確認 Oracle XE 容器可正常啟動
3. **執行整合測試**: 運行完整測試套件並產生報告

---

## 相關文件

- [ADR-018: TestContainers 整合測試策略](../../../docs/adr/ADR-018-integration-test-strategy.md)
- [CratMapperIntegrationTest.kt](../../src/test/kotlin/com/vlife/cv/CratMapperIntegrationTest.kt)
- [init-cv-schema.sql](../../src/test/resources/init-cv-schema.sql)
- [TestConfiguration.kt](../../src/test/kotlin/com/vlife/cv/TestConfiguration.kt)
