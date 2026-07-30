export interface UserVO {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  roles?: Array<{ id?: number; code?: string; name?: string }>
}

export interface UserCreateDTO {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  deptId?: number
}

export interface UserUpdateDTO {
  id: number
  nickname?: string
  email?: string
  phone?: string
  deptId?: number
  status?: number
  version?: number
}

export interface UserPageQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
  deptId?: number
  orderBy?: string
}

export interface RoleVO {
  id: number
  code: string
  name: string
  dataScope?: number
  status?: number
  remark?: string
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  type: number
  path?: string
  component?: string
  icon?: string
  perms?: string
  sort: number
  visible: number
  children?: MenuVO[]
}

export interface DeptVO {
  id: number
  parentId: number
  name: string
  ancestors?: string
  leader?: string
  sort?: number
  status?: number
  children?: DeptVO[]
}

export interface DictVO {
  id: number
  type: string
  label: string
  value: string
  sort?: number
  status?: number
  remark?: string
}

export interface ParamVO {
  id: number
  key: string
  value: string
  remark?: string
  updateTime?: string
}

export interface NoticeVO {
  id: number
  title: string
  content: string
  type?: number
  status: number
  createTime?: string
  publishTime?: string
}
