/** 用户视图对象：个人资料 / 用户列表通用出参 */
export interface UserVO {
  /** 用户 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 登录账号 */
  username: string
  /** 显示昵称 */
  nickname?: string
  /** 邮箱 */
  email?: string
  /** 手机号 */
  phone?: string
  /** 头像 URL */
  avatar?: string
  /** 个人简介 */
  bio?: string
  /** 关注数 */
  followCount?: number
  /** 粉丝数 */
  followerCount?: number
  /** 部门 ID（雪花 ID，为 string，禁止转 number） */
  deptId?: string
  /** 账号状态：1=正常 2=禁用 */
  status: number
  /** 最近登录时间 */
  lastLoginTime?: string
  /** 最近登录 IP */
  lastLoginIp?: string
  /** 创建时间 */
  createTime?: string
  /** 角色列表 */
  roles?: Array<{ id?: string; code?: string; name?: string }>
}

/** 系统用户创建入参（管理员建号） */
export interface UserCreateDTO {
  /** 登录账号（唯一） */
  username: string
  /** 初始密码（后端加密存储） */
  password: string
  /** 显示昵称 */
  nickname?: string
  /** 邮箱 */
  email?: string
  /** 手机号 */
  phone?: string
  /** 部门 ID */
  deptId?: string
}

/** 系统用户更新入参（部分字段可选，仅更新传入字段） */
export interface UserUpdateDTO {
  /** 用户 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 显示昵称 */
  nickname?: string
  /** 邮箱 */
  email?: string
  /** 手机号 */
  phone?: string
  /** 个人简介 */
  bio?: string
  /** 部门 ID */
  deptId?: string
  /** 账号状态：1=正常 2=禁用 */
  status?: number
  /** 乐观锁版本号（防止并发覆盖更新） */
  version?: number
}

/** 用户分页查询入参 */
export interface UserPageQuery {
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
  /** 关键字：模糊匹配用户名/昵称 */
  keyword?: string
  /** 账号状态过滤 */
  status?: number
  /** 部门 ID 过滤 */
  deptId?: number
  /** 排序字段（如 create_time_desc） */
  orderBy?: string
}

/** 角色视图对象 */
export interface RoleVO {
  /** 角色 ID */
  id: number
  /** 角色编码（唯一，如 ADMIN/AUTHOR） */
  code: string
  /** 角色名称 */
  name: string
  /** 数据权限范围枚举（1=全部 2=本部门 3=本人） */
  dataScope?: number
  /** 状态：1=正常 0=停用 */
  status?: number
  /** 备注 */
  remark?: string
}

/** 菜单视图对象（动态路由与菜单渲染的数据源） */
export interface MenuVO {
  /** 菜单 ID */
  id: number
  /** 父菜单 ID（0 为顶级菜单） */
  parentId: number
  /** 菜单名称 */
  name: string
  /** 类型：1=目录 2=菜单 3=按钮 */
  type: number
  /** 路由路径 */
  path?: string
  /** 组件路径（懒加载导入用） */
  component?: string
  /** 菜单图标 */
  icon?: string
  /** 权限标识（按钮级用，如 system:user:add） */
  perms?: string
  /** 排序号（越小越靠前） */
  sort: number
  /** 是否可见：1=显示 0=隐藏 */
  visible: number
  /** 子菜单 */
  children?: MenuVO[]
}

/** 部门视图对象 */
export interface DeptVO {
  /** 部门 ID */
  id: number
  /** 父部门 ID（0 为顶级部门） */
  parentId: number
  /** 部门名称 */
  name: string
  /** 祖先链（如 0,100,200，用于子树查询） */
  ancestors?: string
  /** 负责人 */
  leader?: string
  /** 排序号 */
  sort?: number
  /** 状态：1=正常 0=停用 */
  status?: number
  /** 子部门 */
  children?: DeptVO[]
}

/** 数据字典视图对象 */
export interface DictVO {
  /** 字典 ID */
  id: number
  /** 字典类型（如 sys_user_sex） */
  type: string
  /** 字典标签（展示用） */
  label: string
  /** 字典值（存储用） */
  value: string
  /** 排序号 */
  sort?: number
  /** 状态：1=正常 0=停用 */
  status?: number
  /** 备注 */
  remark?: string
}

/** 系统参数视图对象（key-value 配置项） */
export interface ParamVO {
  /** 参数 ID */
  id: number
  /** 参数键（唯一） */
  key: string
  /** 参数值 */
  value: string
  /** 备注 */
  remark?: string
  /** 更新时间 */
  updateTime?: string
}

/** 系统通知视图对象 */
export interface NoticeVO {
  /** 通知 ID */
  id: number
  /** 通知标题 */
  title: string
  /** 通知正文 */
  content: string
  /** 类型：1=通知 2=公告 */
  type?: number
  /** 状态：1=草稿 2=已发布 */
  status: number
  /** 创建时间 */
  createTime?: string
  /** 发布时间 */
  publishTime?: string
}
