import { request } from '@sca/api'
import type {
  PageVO,
  UserVO,
  UserCreateDTO,
  UserUpdateDTO,
  UserPageQuery
} from '@sca/types'

export function pageUsers(query: UserPageQuery) {
  return request.get<PageVO<UserVO>>('/api/system/users', { params: query })
}

export function getUser(id: number) {
  return request.get<UserVO>(`/api/system/users/${id}`)
}

export function createUser(dto: UserCreateDTO) {
  return request.post<boolean>('/api/system/users', dto)
}

export function updateUser(id: number, dto: UserUpdateDTO) {
  return request.put<boolean>(`/api/system/users/${id}`, dto)
}

export function deleteUser(id: number) {
  return request.delete<boolean>(`/api/system/users/${id}`)
}

export function resetPassword(id: number, newPwd: string) {
  return request.patch<boolean>(`/api/system/users/${id}/password`, { newPassword: newPwd })
}

export function changeUserStatus(id: number, status: number) {
  return request.patch<boolean>(`/api/system/users/${id}/status`, { status })
}
