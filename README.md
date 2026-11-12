# WMS API

仓储管理系统后端，Spring Boot 2.7 / Java 11 / Spring Data JPA / MySQL / Spring Security + JWT。

## 本地运行

```bash
mvn spring-boot:run
```

默认端口 `8088`。配置通过环境变量覆盖：

| 变量 | 说明 |
| --- | --- |
| `DB_URL` | MySQL JDBC URL，默认 `jdbc:mysql://localhost:3306/wmsadmin?...` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号，默认 `root/root` |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 邮件验证码登录 |
| `JWT_SECRET` | JWT 签名密钥 |
| `INTEGRATION_API_KEY` | MES 调用开放接口的共享密钥 |

数据库表由 `spring.jpa.hibernate.ddl-auto=update` 自动创建。仓库不包含本地账号密码文件和含演示账号的 SQL 导出，请自行导入本地 `wmsadmin.sql`。

## MES 集成

- `POST /api/open/inventory/outbound`：领料出库
- `POST /api/open/inventory/inbound`：完工入库
- 请求头：`X-Integration-Key`

接口协议、幂等键和字段映射见 [MES 集成文档](docs/WMS_MES_INTEGRATION.md)。

## 已知限制

- 密码明文存储，仅用于演示；接生产前必须改为 BCrypt。
- 库存按「仓库 + 商品」聚合，不按批次记账。
- 开放接口使用共享密钥，不是 OAuth2。
