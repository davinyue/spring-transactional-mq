# 默认 JSON 改 luava-json 并在 RabbitMQ 层 gzip

## 背景

当前默认序列化器使用 Jackson，RabbitMQ 发送内容也是未压缩文本。现在需要调整为：数据库中仍保存原始 JSON 文本，默认 JSON 工具改为 `luava-json` 的 `JacksonUtils`；RabbitMQ 发送时对 JSON 文本做 gzip 压缩，消费时先解压再反序列化。

## 需求

- 默认 `MessagePayloadSerializer` 改用 `JacksonUtils`。
- 数据库存储的 `payload_text` 保持原始 JSON 文本，不做压缩。
- RabbitMQ 发送时将 `payloadText` gzip 压缩成字节数组。
- RabbitMQ 消费时支持 gzip 解压后再 JSON 反序列化。
- 更新相关测试并执行验证。

## 任务列表

- [x] 补充序列化与 RabbitMQ 压缩红测。
- [x] 默认序列化器切换为 luava-json。
- [x] RabbitMQ 发送/消费加入 gzip 处理。
- [x] 调整真实测试中的消息体读取方式。
- [x] 执行相关测试。

## 进度

- 2026-04-16 创建任务记录。
- 2026-04-16 已完成默认 JSON 切换与 RabbitMQ gzip 传输验证。
