# AgentHub 业务接入验收记录：agent-document-processing

## 1. 接入系统信息

```text
业务系统名称：agent-document-processing
业务系统负责人：Sean
接入分支 / 版本：0.1.0-SNAPSHOT
JDK 版本：本机 Maven 运行环境，JDK 17+ reactor 已验证
Spring Boot 版本：2.3.12.RELEASE
AgentHub 版本：0.1.0-SNAPSHOT
验收日期：2026-06-12
验收人：Codex
```

## 2. 接入范围

```text
接入方式：SDK / Starter
模型协议：本地 AttachmentAnalysisModelProvider mock 决策
真实模型：默认不接入真实模型；mimo profile 可接入 MiMo 图片解析和文档大纲提炼
是否启用流式接口：否，本样板重点验收非流式附件分析业务接口
是否覆盖 PermissionEngine：是，AttachmentPermissionEngine
是否覆盖 AuditService：是，AttachmentAuditService
是否覆盖 AgentMemory：否，使用 starter 默认实现
```

边界说明：

```text
附件上传、OCR、文件解析、文档分类、字段抽取、规则校验和审核意见生成属于业务样板模块
agent-core 不内置附件、OCR、PDF、Word、身份证或材料审核领域能力
确定性规则由业务代码执行，模型或 mock provider 只负责触发 Tool 和总结结果
api 层只保留 HTTP 入口，请求和响应 DTO 归入 domain，避免 Controller 包承载业务模型
```

## 3. 业务接口

```text
POST /attachments
上传附件，返回 attachmentId、filename、parserName。

POST /agent/chat
AgentHub 通用入口。用户明确要求“请分析附件 att-...”时触发附件分析 Tool 链路。

POST /attachment-analysis/analyze
业务入口。根据已有 attachmentId 触发分析，返回 answer 和结构化 analysis。

POST /attachment-analysis/analyze-file
业务一体化入口。上传文件并立即分析，适合其他服务一次调用。

POST /attachment-analysis/outline-file
业务入口。上传 PDF 或 Markdown 并返回结构化大纲和重点。
```

## 4. Tool 信息

```text
Tool name：parse_attachment
业务场景：读取 attachmentId 对应的解析内容
风险等级：READ
参数列表：attachmentId
必填参数：attachmentId
权限边界：仅 userId=attachment-reviewer 允许分析附件
脱敏字段：完整公民身份号码不进入 answer 和审计结果摘要
失败成本：低，样板内存数据，不访问真实业务数据
```

```text
Tool name：classify_document
业务场景：识别材料类型，例如 ID_CARD、CONTRACT、UNKNOWN
风险等级：READ
参数列表：attachmentId
必填参数：attachmentId
权限边界：同附件分析权限
```

```text
Tool name：extract_document_fields
业务场景：抽取出生日期、证件号等结构化字段
风险等级：READ
参数列表：attachmentId
必填参数：attachmentId
权限边界：同附件分析权限
脱敏字段：证件号默认脱敏后进入后续总结和审计
```

```text
Tool name：check_document_rules
业务场景：执行确定性业务规则，例如是否满 18 周岁
风险等级：READ
参数列表：attachmentId
必填参数：attachmentId
权限边界：同附件分析权限
规则边界：通过 / 驳回由业务代码确定，模型不覆盖确定性结论
```

```text
Tool name：summarize_attachment_analysis
业务场景：汇总文档类型、抽取字段、规则结果和审核意见
风险等级：READ
参数列表：attachmentId
必填参数：attachmentId
权限边界：同附件分析权限
```

Tool description 检查：

```text
是否写明明确触发条件：是，只有明确要求分析 attachmentId 时触发
是否写明不应触发场景：是，普通聊天和介绍类问题不触发附件 Tool
是否避免把内部实现细节暴露给模型：是，Tool 只暴露业务动作和参数
```

Tool Schema 检查：

```text
type 是否为 object：是
properties 是否覆盖全部入参：是，attachmentId
required 是否覆盖业务必填参数：是，attachmentId
enum 是否只用于稳定枚举：当前无 enum
```

## 5. 配置摘要

默认 profile：

```yaml
agent:
  enabled: true
  model:
    protocol: echo

attachment:
  mock-model:
    enabled: true
```

MiMo 图片解析 profile：

```yaml
attachment:
  parser:
    image:
      mode: mimo
      mimo:
        base-url: ${AGENTHUB_MIMO_BASE_URL:https://api.xiaomimimo.com/v1}
        api-key: "***"
        model: ${AGENTHUB_MODEL_MIMO_IMAGE:mimo-v2-omni}
        connect-timeout-ms: 10000
        read-timeout-ms: 120000
  outline:
    mode: mimo
    mimo:
      base-url: ${AGENTHUB_MIMO_BASE_URL:https://api.xiaomimimo.com/v1}
      api-key: "***"
      model: ${AGENTHUB_MODEL_MIMO_TEXT:mimo-v2.5-pro}
      connect-timeout-ms: 10000
      read-timeout-ms: 120000
```

## 6. 验收命令

模块测试：

```bash
mvn -pl agent-document-processing test
```

MVP 主 reactor：

```bash
mvn '-P!adapters-java17' test
```

JDK 17+ 完整 reactor：

```bash
mvn test
```

启动样板：

```bash
mvn -pl agent-document-processing spring-boot:run
```

上传文本附件：

```bash
curl -F 'file=@id-card.txt;type=text/plain' http://127.0.0.1:8080/attachments
```

通过 AgentHub 通用入口分析：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"att-chat-001","userId":"attachment-reviewer","message":"请分析附件 att-xxx"}'
```

通过业务接口分析已有附件：

```bash
curl -sS -X POST http://127.0.0.1:8080/attachment-analysis/analyze \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"att-business-001","userId":"attachment-reviewer","attachmentId":"att-xxx"}'
```

上传并立即分析：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@id-card.txt;type=text/plain' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

上传 Markdown 并提炼大纲：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@policy.md;type=text/markdown' \
  http://127.0.0.1:8080/attachment-analysis/outline-file
```

上传 PDF 并提炼大纲：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@policy.pdf;type=application/pdf' \
  http://127.0.0.1:8080/attachment-analysis/outline-file
```

无权限样例：

```bash
curl -sS -F 'userId=u001' \
  -F 'file=@contract.txt;type=text/plain' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

## 7. 验收结果

| 项目 | 结果 | 证据 |
|------|------|------|
| `mvn -pl agent-document-processing -am test` 通过 | 通过 | 2026-06-12 本地执行，12 个附件样板测试通过，依赖模块测试同步通过 |
| `mvn '-P!adapters-java17' test` 通过 | 通过 | 2026-06-12 本地执行，74 tests passed |
| `mvn test` 通过 | 通过 | 2026-06-12 本地执行，82 tests passed |
| `git diff --check` 通过 | 通过 | 2026-06-12 本地执行，无输出 |
| 应用启动成功 | 部分通过 | 集成测试使用 RANDOM_PORT 启动 Spring Boot 上下文并调用 HTTP 接口，未单独手工启动 |
| `/attachments` 上传通过 | 通过 | `shouldUploadAttachmentAndAnalyzeWithToolAudit` 间接覆盖上传并返回 attachmentId |
| `/agent/chat` 文本响应通过 | 通过 | `shouldNotTriggerAttachmentToolForNormalChat` |
| `/agent/chat` ToolCall 通过 | 通过 | `shouldUploadAttachmentAndAnalyzeWithToolAudit` |
| `/attachment-analysis/analyze` 通过 | 通过 | `shouldAnalyzeExistingAttachmentThroughBusinessApi` |
| `/attachment-analysis/analyze-file` 通过 | 通过 | `shouldUploadAndAnalyzeFileThroughBusinessApi` |
| `/attachment-analysis/outline-file` Markdown 大纲提炼通过 | 通过 | `shouldExtractOutlineFromMarkdownDocument` |
| `/attachment-analysis/outline-file` PDF 大纲提炼通过 | 通过 | `shouldExtractOutlineFromPdfDocument` |
| `/attachment-analysis/outline-file` 无权限拒绝通过 | 通过 | `shouldDenyOutlineAnalysisWithoutPermission` |
| 图片 parser 扩展点通过 | 通过 | `shouldAnalyzeImageThroughImageParserExtensionPoint` 覆盖 image/* mock parser |
| 普通聊天不误触发 Tool | 通过 | 普通聊天返回空 toolCalls，审计事件为空 |
| 明确分析 attachmentId 可触发 Tool | 通过 | 返回 5 个 ToolCall，首个为 parse_attachment，末个为 summarize_attachment_analysis |
| 无权限时不执行 Tool | 通过 | userId=u001 返回 Tool permission denied，并记录失败审计 |
| AuditEvent 记录完整 | 部分通过 | 测试断言 userId、toolName、success、errorMessage、toolResultSummary 脱敏 |
| Tool 结果已脱敏 | 通过 | answer 和 toolResultSummary 不包含完整证件号 |
| 模型总结不泄露敏感字段 | 通过 | answer 不包含完整公民身份号码 |
| 规则判断由业务代码确定 | 通过 | 测试断言 age、adult、passed、opinion |
| 不支持文件类型返回可读错误 | 通过 | `shouldReturnReadableErrorForUnsupportedFileType` 返回 HTTP 400 和 errorMessage |
| 空文件 / 空解析结果返回可读错误 | 通过 | `shouldReturnReadableErrorForEmptyParsedAttachment` 返回 HTTP 400 和 errorMessage |
| 不存在 attachmentId 返回结构化错误 | 通过 | `shouldReturnStructuredErrorForMissingAttachmentId` 返回 ok=false、errorMessage，不写审计 |

## 8. 审计字段检查

```text
traceId：未断言
sessionId：由 AgentRequest 传入，测试未断言
userId：已断言
toolName：已断言
requestSummary：未断言
toolResultSummary：已断言不包含完整证件号
latencyMs：未断言
success：已断言
errorMessage：已断言无权限错误
createdAt：未断言
```

审计要求：

```text
requestSummary 不记录密钥、token、密码、完整证件号：样板未使用密钥，后续需补 requestSummary 断言
toolResultSummary 只记录摘要或脱敏字段：已覆盖完整证件号不进入摘要
errorMessage 不包含完整堆栈和敏感配置：无权限错误为 Only attachment-reviewer can analyze attachments
```

## 9. 风险和遗留问题

```text
权限模型风险：当前只验证 userId 白名单，生产系统需接入真实 RBAC / 数据权限。
数据脱敏风险：当前重点覆盖身份证号脱敏，后续应扩展到手机号、银行卡、地址等字段。
模型误触发风险：样板 AttachmentAnalysisModelProvider 是规则型 mock，不能代表真实模型误触发概率。
业务接口稳定性风险：默认 profile 不访问真实 OCR、Word 或存储服务，未覆盖超时和下游异常。
审计落库或日志平台风险：样板使用内存 List，生产系统需接数据库、日志平台或审计系统。
错误响应风险：不存在 attachmentId、不支持文件类型、空文件等结构化错误已覆盖，后续仍需统一生产级错误码和 traceId。
MiMo 真实模型风险：mimo profile 已有图片解析和文档大纲入口，但本验收未执行真实外部模型调用。
PDF 解析风险：当前仅覆盖文本型 PDF，扫描件 PDF 仍需 OCR adapter。
```

## 10. 验收结论

```text
结论：有条件通过
是否允许进入试运行：允许作为附件类业务接入样板，不等同生产业务通过
是否需要补 AgentHub core 字段：暂不需要
是否需要补 starter 配置：暂不需要
是否需要进入 Gateway：暂不需要
是否需要进入 MCP SDK Adapter：暂不需要
是否需要进入 Admin UI：暂不需要
```

下一步：

```text
1. 将年龄判断、审核通过 / 驳回等确定性规则从 AttachmentToolSupport 逐步拆到 domain/service。
2. 按 AttachmentContentParser 扩展 Word、扫描件 PDF 或企业 OCR adapter，继续保持 agent-core 不感知附件领域。
3. 如需验收真实图片识别，单独启用 mimo profile 并记录外部模型调用结果。
4. 生产接入前补统一错误码、traceId 和审计 requestSummary 断言。
```
