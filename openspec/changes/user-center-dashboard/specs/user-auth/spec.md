## MODIFIED Requirements

### Requirement: 登录成功跳转 dashboard 用户中心

系统 SHALL 在登录成功后自动跳转到 dashboard 用户中心（而非个人主页），dashboard 按角色展示不同菜单与页面；未登录访问受保护页面时跳转登录页并在登录后返回原目标页面。

#### Scenario: 登录成功跳转 dashboard

- **WHEN** 用户通过账号密码方式登录成功
- **THEN** 系统保存登录态并跳转到 dashboard 用户中心页面，按角色渲染菜单

#### Scenario: 带 redirect 登录

- **WHEN** 用户未登录访问受保护页面被重定向到登录页，登录成功后
- **THEN** 系统跳转回原来的目标页面；若未指定 redirect 则跳转 dashboard 用户中心

## ADDED Requirements

### Requirement: 修改密码

系统 SHALL 允许登录用户修改自己的密码：需提交旧密码、新密码与确认新密码；新密码 SHALL 与旧密码不同且满足长度规则（6-20 位）；旧密码错误 SHALL 返回错误提示且不更新密码；修改成功后 SHALL 清除当前登录态并要求重新登录。

#### Scenario: 修改密码成功

- **WHEN** 登录用户提交正确旧密码与合规新密码
- **THEN** 密码更新成功，当前登录态失效，需重新登录

#### Scenario: 旧密码错误

- **WHEN** 登录用户提交错误的旧密码
- **THEN** 系统提示"旧密码错误"，不更新密码，保持当前登录态

#### Scenario: 新密码与旧密码相同

- **WHEN** 登录用户提交的新密码与旧密码完全一致
- **THEN** 系统拒绝并提示"新密码不能与旧密码相同"，不更新密码