import { request } from '@sca/api'
import type { MenuVO } from '@sca/types'

export function getMenuTree() {
  return request.get<MenuVO[]>('/api/system/menus/tree')
}

export function createMenu(dto: Partial<MenuVO>) {
  return request.post<boolean>('/api/system/menus', dto)
}

export function updateMenu(id: number, dto: Partial<MenuVO>) {
  return request.put<boolean>(`/api/system/menus/${id}`, dto)
}

export function deleteMenu(id: number) {
  return request.delete<boolean>(`/api/system/menus/${id}`)
}
