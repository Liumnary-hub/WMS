# WMS ↔ MES 库存集成

两个方向：

- MES 领料单完成 → WMS 出库（原料从仓库到产线）
- MES 工单完工 → WMS 入库（成品从产线回仓库）

WMS 是库存主账，MES 本地库存是车间执行镜像。

## 链路

### 领料出库

```
MES POST /api/picklist/{id}/complete
  └─ 校验领料单状态、明细已拣选
  └─ MES 本地扣减 mes_inventory（现存量 + 锁定量）
  └─ 逐条调用 WMS POST /api/open/inventory/outbound
       └─ WMS 校验集成密钥
       └─ WMS 按 requestId 幂等
       └─ WMS 扣减 inventory.count 与 commodity.count
       └─ WMS 写 inventory_record（出库流水，带批次/来源单号）
  └─ 任一 WMS 调用失败 → MES 本地事务回滚
```

### 完工入库

```
MES syncOrderStatus 首次把工单状态改为 completed
  └─ 取 order.completedQuantity
  └─ 调用 WMS POST /api/open/inventory/inbound
       └─ WMS 按 requestId 幂等
       └─ WMS 增加 inventory.count 与 commodity.count
       └─ WMS 写 inventory_record(type=+1)
  └─ WMS 调用失败 → 工单完成和任务完成整体回滚
```

成品仓由 MES 配置 `mes.wms.finished-goods-warehouse-code` 指定，默认 `FG01`。

## 鉴权

请求头：

```
X-Integration-Key: <integration.api-key>
```

WMS 配置：

```yaml
integration:
  api-key: ${INTEGRATION_API_KEY:mes-wms-dev-key}
```

环境变量 `INTEGRATION_API_KEY` 覆盖。MES 用 `WMS_API_KEY` 配置同一个值。

## 接口

`POST /api/open/inventory/outbound`

请求：

```json
{
  "requestId": "MES-PICKLIST-1001-ITEM-2001",
  "commodityCode": "M-DEMO-001",
  "commodityName": "演示原料",
  "warehouseCode": "WH01",
  "warehouseName": "原料仓",
  "quantity": 10,
  "batchNo": "DEMO-2026-001",
  "sourceType": "MES_PICK_LIST",
  "sourceNo": "PL-2026-0001",
  "sourceItemNo": "2001",
  "description": "MES领料单 PL-2026-0001 明细 2001"
}
```

响应（经统一响应封装）：

```json
{
  "code": 200,
  "status": true,
  "msg": null,
  "data": {
    "requestId": "MES-PICKLIST-1001-ITEM-2001",
    "recordId": "wms-record-uuid",
    "commodityId": "wms-commodity-uuid",
    "warehouseId": "wms-warehouse-uuid",
    "quantity": 10,
    "batchNo": "DEMO-2026-001",
    "idempotent": false
  }
}
```

重复提交同一 `requestId`：不重复扣库存，返回首次结果且 `idempotent=true`。

## 完工入库接口

`POST /api/open/inventory/inbound`

请求字段与出库一致：

```json
{
  "requestId": "MES-ORDER-9001-FINISHED-IN",
  "commodityCode": "P-DEMO-001",
  "warehouseCode": "FG01",
  "quantity": 100,
  "batchNo": "WO-DEMO-001",
  "sourceType": "MES_PRODUCTION_ORDER",
  "sourceNo": "WO-DEMO-001",
  "sourceItemNo": "9001",
  "description": "MES完工入库 WO-DEMO-001"
}
```

响应结构同出库接口，`idempotent=true` 表示该 `requestId` 已处理过。
入库会同步增加 `inventory.count` 与 `commodity.count`；仓库内无该商品时自动新建库存行。

## 商品/仓库匹配规则

按顺序匹配，先命中先用：

1. `commodityId` / `warehouseId`（WMS 内部 UUID）
2. `commodityCode` / `warehouseCode`（WMS 实体新增的 `code` 字段）
3. `commodityName` / `warehouseName`

建议在 WMS 商品/仓库上维护与 MES 一致的编码：

```sql
UPDATE commodity SET code = 'M-DEMO-001' WHERE name = '演示原料';
UPDATE warehouse SET code = 'WH01' WHERE name = '原料仓';
```

实体新增字段由 `spring.jpa.hibernate.ddl-auto=update` 自动建列。`inventory_record` 新增：

| 字段 | 用途 |
| --- | --- |
| `request_id` | 幂等键，唯一索引 |
| `batch_no` | MES 批次号留痕 |
| `source_type` | `MES_PICK_LIST` 或 `MES_PRODUCTION_ORDER` |
| `source_no` | MES 领料单号 / 工单号 |
| `source_item_no` | MES 领料明细 ID / 工单 ID |

## 校验与失败语义

- `requestId` 必填且非空。
- `quantity` 必须为正整数。
- WMS 仓库内无该商品、库存不足、商品总库存不足 → 返回业务错误，MES 事务回滚。
- WMS 使用数据库条件更新扣减，不会扣成负数。
- MES 通过 `requestId` 重试安全；WMS 已成功但响应丢失时，重试返回 `idempotent=true`。

## 已知边界

- WMS 库存是「仓库 + 商品」聚合数量，不按批次记账；批次只写入出库流水，无法在 WMS 按批次查库存。
- WMS 不支持出库前锁库存。MES 在拣料阶段锁定的是 MES 本地库存，WMS 库存在完成时才扣减，存在时间窗口。
- MES 出库数量必须为整数；WMS 库存字段为 `int`。
- 未实现 MES → WMS 的退货/冲销接口；失败后由 MES 重试完成领料单。

## 手工验证

```bash
# 1. WMS 启动在 8088
curl -H "X-Integration-Key: mes-wms-dev-key" http://localhost:8088/api/open/ping

# 2. 直接调 WMS 出库
curl -X POST http://localhost:8088/api/open/inventory/outbound \
  -H "Content-Type: application/json" \
  -H "X-Integration-Key: mes-wms-dev-key" \
  -d '{"requestId":"SMOKE-1","commodityCode":"M-DEMO-001","warehouseCode":"WH01","quantity":1,"batchNo":"B1","sourceType":"SMOKE"}'

# 3. 重复调用，应返回 idempotent=true
```
