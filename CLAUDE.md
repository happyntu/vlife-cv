# CLAUDE.md - vlife-cv

此模組為 vlife-v4 的子模組，負責產品與精算 (Product & Actuarial) 功能。

## 模組職責

- 產品定義與管理
- 費率表維護
- 精算計算
- 代碼表管理 (CVTB/ETAB)

## V3 對應

| V3 模組 | 表格數 | 說明 |
|:--------|-------:|:-----|
| CV | 135 | 產品與精算核心表格 |

## 核心表格 (P0-P2)

| 優先級 | 表格 | 說明 | 筆數 |
|:------:|:-----|:-----|-----:|
| P0 | CRAT | 佣金率檔 | 53,108 |
| P0 | CVTB | 代碼表 | 2,735 |
| P1 | CVCO | 保單基礎值變化 | 3,782 |
| P1 | CVPU | 產品單位 | 148 |
| P2 | CVDI | 紅利分配水準檔 | 51,409 |
| P2 | CVRF | 準備金因子檔 | 3,368 |

## Coding Standards

### Null Safety (Kotlin)

**嚴格遵守 Kotlin Null Safety 原則：**

1. **優先使用非空類型**：除非業務邏輯明確需要 null，否則使用非空類型
2. **禁止濫用 `!!`**：僅在 100% 確定非空時使用，且須附註說明原因
3. **善用 Safe Call**：使用 `?.` 和 `?:` 處理可空類型
4. **Entity 欄位對應**：
   - 資料庫 `NOT NULL` → Kotlin 非空類型
   - 資料庫允許 NULL → Kotlin `?` 可空類型
   - 有 `DEFAULT` 值且新增時可省略 → 可空類型 `= null`
5. **禁止隱式 null**：不使用 platform types，明確標註可空性
6. **集合類型**：優先使用空集合 `emptyList()` 而非 `null`

**範例**：

```kotlin
// ✅ Good
data class Policy(
    val policyNo: String,          // 必填欄位（DB NOT NULL）
    val status: String,            // 必填欄位（DB NOT NULL）
    val remarks: String?,          // 可選欄位（DB 允許 NULL）
    val createdAt: Instant? = null // 可選欄位，有預設值
)

val items: List<Item> = emptyList()  // 空集合代替 null

// ❌ Bad
val name: String? = null      // 必填欄位不應可空
val items: List<Item>? = null // 使用空集合代替 null
val id: String = id!!         // 禁止無說明的 !!
```

### Code Quality

- **Immutability**：Entity 使用 `data class` + `val`，確保不可變
- **Explicit Types**：公開 API 明確標註返回類型
- **No Magic Numbers**：使用常數或 enum 代替魔術數字
- **Input Validation**：Controller 層使用 `@Size`, `@NotBlank` 等驗證註解

## Git 工作流程

**重要**：禁止直接 commit 到 `main`，所有變更必須透過 feature branch + PR。

```bash
# 1. 建立 feature 分支
git switch -c feat/xxx

# 2. 開發與 Commit
git add <files>
git commit -m "feat: xxx

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"

# 3. Push 前先 Rebase
git fetch origin
git rebase origin/main

# 4. Push 並建立 PR
git push -u origin feat/xxx
gh pr create --title "feat: xxx" --body "..."

# 5. 合併後同步
git switch main && git pull --ff-only origin main
```

## 分支命名規則

| Pattern | 用途 |
|---------|------|
| `feat/*` | 新功能 |
| `fix/*` | 錯誤修復 |
| `docs/*` | 文檔變更 |
| `chore/*` | 維護性變更 |

## 相關文檔

- [根專案 CLAUDE.md](../../CLAUDE.md)
- [CV 表格探索](../../docs/exploration/cv-all-tables.json)
- [V3→V4 模組對應](../../docs/v3-v4-module-mapping.md)
