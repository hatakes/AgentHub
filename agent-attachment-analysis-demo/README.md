# agent-attachment-analysis-demo

智能附件分析业务样板，用于验证业务系统通过 `agent-spring-boot-starter` 接入 AgentHub。

当前边界：

```text
文件上传、OCR、文件解析、文档分类、字段抽取、规则校验属于本业务样板
AgentHub core 只负责 AgentRuntime、Tool、权限、审计和模型抽象
默认使用 mock 模型和文本附件模拟 OCR / 解析结果
启用 mimo profile 后，image/* 会通过 MiMo 多模态模型解析图片内容
PDF / Markdown 支持独立的大纲和重点提炼接口
```

当前包结构：

```text
api              HTTP 入口
application      附件分析用例编排和 mock model provider
domain           附件、文档类型、抽取字段、规则结果
infrastructure   内存仓库、权限、审计、OCR / 文件解析 adapter
tool             AgentTool 适配层
support          临时辅助类
```

当前文件解析入口：

```text
infrastructure/parser/AttachmentContentParser
infrastructure/parser/TextAttachmentContentParser
infrastructure/parser/ImageAttachmentContentParser
infrastructure/parser/MarkdownAttachmentContentParser
infrastructure/parser/PdfAttachmentContentParser
```

`TextAttachmentContentParser` 负责 `.txt` / `text/*` 文件；`MarkdownAttachmentContentParser` 负责 `.md` / `.markdown`；`PdfAttachmentContentParser` 通过 PDFBox 解析文本型 PDF；`ImageAttachmentContentParser` 默认是 mock，占位读取测试内容。启动 `mimo` profile 后，图片解析会调用 MiMo OpenAI-compatible `/chat/completions`，用 `mimo-v2-omni` 识别图片并把模型 JSON 结果转成后续工具可检索的文本；文档大纲提炼会调用 MiMo 文本模型，默认 `mimo-v2.5-pro`。

接口流程说明：

```text
docs/analyze-file-flow.md
```

运行：

```bash
mvn -pl agent-attachment-analysis-demo spring-boot:run
```

启用 MiMo 图片解析：

```bash
export AGENTHUB_MIMO_BASE_URL=https://api.xiaomimimo.com/v1
export AGENTHUB_MIMO_API_KEY=你的 MiMo API Key
export AGENTHUB_MODEL_MIMO_IMAGE=mimo-v2-omni
export AGENTHUB_MODEL_MIMO_TEXT=mimo-v2.5-pro

mvn -pl agent-attachment-analysis-demo spring-boot:run \
  -Dspring-boot.run.profiles=mimo
```

如果 8080 被占用，可以临时换端口：

```bash
mvn -pl agent-attachment-analysis-demo spring-boot:run \
  -Dspring-boot.run.profiles=mimo \
  -Dspring-boot.run.arguments=--server.port=18080
```

上传附件：

```bash
curl -F 'file=@id-card.txt;type=text/plain' http://127.0.0.1:8080/attachments
```

也可以上传本地绝对路径文件：

```bash
curl -F 'file=@/Users/sean/Desktop/id-card.txt;type=text/plain' \
  http://127.0.0.1:8080/attachments
```

通过 AgentHub 通用入口分析附件：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"att-001","userId":"attachment-reviewer","message":"请分析附件 att-xxx"}'
```

通过业务接口分析已有附件：

```bash
curl -sS -X POST http://127.0.0.1:8080/attachment-analysis/analyze \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"att-001","userId":"attachment-reviewer","attachmentId":"att-xxx"}'
```

上传并立即分析，适合其他服务一次调用完成基础分析：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@id-card.txt;type=text/plain' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

上传本地绝对路径并立即分析：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@/Users/sean/Desktop/id-card.txt;type=text/plain' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

默认 profile 使用文本附件模拟 OCR / 文件解析结果，推荐先上传 `.txt` 文件，例如：

```text
居民身份证 姓名 张三 公民身份号码 110101200901011234 出生日期 2009-01-01
```

启用 `mimo` profile 后，可以上传图片并立即分析：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@/Users/sean/Desktop/id-card.png;type=image/png' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

当前真实图片解析只覆盖 `image/*`。PDF、Word 等真实附件后续应继续通过 `AttachmentContentParser` 扩展专用 adapter。

上传 PDF 或 Markdown 并提炼大纲和重点：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@/Users/sean/Desktop/policy.md;type=text/markdown' \
  http://127.0.0.1:8080/attachment-analysis/outline-file
```

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@/Users/sean/Desktop/policy.pdf;type=application/pdf' \
  http://127.0.0.1:8080/attachment-analysis/outline-file
```

默认 profile 使用本地确定性提炼，便于集成测试和离线验证。启用 `mimo` profile 后，`/attachment-analysis/outline-file` 会调用 MiMo 文本模型生成结构化 JSON：

```json
{
  "attachmentId": "att-xxx",
  "ok": true,
  "outline": {
    "attachmentId": "att-xxx",
    "filename": "policy.md",
    "parserName": "markdown",
    "title": "招生政策解读",
    "summary": "已解析 policy.md，提炼出 3 个大纲节点和 2 条重点。",
    "outline": ["招生政策解读", "报名条件", "录取重点"],
    "keyPoints": ["考生需要关注报名时间、资格审核和材料提交要求。"],
    "metadata": {
      "format": "markdown",
      "textLength": 96
    }
  },
  "errorMessage": null
}
```

业务分析接口会返回结构化 `analysis` 字段，同时保留 `answer` 兼容 Agent 对话输出：

```json
{
  "attachmentId": "att-xxx",
  "ok": true,
  "answer": "附件分析结果：{attachmentId=att-xxx, documentType=ID_CARD, birthDate=2009-01-01, age=17, adult=false, passed=false, opinion=建议驳回，原因是申请人未满 18 周岁}",
  "analysis": {
    "attachmentId": "att-xxx",
    "documentType": "ID_CARD",
    "birthDate": "2009-01-01",
    "age": 17,
    "adult": false,
    "passed": false,
    "opinion": "建议驳回，原因是申请人未满 18 周岁"
  },
  "errorMessage": null,
  "toolCalls": []
}
```
