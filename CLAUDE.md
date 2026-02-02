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
