# scripts 辅助脚本

| 脚本 | 用途 |
|------|------|
| `seed.sql` | 用户中心 dashboard 测试种子数据（用户/文章/专栏/评论/互动/关注） |
| `fix-mojibake.sql` | 修复历史乱码数据 |

## 种子数据

账号密码统一为 `Admin@123`（Argon2id 哈希，与后端 `PasswordEncoder` 参数一致）。

| 账号 | 角色 | 说明 |
|------|------|------|
| `admin`（或 `superadmin`） | ADMIN | 管理员，可看到用户中心 + 管理菜单（统计/审核/用户管理） |
| `alice01` | AUTHOR | 作者，发布了多篇文章与专栏 |
| `bobuser` | USER | 普通用户，有评论/点赞/收藏/关注 |
| `caroluser` | USER | 普通用户，有评论/点赞/收藏/关注 |

执行（需先执行各服务 `V1.0.0`/`V1.1.0` 迁移脚本）：

```bash
mysql -u root -p sca_system < scripts/seed.sql
```

> 脚本使用 `ON DUPLICATE KEY UPDATE`，可重复执行；`admin` 用户的密码会用 Argon2id 哈希覆盖 `V1.0.0` 初始化时写入的 BCrypt 哈希（BCrypt 无法被 Argon2id 校验）。