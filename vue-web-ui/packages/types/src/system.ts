export interface UserVO {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  bio?: string
  followCount?: number
  followerCount?: number
  deptId?: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  roles?: Array<{ id?: string; code?: string; name?: string }>
}

export interface UserCreateDTO {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  deptId?: string
}

export interface UserUpdateDTO {
  id: string
  nickname?: string
  email?: string
  phone?: string
  bio?: string
  deptId?: string
  status?: number
  version?: number
}

export interface UserPageQuery {
  page: number
  size: number
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
